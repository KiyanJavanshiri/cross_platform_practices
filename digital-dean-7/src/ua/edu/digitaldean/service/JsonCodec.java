package ua.edu.digitaldean.service;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import ua.edu.digitaldean.model.StudentId;

public final class JsonCodec {
    public List<RequestFileRow> read(Reader reader) throws IOException {
        Objects.requireNonNull(reader, "Джерело не може бути null.");
        StringWriter text = new StringWriter();
        reader.transferTo(text);
        return new Parser(text.toString()).readArray();
    }

    public void write(Writer writer, List<RequestFileRow> rows) throws IOException {
        Objects.requireNonNull(writer, "Призначення не може бути null.");
        List<RequestFileRow> snapshot = List.copyOf(rows);
        writer.write("[\n");
        for (int i = 0; i < snapshot.size(); i++) {
            RequestFileRow row = snapshot.get(i);
            writer.write("  {\"requestId\":" + quote(row.requestId())
                    + ",\"studentId\":" + quote(row.studentId().value())
                    + ",\"language\":" + quote(row.language())
                    + ",\"copies\":" + row.copies()
                    + ",\"note\":" + quote(row.note()) + "}");
            writer.write(i + 1 < snapshot.size() ? ",\n" : "\n");
        }
        writer.write("]\n");
    }

    private static String quote(String text) {
        StringBuilder result = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (c < 0x20) {
                        result.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        result.append(c);
                    }
                }
            }
        }
        return result.append('"').toString();
    }

    private static final class Parser {
        private final String text;
        private int position;

        private Parser(String text) {
            this.text = text;
        }

        private List<RequestFileRow> readArray() throws ExchangeFormatException {
            expect('[');
            List<RequestFileRow> result = new ArrayList<>();
            if (!take(']')) {
                do {
                    result.add(readObject());
                } while (take(','));
                expect(']');
            }
            skipWhitespace();
            if (position != text.length()) {
                throw error();
            }
            return List.copyOf(result);
        }

        private RequestFileRow readObject() throws ExchangeFormatException {
            expect('{');
            String requestId = null;
            String studentId = null;
            String language = null;
            String note = null;
            int copies = 0;
            Set<String> seen = new HashSet<>();
            if (!take('}')) {
                do {
                    String key = readString();
                    if (!seen.add(key)) {
                        throw error();
                    }
                    expect(':');
                    switch (key) {
                        case "requestId" -> requestId = readString();
                        case "studentId" -> studentId = readString();
                        case "language" -> language = readString();
                        case "note" -> note = readString();
                        case "copies" -> copies = readInteger();
                        default -> throw error();
                    }
                } while (take(','));
                expect('}');
            }
            if (seen.size() != 5) {
                throw error();
            }
            try {
                return new RequestFileRow(
                        requestId, new StudentId(studentId), language, copies, note);
            } catch (IllegalArgumentException exception) {
                throw new ExchangeFormatException("JSON: порушено правила даних.", exception);
            }
        }

        private int readInteger() throws ExchangeFormatException {
            skipWhitespace();
            int start = position;
            if (position < text.length() && text.charAt(position) == '-') {
                position++;
            }
            if (position >= text.length() || !digit(text.charAt(position))) {
                throw error();
            }
            if (text.charAt(position) == '0') {
                position++;
            } else {
                while (position < text.length() && digit(text.charAt(position))) {
                    position++;
                }
            }
            try {
                return Integer.parseInt(text.substring(start, position));
            } catch (NumberFormatException exception) {
                throw new ExchangeFormatException("JSON: ціле число поза діапазоном int.", exception);
            }
        }

        private String readString() throws ExchangeFormatException {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (position < text.length()) {
                char c = text.charAt(position++);
                if (c == '"') {
                    return result.toString();
                }
                if (c < 0x20) {
                    throw error();
                }
                if (c != '\\') {
                    result.append(c);
                    continue;
                }
                if (position == text.length()) {
                    throw error();
                }
                char escape = text.charAt(position++);
                switch (escape) {
                    case '"', '\\', '/' -> result.append(escape);
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> {
                        if (position + 4 > text.length()) {
                            throw error();
                        }
                        String hex = text.substring(position, position + 4);
                        if (!hex.matches("[0-9a-fA-F]{4}")) {
                            throw error();
                        }
                        result.append((char) Integer.parseInt(hex, 16));
                        position += 4;
                    }
                    default -> throw error();
                }
            }
            throw error();
        }

        private void expect(char expected) throws ExchangeFormatException {
            if (!take(expected)) {
                throw error();
            }
        }

        private boolean take(char expected) {
            skipWhitespace();
            if (position < text.length() && text.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (position < text.length() && " \t\r\n".indexOf(text.charAt(position)) >= 0) {
                position++;
            }
        }

        private static boolean digit(char c) {
            return c >= '0' && c <= '9';
        }

        private ExchangeFormatException error() {
            return new ExchangeFormatException(
                    "JSON: некоректна структура, позиція " + (position + 1) + ".");
        }
    }
}