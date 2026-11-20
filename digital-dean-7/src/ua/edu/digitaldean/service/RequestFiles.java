package ua.edu.digitaldean.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import ua.edu.digitaldean.model.RequestKey;

public final class RequestFiles {
    public static final int DEFAULT_MAX_BYTES = 1_048_576;
    private static final String RESOURCE_PREFIX = "/digital-dean/";
    private static final String RESOURCE_NAME_PATTERN = "[A-Za-z0-9][A-Za-z0-9._-]*\\.csv";

    private final int maxBytes;
    private final char csvDelimiter;

    public RequestFiles() {
        this(DEFAULT_MAX_BYTES, ',');
    }

    public RequestFiles(int maxBytes) {
        this(maxBytes, ',');
    }

    public RequestFiles(int maxBytes, char csvDelimiter) {
        if (maxBytes < 0 || maxBytes > DEFAULT_MAX_BYTES) {
            throw new IllegalArgumentException("Ліміт байтів має бути в межах 0..1048576.");
        }
        if (csvDelimiter != ',' && csvDelimiter != ';') {
            throw new IllegalArgumentException("Дозволені розділювачі CSV: ',' або ';'.");
        }
        this.maxBytes = maxBytes;
        this.csvDelimiter = csvDelimiter;
    }

    public static Path resolveInside(Path base, String value) {
        Objects.requireNonNull(base, "Базовий шлях не може бути null.");
        Objects.requireNonNull(value, "Значення шляху не може бути null.");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Шлях не може бути порожнім.");
        }
        Path baseNormalized = base.toAbsolutePath().normalize();
        Path relative = Path.of(value);
        if (relative.isAbsolute() || relative.getRoot() != null) {
            throw new IllegalArgumentException("Очікується відносний шлях без кореня.");
        }
        Path resolved = baseNormalized.resolve(relative).normalize();
        if (!resolved.startsWith(baseNormalized)) {
            throw new IllegalArgumentException("Шлях виходить за межі базового каталогу.");
        }
        return resolved;
    }

    public List<RequestFileRow> readResource() throws IOException {
        return readResource("requests.csv");
    }

    public List<RequestFileRow> readResource(String name) throws IOException {
        Objects.requireNonNull(name, "Ім'я ресурсу не може бути null.");
        if (!name.matches(RESOURCE_NAME_PATTERN)) {
            throw new IllegalArgumentException("Некоректне ім'я CSV-ресурсу.");
        }
        String resourcePath = RESOURCE_PREFIX + name;
        InputStream input = RequestFiles.class.getResourceAsStream(resourcePath);
        if (input == null) {
            throw new IOException("Ресурс застосунку відсутній: " + name);
        }
        try (InputStream managed = input) {
            String content = readAndDecodeUtf8(managed);
            try (Reader reader = new StringReader(content)) {
                return checked(new CsvCodec(csvDelimiter).read(reader));
            }
        }
    }

    public List<RequestFileRow> read(Path path, String format) throws IOException {
        Objects.requireNonNull(path, "Шлях не може бути null.");
        requireFormat(format);
        try (InputStream input = Files.newInputStream(path)) {
            String content = readAndDecodeUtf8(input);
            try (Reader reader = new StringReader(content)) {
                return checked(read(reader, format));
            }
        }
    }

    public void write(Path path, String format, List<RequestFileRow> rows) throws IOException {
        Objects.requireNonNull(path, "Шлях не може бути null.");
        requireFormat(format);
        List<RequestFileRow> snapshot = checked(rows);
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(
                path,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE)) {
            if (format.equals("csv")) {
                new CsvCodec(csvDelimiter).write(writer, snapshot);
            } else {
                new JsonCodec().write(writer, snapshot);
            }
        }
    }

    public static void requireFormat(String format) {
        Objects.requireNonNull(format, "Формат не може бути null.");
        if (!format.equals("csv") && !format.equals("json")) {
            throw new IllegalArgumentException("Формат має бути csv або json.");
        }
    }

    private String readAndDecodeUtf8(InputStream input) throws IOException {
        byte[] raw = input.readNBytes(maxBytes + 1);
        if (raw.length > maxBytes) {
            throw new ExchangeFormatException("Перевищено допустимий розмір входу у байтах.");
        }
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        CharBuffer decoded;
        try {
            decoded = decoder.decode(ByteBuffer.wrap(raw));
        } catch (CharacterCodingException exception) {
            throw new IOException("Помилка декодування UTF-8.", exception);
        }
        String text = decoded.toString();
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
            return text.substring(1);
        }
        return text;
    }

    private List<RequestFileRow> read(Reader reader, String format) throws IOException {
        return format.equals("csv")
                ? new CsvCodec(csvDelimiter).read(reader)
                : new JsonCodec().read(reader);
    }

    private static List<RequestFileRow> checked(List<RequestFileRow> rows)
            throws ExchangeFormatException {
        Objects.requireNonNull(rows, "Список рядків не може бути null.");
        for (RequestFileRow row : rows) {
            Objects.requireNonNull(row, "Елемент списку рядків не може бути null.");
        }
        List<RequestFileRow> snapshot = List.copyOf(rows);
        Set<RequestKey> keys = new HashSet<>();
        for (RequestFileRow row : snapshot) {
            if (!keys.add(new RequestKey(row.requestId()))) {
                throw new ExchangeFormatException("Повторний ключ заяви.");
            }
        }
        return snapshot;
    }
}