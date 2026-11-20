package ua.edu.digitaldean.service;
import java.io.IOException;
import java.io.Serial;
public final class ExchangeFormatException extends IOException {
    @Serial
    private static final long serialVersionUID = 1L;
    public ExchangeFormatException(String message) {
        super(message);
    }
    public ExchangeFormatException(String message, IllegalArgumentException cause) { super(message, cause);
    }
}
