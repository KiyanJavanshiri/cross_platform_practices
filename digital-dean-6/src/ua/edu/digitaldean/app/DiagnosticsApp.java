package ua.edu.digitaldean.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import ua.edu.digitaldean.model.AcademicRequest;
import ua.edu.digitaldean.model.TranscriptRequest;
import ua.edu.digitaldean.service.DiagnosticLog;
import ua.edu.digitaldean.service.DuplicateRequestException;
import ua.edu.digitaldean.service.ImportReport;
import ua.edu.digitaldean.service.RequestAnalytics;
import ua.edu.digitaldean.service.RequestCatalog;
import ua.edu.digitaldean.service.RequestDataException;
import ua.edu.digitaldean.service.RequestImporter;
import ua.edu.digitaldean.service.RequestProcessor;
import ua.edu.digitaldean.service.StandardFeePolicy;

public final class DiagnosticsApp {
    private static final String DEMO = """
            REQ-003;ST-2025-0042;uk;2
            REQ-002;ST-2025-0007;en;1
            REQ-006;ST-2025-0099;uk;3
            """;

    private static final Set<String> ALLOWED_LEVELS = Set.of(
            "FINE", "INFO", "WARNING", "SEVERE", "OFF");

    private record Parameters(String mode, Path path, Level level) {
    }

    private static final class AuditProbe implements AutoCloseable {
        private final String name;
        private final List<String> closed;
        private final boolean throwOnClose;

        private AuditProbe(String name, List<String> closed, boolean throwOnClose) {
            this.name = name;
            this.closed = closed;
            this.throwOnClose = throwOnClose;
        }

        @Override
        public void close() throws IOException {
            closed.add(name);
            if (throwOnClose) {
                throw new IOException("Закриття " + name);
            }
        }
    }

    private DiagnosticsApp() {
    }

