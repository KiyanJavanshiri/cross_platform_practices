package ua.edu.digitaldean.model;

import java.util.Locale;

public record Course(String code, String title, int ects) {
    public Course {
        code = ModelRules.requireText(
                        code, "Код дисципліни не може бути порожнім.")
                .toUpperCase(Locale.ROOT);
        title = ModelRules.requireText(
                title, "Назва дисципліни не може бути порожньою.");
        if (!code.matches("[A-Z]{2,5}\\d{3}")) {
            throw new IllegalArgumentException(
                    "Код дисципліни має містити 2–5 літер і три цифри.");
        }
        if (ects < 1 || ects > 30) {
            throw new IllegalArgumentException(
                    "Кількість ECTS має бути від 1 до 30.");
        }
    }
}
