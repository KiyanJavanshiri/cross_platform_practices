package ua.edu.digitaldean.model;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

public abstract class AcademicRequest {
    private final String requestId;
    private final StudentId studentId;
    protected AcademicRequest(String requestId, StudentId studentId) { if (requestId == null || requestId.isBlank()) {
        throw new IllegalArgumentException(
                "Ідентифікатор заяви не може бути порожнім.");
    }
        String normalizedId = requestId.strip().toUpperCase(Locale.ROOT); if (!normalizedId.matches("REQ-\\d{3}")) {
            throw new IllegalArgumentException(
                    "Ідентифікатор заяви має формат REQ-номер.");
        }
        this.requestId = normalizedId;
        this.studentId = Objects.requireNonNull(
                studentId, "Ідентифікатор студента не може бути null.");
    }
    public final String requestId() {
        return requestId;
    }
    public final StudentId studentId() {
        return studentId;
    }
    public abstract String type();
    public abstract String details();
    public abstract BigDecimal baseFee();
    public final String summary() {
        return requestId + " | " + type() + " | "
                + studentId.value() + " | " + details();
    }
}
