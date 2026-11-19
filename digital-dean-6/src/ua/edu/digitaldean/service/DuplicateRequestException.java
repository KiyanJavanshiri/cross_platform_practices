package ua.edu.digitaldean.service;

import java.io.Serial;
import java.util.Objects;
import ua.edu.digitaldean.model.RequestKey;

public final class DuplicateRequestException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final RequestKey key;

    public DuplicateRequestException(RequestKey key) {
        super("Повторний ідентифікатор заяви.");
        this.key = Objects.requireNonNull(key, "Ключ заяви не може бути null.");
    }

    public RequestKey key() {
        return key;
    }
}