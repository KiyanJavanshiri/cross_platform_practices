package ua.edu.digitaldean.model;

import java.util.Locale;

public final class Course {
    private final String code;
    private final String title;
    private final int ects;

    public Course(String code, String title, int ects) {
        this.code = normalizeCode(code);

        this.title = requireText(
                title,
                "Назва дисципліни не може бути порожньою.");

        if (ects < 1 || ects > 30) {
            throw new IllegalArgumentException(
                    "Кількість ECTS має бути від 1 до 30.");
        }

        this.ects = ects;
    }

    public String code() {
        return code;
    }

    public String title() {
        return title;
    }

    public int ects() {
        return ects;
    }

    private static String normalizeCode(String value) {
        String normalized = requireText(
                value,
                "Код дисципліни не може бути порожнім.")
                .toUpperCase(Locale.ROOT);

        if (!normalized.matches("[A-Z]{2,5}\\d{3}")) {
            throw new IllegalArgumentException(
                    "Код дисципліни має містити 2–5 літер і три цифри.");
        }

        return normalized;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.strip();
    }

    @Override
    public String toString() {
        return "%s — %s (%d ECTS)".formatted(code, title, ects);
    }
}