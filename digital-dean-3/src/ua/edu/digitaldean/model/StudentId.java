package ua.edu.digitaldean.model;

import java.util.Locale;

public record StudentId(String value) {
    public StudentId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Ідентифікатор студента не може бути порожнім.");
        }
        value = value.strip().toUpperCase(Locale.ROOT);
        if (!value.matches("ST-\\d{4}-\\d{4}")) {
            throw new IllegalArgumentException(
                    "Ідентифікатор має формат ST-рік-номер.");
        }
    }
}
