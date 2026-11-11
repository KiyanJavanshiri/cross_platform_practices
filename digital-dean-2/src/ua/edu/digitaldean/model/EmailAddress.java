package ua.edu.digitaldean.model;

import java.util.Locale;

public record EmailAddress(String value) {
    public EmailAddress {
        value = ModelRules.requireText(
                        value, "Електронна адреса не може бути порожньою.")
                .toLowerCase(Locale.ROOT);
        int at = value.indexOf('@');
        if (at <= 0
                || at != value.lastIndexOf('@')
                || at == value.length() - 1
                || containsWhitespace(value)) {
            throw new IllegalArgumentException(
                    "Електронна адреса має містити один символ @ "
                            + "між локальною та доменною частинами "
                            + "без пробілів.");
        }
    }

    private static boolean containsWhitespace(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isWhitespace(value.charAt(index))) {
                return true;
            }
        }
        return false;
    }

}
