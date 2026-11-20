package ua.edu.digitaldean.app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import ua.edu.digitaldean.model.AcademicRequest;
import ua.edu.digitaldean.model.TranscriptRequest;
import ua.edu.digitaldean.service.DiagnosticLog;
import ua.edu.digitaldean.service.ExchangeFormatException;
import ua.edu.digitaldean.service.RequestCatalog;
import ua.edu.digitaldean.service.RequestFileRow;
import ua.edu.digitaldean.service.RequestFiles;
import ua.edu.digitaldean.service.RequestProcessor;
import ua.edu.digitaldean.service.StandardFeePolicy;

public final class FileExchangeApp {
    private record Parameters(
            Path baseDir,
            char delimiter,
            int maxBytes,
            String mode,
            String resourceName,
            String sourceFormat,
            Path input,
            String targetFormat,
            Path output,
            Path probeTarget) {
    }

    private FileExchangeApp() {
    }

    public static void main(String[] args) {
        Parameters parameters;
        try {
            parameters = parse(args);
        } catch (IllegalArgumentException exception) {
            System.err.println("Помилка: некоректні аргументи.");
            System.exit(2);
            return;
        }
        Logger logger = DiagnosticLog.create();
        int code;
        try {
            code = execute(parameters, logger);
        } finally {
            for (Handler handler : logger.getHandlers()) {
                handler.close();
                logger.removeHandler(handler);
            }
        }
        if (code != 0) {
            System.exit(code);
        }
    }

    private static Parameters parse(String[] args) {
        if (args == null) {
            throw new IllegalArgumentException("Аргументи відсутні.");
        }
        for (String arg : args) {
            if (arg == null) {
                throw new IllegalArgumentException("Аргумент не може бути null.");
            }
        }

        Path baseDir = Path.of(".");
        char delimiter = ',';
        int maxBytes = RequestFiles.DEFAULT_MAX_BYTES;

        boolean seenBase = false;
        boolean seenDelimiter = false;
        boolean seenMaxBytes = false;

        int idx = 0;
        while (idx < args.length) {
            String token = args[idx];
            if (token.equals("--base")) {
                if (seenBase || idx + 1 >= args.length || args[idx + 1].isBlank()) {
                    throw new IllegalArgumentException("Некоректний --base.");
                }
                baseDir = Path.of(args[idx + 1]);
                seenBase = true;
                idx += 2;
            } else if (token.equals("--delimiter")) {
                if (seenDelimiter || idx + 1 >= args.length) {
                    throw new IllegalArgumentException("Некоректний --delimiter.");
                }
                delimiter = switch (args[idx + 1]) {
                    case "comma" -> ',';
                    case "semicolon" -> ';';
                    default -> throw new IllegalArgumentException("Невідомий розділювач.");
                };
                seenDelimiter = true;
                idx += 2;
            } else if (token.equals("--max-bytes")) {
                if (seenMaxBytes || idx + 1 >= args.length
                        || !args[idx + 1].matches("0|[1-9][0-9]*")) {
                    throw new IllegalArgumentException("Некоректний --max-bytes.");
                }
                try {
                    maxBytes = Integer.parseInt(args[idx + 1]);
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("Переповнення --max-bytes.", exception);
                }
                if (maxBytes < 0 || maxBytes > RequestFiles.DEFAULT_MAX_BYTES) {
                    throw new IllegalArgumentException("Ліміт поза межами.");
                }
                seenMaxBytes = true;
                idx += 2;
            } else {
                break;
            }
        }

        int remaining = args.length - idx;
        if (remaining == 0 || (remaining == 1 && args[idx].equals("--resource"))) {
            return new Parameters(baseDir, delimiter, maxBytes,
                    "--resource", "requests.csv", "csv", null, null, null, null);
        }
        if (remaining == 2 && args[idx].equals("--seed")) {
            Path out = RequestFiles.resolveInside(baseDir, args[idx + 1]);
            return new Parameters(baseDir, delimiter, maxBytes,
                    "--seed", "requests.csv", "csv", null, "json", out, null);
        }
        if (remaining == 3 && args[idx].equals("--seed-resource")) {
            String resourceName = args[idx + 1];
            if (!resourceName.matches("[A-Za-z0-9][A-Za-z0-9._-]*\\.csv")) {
                throw new IllegalArgumentException("Некоректне ім'я ресурсу.");
            }
            Path out = RequestFiles.resolveInside(baseDir, args[idx + 2]);
            return new Parameters(baseDir, delimiter, maxBytes,
                    "--seed-resource", resourceName, "csv", null, "json", out, null);
        }
        if (remaining == 3 && args[idx].equals("--inspect")) {
            RequestFiles.requireFormat(args[idx + 1]);
            Path in = RequestFiles.resolveInside(baseDir, args[idx + 2]);
            return new Parameters(baseDir, delimiter, maxBytes,
                    "--inspect", null, args[idx + 1], in, null, null, null);
        }
        if (remaining == 5 && args[idx].equals("--convert")) {
            RequestFiles.requireFormat(args[idx + 1]);
            RequestFiles.requireFormat(args[idx + 3]);
            Path in = RequestFiles.resolveInside(baseDir, args[idx + 2]);
            Path out = RequestFiles.resolveInside(baseDir, args[idx + 4]);
            return new Parameters(baseDir, delimiter, maxBytes,
                    "--convert", null, args[idx + 1], in, args[idx + 3], out, null);
        }
        if (remaining == 2 && args[idx].equals("--probe")) {
            Path target = RequestFiles.resolveInside(baseDir, args[idx + 1]);
            return new Parameters(baseDir, delimiter, maxBytes,
                    "--probe", null, null, null, null, null, target);
        }

        throw new IllegalArgumentException("Формат аргументів.");
    }

