package ua.edu.digitaldean.model;

import java.util.Locale;
import java.util.Objects;

public record RequestKey(String value) implements Comparable<RequestKey> {
    public RequestKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Ідентифікатор заяви не може бути порожнім.");

        }
        value = value.strip().toUpperCase(Locale.ROOT);
        if (!value.matches("REQ-\\d{3}")) {
            throw new IllegalArgumentException(
                    "Ідентифікатор заяви має формат REQ-номер.");

        }
    }
    @Override
    public int compareTo(RequestKey other) {
        Objects.requireNonNull(other, "Ключ для порівняння не може бути null.");
        return value.compareTo(other.value);
    }
}