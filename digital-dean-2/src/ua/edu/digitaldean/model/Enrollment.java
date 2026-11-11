package ua.edu.digitaldean.model;

public final class Enrollment {
    private final StudentId studentId;
    private final Course course;
    private final Semester semester;

    public Enrollment(
            StudentId studentId,
            Course course,
            Semester semester) {

        if (studentId == null) {
            throw new IllegalArgumentException(
                    "Ідентифікатор реєстрації не може бути null.");
        }

        if (course == null) {
            throw new IllegalArgumentException(
                    "Дисципліна реєстрації не може бути null.");
        }

        if (semester == null) {
            throw new IllegalArgumentException(
                    "Семестр реєстрації не може бути null.");
        }

        this.studentId = studentId;
        this.course = course;
        this.semester = semester;
    }

    public StudentId studentId() {
        return studentId;
    }

    public Course course() {
        return course;
    }

    public Semester semester() {
        return semester;
    }

    public String summary() {
        return "%s | %s | %s".formatted(
                studentId.value(),
                course.code(),
                semester);
    }
}