    private static int execute(Parameters parameters, Logger logger) {
        if (parameters.mode().equals("--probe")) {
            return runProbe(parameters.probeTarget(), logger);
        }

        RequestFiles files = new RequestFiles(parameters.maxBytes(), parameters.delimiter());
        try {
            List<RequestFileRow> rows = parameters.input() == null
                    ? files.readResource(parameters.resourceName())
                    : files.read(parameters.input(), parameters.sourceFormat());
            if (parameters.output() != null) {
                files.write(parameters.output(), parameters.targetFormat(), rows);
            }
            RequestCatalog<AcademicRequest> catalog = new RequestCatalog<>();
            List<TranscriptRequest> requests = rows.stream()
                    .map(RequestFileRow::toRequest)
                    .toList();
            for (TranscriptRequest request : requests) {
                if (!catalog.add(request)) {
                    throw new IllegalStateException("Повторення ключа після перевірки.");
                }
            }
            RequestProcessor processor = new RequestProcessor(new StandardFeePolicy());
            var results = processor.processAll(requests);
            logger.info("EXCHANGE_OK count=" + rows.size());
            System.out.println("Прочитано заяв: " + rows.size());
            System.out.println("Зареєстровано заяв: " + catalog.size());
            System.out.println("Сума зборів: " + processor.totalFee(results).toPlainString() + " UAH");
            System.out.println("Заяв у черзі: " + catalog.pendingCount());
            return 0;
        } catch (ExchangeFormatException exception) {
            logger.log(Level.WARNING, "EXCHANGE_DATA_ERROR", exception);
            System.err.println("Помилка даних CSV або JSON.");
            return 3;
        } catch (IOException exception) {
            logger.log(Level.SEVERE, "EXCHANGE_IO_ERROR", exception);
            System.err.println("Помилка читання або запису.");
            return 4;
        }
    }

    private static int runProbe(Path targetDir, Logger logger) {
        Path tempDir = null;
        Path lowerFile = null;
        Path upperFile = null;
        try {
            if (Files.exists(targetDir) && !Files.isDirectory(targetDir)) {
                throw new IOException("Ціль дослідження не є каталогом.");
            }
            Files.createDirectories(targetDir);
            tempDir = Files.createTempDirectory(targetDir, "probe-");
            lowerFile = tempDir.resolve("report.txt");
            upperFile = tempDir.resolve("REPORT.txt");

            Files.writeString(lowerFile, "", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

            boolean sameCaseFile = false;
            try {
                Files.writeString(upperFile, "", StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            } catch (FileAlreadyExistsException existsException) {
                if (Files.isSameFile(lowerFile, upperFile)) {
                    sameCaseFile = true;
                } else {
                    throw existsException;
                }
            }

            FileStore store = Files.getFileStore(tempDir);
            boolean posixSupported = store.supportsFileAttributeView("posix");
            boolean dosSupported = store.supportsFileAttributeView("dos");

            logger.info("PROBE_OK");
            System.out.println("POSIX: " + posixSupported);
            System.out.println("DOS: " + dosSupported);
            System.out.println("Регістри: " + (sameCaseFile ? "спільний" : "різні"));
            return 0;
        } catch (IOException exception) {
            logger.log(Level.SEVERE, "EXCHANGE_IO_ERROR", exception);
            System.err.println("Помилка читання або запису.");
            return 4;
        } finally {
            deleteQuietly(upperFile);
            deleteQuietly(lowerFile);
            deleteQuietly(tempDir);
        }
    }

    private static void deleteQuietly(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
            }
        }
    }
}