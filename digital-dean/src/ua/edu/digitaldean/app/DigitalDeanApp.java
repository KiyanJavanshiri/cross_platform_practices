package ua.edu.digitaldean.app;

import ua.edu.digitaldean.model.Course;
import ua.edu.digitaldean.model.EmailAddress;
import ua.edu.digitaldean.model.Enrollment;
import ua.edu.digitaldean.model.Semester;
import ua.edu.digitaldean.model.Student;
import ua.edu.digitaldean.model.StudentId;

public final class DigitalDeanApp {

    private DigitalDeanApp() { }

    public static void main(String[] args) {

        if (args.length != 9) {
            System.err.println(
                    "Використання: DigitalDeanApp <studentId> \"<fullName>\" <email> <completedCredits> <courseCode> \"<courseTitle>\" <ects> <year> <term>");
            return;
        }

        try {
            StudentId studentId = new StudentId(args[0]);

            String fullName = args[1];

            EmailAddress email = new EmailAddress(args[2]);

            int completedCredits = Integer.parseInt(args[3]);

            Course course = new Course(
                    args[4],
                    args[5],
                    Integer.parseInt(args[6]));

            Semester semester = new Semester(
                    Integer.parseInt(args[7]),
                    Integer.parseInt(args[8]));

            Student student = new Student(
                    studentId,
                    fullName,
                    email,
                    completedCredits);

            Enrollment enrollment = new Enrollment(
                    student.id(),
                    course,
                    semester);

            String studentSummary = student.toString();
            String contact = student.email().value();
            String enrollmentSummary = enrollment.summary();

            student.completeCourse(course);

            System.out.println("Студент: " + studentSummary);

            System.out.println("Контакт: " + contact);

            System.out.println("Реєстрація: " + enrollmentSummary);

            System.out.println(
                    "Кредити після завершення дисципліни: "
                            + student.completedCredits());

        } catch (NumberFormatException e) {

            System.err.println(
                    "Помилка: числові аргументи мають містити цілі числа.");

        } catch (IllegalArgumentException e) {

            System.err.println("Помилка: " + e.getMessage());
        }
    }
}