package ua.edu.digitaldean.model;

public record CourseResult(Course course, int grade) {

    public CourseResult {

        if (course == null) {
            throw new IllegalArgumentException(
                    "Дисципліна результату не може бути null.");
        }

        if (grade < 0 || grade > 100) {
            throw new IllegalArgumentException(
                    "Оцінка має бути від 0 до 100.");
        }
    }
}