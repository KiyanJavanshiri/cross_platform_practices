package ua.edu.digitaldean.model;

public final class Student {
    public static final int MAX_COMPLETED_CREDITS = 240;

    private final StudentId id;
    private final EmailAddress email;
    private String fullName;
    private int completedCredits;

    public Student(
            StudentId id,
            String fullName,
            EmailAddress email,
            int completedCredits) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "Ідентифікатор студента не може бути null.");
        }

        if (email == null) {
            throw new IllegalArgumentException(
                    "Електронна адреса студента не може бути null.");
        }

        if (completedCredits < 0
                || completedCredits > MAX_COMPLETED_CREDITS) {
            throw new IllegalArgumentException(
                    "Виконані кредити мають бути від 0 до 240.");
        }

        this.id = id;
        this.fullName = normalizeFullName(fullName);
        this.email = email;
        this.completedCredits = completedCredits;
    }

    public StudentId id() {
        return id;
    }

    public String fullName() {
        return fullName;
    }

    public EmailAddress email() {
        return email;
    }

    public int completedCredits() {
        return completedCredits;
    }

    public void rename(String newFullName) {
        fullName = normalizeFullName(newFullName);
    }

    public void completeCourse(Course course) {
        if (course == null) {
            throw new IllegalArgumentException(
                    "Дисципліна не може бути null.");
        }

        int credits = course.ects();

        if (completedCredits > MAX_COMPLETED_CREDITS - credits) {
            throw new IllegalArgumentException(
                    "Загальна кількість кредитів не може перевищувати 240.");
        }

        completedCredits += credits;
    }

    private static String normalizeFullName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "ПІБ студента не може бути порожнім.");
        }

        return value.strip();
    }

    @Override
    public String toString() {
        return "%s | %s | виконано кредитів: %d".formatted(
                id.value(),
                fullName,
                completedCredits);
    }
}