package ua.edu.digitaldean.service;

import java.math.BigDecimal;
import java.util.Objects;

public record ProcessingResult(String summary, BigDecimal fee) {
    public ProcessingResult {
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException(
                    "Підсумок оброблення не може бути порожнім.");
        }
        Objects.requireNonNull(fee, "Сума збору не може бути null.");
        if (fee.signum() < 0) {
            throw new IllegalArgumentException(
                    "Сума збору не може бути від’ємною.");
        }
    }
    public String formattedLine() {
        return summary + " | збір: " + fee.toPlainString() + " UAH";
    }
}
