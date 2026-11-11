package ua.edu.digitaldean.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ua.edu.digitaldean.model.Course;
import ua.edu.digitaldean.model.CourseResult;
import ua.edu.digitaldean.model.EmailAddress;
import ua.edu.digitaldean.model.StudentId;
import ua.edu.digitaldean.model.StudentProfile;
import ua.edu.digitaldean.model.StudyPlan;
import ua.edu.digitaldean.model.Transcript;

public final class DigitalDeanApp {
    private static final String USAGE =
            "Використання: DigitalDeanApp <studentId> \"<fullName>\" <email> "
                    + "<programCode> (<courseCode> \"<courseTitle>\" <ects> <grade>)+";

    private DigitalDeanApp() {
    }

    public static void main(String[] args) {

        try {
            if (args.length < 8 || (args.length - 4) % 4 != 0) {
                System.err.println(USAGE);
                return;
            }

            StudentId studentId = new StudentId(args[0]);
            String fullName = args[1];
            EmailAddress email = new EmailAddress(args[2]);
            String programCode = args[3];
            List<Course> sourceCourses = new ArrayList<>();
            List<CourseResult> sourceResults = new ArrayList<>();

            for (int index = 4; index < args.length; index += 4) {

                String courseCode = args[index];
                String courseTitle = args[index + 1];
                int ects = Integer.parseInt(args[index + 2]);
                int grade = Integer.parseInt(args[index + 3]);
                Course course = new Course(
                        courseCode, courseTitle, ects);
                sourceCourses.add(course);
                sourceResults.add(
                        new CourseResult(course, grade));
            }

            StudyPlan studyPlan = new StudyPlan(
                    programCode, sourceCourses);
            Transcript transcript = new Transcript(sourceResults);

            sourceCourses.clear();
            sourceResults.clear();

            StudentProfile profile = new StudentProfile(
                    studentId,
                    fullName,
                    email,
                    studyPlan,
                    transcript
            );

            System.out.println("Профіль: %s | %s | %s".formatted(
                    profile.id().value(),
                    profile.fullName(),
                    profile.email().value()
            ));

            System.out.println("План: " + profile.studyPlan());
            System.out.println(
                    "Транскрипт: результатів: %d | середній бал: %s".formatted(
                            profile.transcript().results().size(),
                            String.format(
                                    Locale.ROOT,
                                    "%.2f",
                                    profile.transcript().averageGrade()
                            )
                    )
            );

            for (CourseResult result : profile.transcript().results()) {
                System.out.println("%s | %d".formatted(
                        result.course().code(),
                        result.grade()
                ));
            }

        } catch (NumberFormatException exception) {
            System.err.println(
                    "Помилка: ECTS та оцінка мають бути цілими числами.");

        } catch (IllegalArgumentException exception) {
            System.err.println("Помилка: " + exception.getMessage());
        }
    }
}