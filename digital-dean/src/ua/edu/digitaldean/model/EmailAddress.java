package ua.edu.digitaldean.model;

import java.util.Locale;

public record EmailAddress(String value) {
    public EmailAddress {
        if (value == null || value.strip().isEmpty()) {
            throw new IllegalArgumentException(
                    "Електронна адреса не може бути порожньою.");
        }

        value = value.strip().toLowerCase(Locale.ROOT);

        long atCount = value.chars()
                .filter(ch -> ch == '@')
                .count();

        if (atCount != 1
                || value.startsWith("@")
                || value.endsWith("@")
                || value.chars().anyMatch(Character::isWhitespace)) {

            throw new IllegalArgumentException(
                    "Електронна адреса має містити один символ @ між локальною та доменною частинами без пробілів.");
        }
    }
}