    public static void main(String[] args) {
        Parameters parameters;
        try {
            parameters = parseArguments(args);
        } catch (IllegalArgumentException exception) {
            System.err.println("Помилка: некоректні аргументи.");
            System.exit(2);
            return;
        }

        if (parameters.mode().startsWith("--resources")) {
            demonstrateResources(parameters.mode());
            return;
        }

        Logger logger = DiagnosticLog.create(parameters.level());
        int code;
        try {
            code = importRequests(parameters, logger);
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

    private static Parameters parseArguments(String[] args) {
        if (args == null) {
            throw new IllegalArgumentException("Аргументи відсутні.");
        }
        for (String arg : args) {
            if (arg == null) {
                throw new IllegalArgumentException("Аргумент не може бути null.");
            }
        }

        int idx = 0;
        Level level = Level.INFO;
        if (idx < args.length && args[idx].equals("--level")) {
            if (idx + 1 >= args.length || !ALLOWED_LEVELS.contains(args[idx + 1])) {
                throw new IllegalArgumentException("Некоректний рівень журналу.");
            }
            level = Level.parse(args[idx + 1]);
            idx += 2;
        }

        int remaining = args.length - idx;
        if (remaining == 0 || (remaining == 1 && args[idx].equals("--demo"))) {
            return new Parameters("--demo", null, level);
        }
        if (remaining == 1 && args[idx].equals("--resources")) {
            return new Parameters("--resources body", null, level);
        }
        if (remaining == 2 && args[idx].equals("--resources")
                && (args[idx + 1].equals("body")
                || args[idx + 1].equals("close-only")
                || args[idx + 1].equals("ok"))) {
            return new Parameters("--resources " + args[idx + 1], null, level);
        }
        if (remaining == 2
                && (args[idx].equals("--file") || args[idx].equals("--tolerant"))
                && !args[idx + 1].isBlank()) {
            return new Parameters(args[idx], Path.of(args[idx + 1]), level);
        }

        throw new IllegalArgumentException("Формат аргументів.");
    }

    private static List<TranscriptRequest> loadStrict(Parameters parameters)
            throws IOException, RequestDataException {
        RequestImporter importer = new RequestImporter();
        if (parameters.mode().equals("--file")) {
            return importer.read(parameters.path());
        }
        try (BufferedReader reader = new BufferedReader(new StringReader(DEMO))) {
            return importer.read(reader);
        }
    }

    private static ImportReport loadTolerant(Path path) throws IOException {
        RequestImporter importer = new RequestImporter();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            return importer.readAll(reader);
        }
    }

    private static int importRequests(Parameters parameters, Logger logger) {
        logger.fine("IMPORT_START");
        RequestImporter importer = new RequestImporter();
        RequestCatalog<AcademicRequest> catalog = new RequestCatalog<>();
        RequestAnalytics analytics = new RequestAnalytics(
                new RequestProcessor(new StandardFeePolicy()));

        try {
            if (parameters.mode().equals("--tolerant")) {
                ImportReport report = loadTolerant(parameters.path());
                int added = importer.registerUnique(report.accepted(), catalog);
                for (RequestDataException rejectedError : report.rejected()) {
                    logger.log(
                            Level.WARNING,
                            "IMPORT_LINE_REJECTED line=" + rejectedError.lineNumber(),
                            rejectedError);
                }
                var rows = analytics.project(catalog.snapshot());
                int rejectedCount = report.rejected().size();
                logger.info("IMPORT_OK read=" + report.accepted().size()
                        + " registered=" + added
                        + " rejected=" + rejectedCount);
                System.out.println("Прочитано заяв: " + report.accepted().size());
                System.out.println("Зареєстровано заяв: " + added);
                System.out.println("Відхилено рядків: " + rejectedCount);
                System.out.println("Сума зборів: " + analytics.totalFee(rows).toPlainString() + " UAH");
                System.out.println("Заяв у черзі: " + catalog.pendingCount());
                return rejectedCount > 0 ? 5 : 0;
            }

            List<TranscriptRequest> requests = loadStrict(parameters);
            int added = importer.registerUnique(requests, catalog);
            var rows = analytics.project(catalog.snapshot());
            logger.info("IMPORT_OK read=" + requests.size() + " registered=" + added);
            System.out.println("Прочитано заяв: " + requests.size());
            System.out.println("Зареєстровано заяв: " + added);
            System.out.println("Сума зборів: " + analytics.totalFee(rows).toPlainString() + " UAH");
            System.out.println("Заяв у черзі: " + catalog.pendingCount());
            return 0;
        } catch (RequestDataException exception) {
            logger.log(Level.WARNING, "IMPORT_DATA_ERROR", exception);
            System.err.println("Помилка даних: рядок " + exception.lineNumber() + ".");
            return 3;
        } catch (IOException exception) {
            logger.log(Level.SEVERE, "IMPORT_IO_ERROR", exception);
            System.err.println("Помилка введення-виведення.");
            return 4;
        } catch (DuplicateRequestException exception) {
            logger.log(Level.WARNING, "IMPORT_DUPLICATE_ERROR", exception);
            System.err.println("Помилка: повторний ідентифікатор заяви.");
            return 6;
        }
    }

    @SuppressWarnings("try")
    private static void demonstrateResources(String resourceMode) {
        List<String> closed = new ArrayList<>();
        boolean throwOnClose = !resourceMode.equals("--resources ok");
        boolean throwInBody = resourceMode.equals("--resources body");

        try (AuditProbe first = new AuditProbe("A", closed, throwOnClose);
             AuditProbe second = new AuditProbe("B", closed, throwOnClose)) {
            if (throwInBody) {
                throw new IOException("Помилка тіла");
            }
        } catch (IOException exception) {
            System.out.println("Закриття ресурсів: " + String.join(", ", closed));
            System.out.println("Основний виняток: " + exception.getClass().getSimpleName());
            System.out.println("Повідомлення: " + exception.getMessage());
            System.out.println("Пригнічених: " + exception.getSuppressed().length);
            for (Throwable suppressed : exception.getSuppressed()) {
                System.out.println("Пригнічений: " + suppressed.getMessage());
            }
            return;
        }

        System.out.println("Закриття ресурсів: " + String.join(", ", closed));
        System.out.println("Основний виняток: НЕМАЄ");
        System.out.println("Пригнічених: 0");
    }
}