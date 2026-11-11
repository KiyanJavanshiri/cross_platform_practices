package ua.edu.digitaldean.model;

import java.math.BigDecimal;
import java.util.Locale;

public final class EnrollmentRequest extends AcademicRequest { private final String courseCode;
    public EnrollmentRequest(
            String requestId, StudentId studentId, String courseCode) {
        super(requestId, studentId);
        if (courseCode == null || courseCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Код дисципліни не може бути порожнім.");
        }
        String normalizedCode = courseCode.strip().toUpperCase(Locale.ROOT); if (!normalizedCode.matches("[A-Z]{2,5}\\d{3}")) {
            throw new IllegalArgumentException(
                    "Код дисципліни має містити 2–5 літер і 3 цифри.");
        }
        this.courseCode = normalizedCode;
    }
    public String courseCode() {
        return courseCode;
    }
    @Override
    public String type() {
        return "Зарахування на дисципліну";
    }
    @Override
    public String details() {
        return courseCode;
    }
    @Override
    public BigDecimal baseFee() {
        return BigDecimal.ZERO;
    }
}
