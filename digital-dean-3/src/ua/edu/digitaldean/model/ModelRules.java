package ua.edu.digitaldean.model;

final class ModelRules {
    private ModelRules() {
    }
    static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }
}
