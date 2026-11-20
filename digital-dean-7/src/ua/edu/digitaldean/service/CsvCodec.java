package ua.edu.digitaldean.service;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import ua.edu.digitaldean.model.StudentId;

public final class CsvCodec {
    private static final List<String> HEADER = List.of(
            "requestId", "studentId", "language", "copies", "note");

    private final char delimiter;

    public CsvCodec() {
        this(',');
    }

    public CsvCodec(char delimiter) {
        if (delimiter != ',' && delimiter != ';') {
            throw new IllegalArgumentException("Дозволені розділювачі CSV: ',' або ';'.");
        }
        this.delimiter = delimiter;
    }

    public char delimiter() {
        return delimiter;
    }

    public List<RequestFileRow> read(Reader reader) throws IOException {
        Objects.requireNonNull(reader, "Джерело не може бути null.");
        StringWriter text = new StringWriter();
        reader.transferTo(text);
        List<List<String>> records = parse(text.toString(), delimiter);
        if (records.isEmpty() || !records.get(0).equals(HEADER)) {
            throw new ExchangeFormatException("CSV: некоректний заголовок.");
        }
        List<RequestFileRow> result = new ArrayList<>();
        for (int i = 1; i < records.size(); i++) {
            List<String> fields = records.get(i);
            try {
                if (fields.size() != 5 || !fields.get(3).strip().matches("[0-9]+")) {
                    throw new IllegalArgumentException("Некоректні поля.");
                }
                result.add(new RequestFileRow(
                        fields.get(0),
                        new StudentId(fields.get(1)),
                        fields.get(2),
                        Integer.parseInt(fields.get(3).strip()),
                        fields.get(4)));
            } catch (IllegalArgumentException exception) {
                throw new ExchangeFormatException(
                        "CSV: некоректний запис " + (i + 1) + ".", exception);
            }
        }
        return List.copyOf(result);
    }

    public void write(Writer writer, List<RequestFileRow> rows) throws IOException {
        Objects.requireNonNull(writer, "Призначення не може бути null.");
        List<RequestFileRow> snapshot = List.copyOf(rows);
        writeRecord(writer, HEADER, delimiter);
        for (RequestFileRow row : snapshot) {
            writeRecord(writer, List.of(
                    row.requestId(),
                    row.studentId().value(),
                    row.language(),
                    Integer.toString(row.copies()),
                    row.note()), delimiter);
        }
    }

    private static void writeRecord(Writer writer, List<String> fields, char delimiter)
            throws IOException {
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                writer.write(delimiter);
            }
            String field = fields.get(i);
            if (field.indexOf(delimiter) >= 0 || field.indexOf('"') >= 0
                    || field.indexOf('\r') >= 0 || field.indexOf('\n') >= 0) {
                writer.write('"');
                writer.write(field.replace("\"", "\"\""));
                writer.write('"');
            } else {
                writer.write(field);
            }
        }
        writer.write("\r\n");
    }

    private static List<List<String>> parse(String text, char delimiter)
            throws ExchangeFormatException {
        List<List<String>> records = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean closed = false;
        boolean active = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                        closed = true;
                    }
                } else {
                    field.append(c);
                }
                continue;
            }
            if (closed && c != delimiter && c != '\r' && c != '\n') {
                throw new ExchangeFormatException("CSV: текст після закритої лапки.");
            }
            if (c == '"') {
                if (field.length() != 0 || closed) {
                    throw new ExchangeFormatException("CSV: лапка всередині нецитованого поля.");
                }
                quoted = true;
                active = true;
            } else if (c == delimiter) {
                record.add(field.toString());
                field.setLength(0);
                closed = false;
                active = true;
            } else if (c == '\r' || c == '\n') {
                if (c == '\r') {
                    if (i + 1 >= text.length() || text.charAt(i + 1) != '\n') {
                        throw new ExchangeFormatException("CSV: одиночний CR між записами.");
                    }
                    i++;
                }
                record.add(field.toString());
                records.add(List.copyOf(record));
                record.clear();
                field.setLength(0);
                closed = false;
                active = false;
            } else {
                field.append(c);
                active = true;
            }
        }
        if (quoted) {
            throw new ExchangeFormatException("CSV: незакрите цитоване поле.");
        }
        if (active || !record.isEmpty() || closed) {
            record.add(field.toString());
            records.add(List.copyOf(record));
        }
        return records;
    }
}