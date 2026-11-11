package ua.edu.digitaldean.model;

import java.util.Locale;
public record StudentId(String value) {
    public StudentId {
        value = ModelRules.requireText(
                        value, "Ідентифікатор студента не може бути порожнім.")
                .toUpperCase(Locale.ROOT);
        if (!value.matches("ST-\\d{4}-\\d{4}")) {
            throw new IllegalArgumentException(
                    "Ідентифікатор має формат ST-рік-номер.");
        }
    }
}
