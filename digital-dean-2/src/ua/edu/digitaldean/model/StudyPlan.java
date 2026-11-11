package ua.edu.digitaldean.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class StudyPlan {
    private final String programCode;
    private final List<Course> courses;
    public StudyPlan(String programCode, List<Course> courses) { this.programCode = ModelRules.requireText(
            programCode, "Код програми не може бути порожнім."); if (courses == null || courses.isEmpty()) {
        throw new IllegalArgumentException(
                "План має містити хоча б одну дисципліну.");
    }
        List<Course> snapshot = List.copyOf(courses);
        ensureUniqueCodes(snapshot);
        this.courses = snapshot;
    }
    public String programCode() {
        return programCode;
    }
    public List<Course> courses() {
        return courses;
    }
    public int totalEcts() {
        int total = 0;
        for (Course course : courses) {
            total += course.ects();
        }
        return total;
    }
    public StudyPlan withCourse(Course course) {
        if (course == null) {
            throw new IllegalArgumentException(
                    "Дисципліна не може бути null.");
        }
        List<Course> extended = new ArrayList<>(courses);
        extended.add(course);
        return new StudyPlan(programCode, extended);
    }
    private static void ensureUniqueCodes(List<Course> courses) { Set<String> codes = new HashSet<>();
        for (Course course : courses) {
            if (!codes.add(course.code())) {
                throw new IllegalArgumentException(
                        "Коди дисциплін у плані не можуть повторюватися.");
            }
        }
    }
    @Override
    public String toString() {
        return "%s | дисциплін: %d | ECTS: %d".formatted(
                programCode, courses.size(), totalEcts());
    }
}
