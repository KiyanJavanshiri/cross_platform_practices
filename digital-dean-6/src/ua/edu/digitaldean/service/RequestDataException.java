package ua.edu.digitaldean.service;
import java.io.Serial;
import java.util.Objects;
public final class RequestDataException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;
    private final int lineNumber;
    public RequestDataException(int lineNumber, IllegalArgumentException cause) {
        super("Рядок " + lineNumber + ": некоректні дані.",
                Objects.requireNonNull(cause, "Причина не може бути null."));
        if (lineNumber < 1) {
            throw new IllegalArgumentException("Номер рядка має бути додатним.");

        }
        this.lineNumber = lineNumber;
    }
    public int lineNumber() {
        return lineNumber;
    }
}