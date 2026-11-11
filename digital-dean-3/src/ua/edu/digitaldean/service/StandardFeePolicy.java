package ua.edu.digitaldean.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import ua.edu.digitaldean.model.AcademicRequest;

public final class StandardFeePolicy implements FeePolicy {
    @Override
    public BigDecimal calculate(AcademicRequest request) {
        Objects.requireNonNull(request, "Заява не може бути null.");
        BigDecimal fee = request.baseFee();
        if (fee == null || fee.signum() < 0) {
            throw new IllegalStateException(
                    "Базовий збір має бути невід’ємним числом.");
        }
        return fee.setScale(2, RoundingMode.HALF_UP);
    }
}
