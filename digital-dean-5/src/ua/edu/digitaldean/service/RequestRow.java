package ua.edu.digitaldean.service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import ua.edu.digitaldean.model.RequestKey;
import ua.edu.digitaldean.model.StudentId;
public record RequestRow(
        RequestKey requestId,
        StudentId studentId,
        String type,
                BigDecimal fee) {
    public RequestRow {
        Objects.requireNonNull(requestId, "Ключ заяви не може бути null.");
        Objects.requireNonNull(studentId, "Ключ студента не може бути null.");
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Тип заяви не може бути порожнім.");
        }
        type = type.strip();
        Objects.requireNonNull(fee, "Збір не може бути null.");
        if (fee.signum() < 0) {
            throw new IllegalArgumentException("Збір не може бути від’ємним.");
        }
        fee = fee.setScale(2, RoundingMode.HALF_UP);
    }
    public String formattedLine() {
        return requestId.value() + " | " + studentId.value() + " | "
                + type + " | " + fee.toPlainString() + " UAH";

    }
}