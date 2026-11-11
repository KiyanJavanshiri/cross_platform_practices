package ua.edu.digitaldean.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Transcript {

    private final List<CourseResult> results;

    public Transcript(List<CourseResult> results) {

        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException(
                    "Транскрипт має містити хоча б один результат.");
        }

        List<CourseResult> snapshot = List.copyOf(results);
        ensureUniqueCourseCodes(snapshot);
        this.results = snapshot;
    }

    public List<CourseResult> results() {
        return results;
    }

    public double averageGrade() {
        int total = 0;
        for (CourseResult result : results) {
            total += result.grade();
        }

        return (double) total / results.size();
    }

    public Transcript withResult(CourseResult result) {

        if (result == null) {
            throw new IllegalArgumentException(
                    "Результат дисципліни не може бути null.");
        }

        List<CourseResult> extended = new ArrayList<>(results);

        extended.add(result);

        return new Transcript(extended);
    }

    private static void ensureUniqueCourseCodes(
            List<CourseResult> results) {

        Set<String> codes = new HashSet<>();

        for (CourseResult result : results) {

            if (!codes.add(result.course().code())) {
                throw new IllegalArgumentException(
                        "Результат дисципліни з таким кодом уже існує.");
            }
        }
    }
}