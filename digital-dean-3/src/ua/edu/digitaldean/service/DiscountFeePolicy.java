package ua.edu.digitaldean.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import ua.edu.digitaldean.model.AcademicRequest;

public final class DiscountFeePolicy implements FeePolicy {
    private final FeePolicy basePolicy;
    private final int discountPercent;

    public DiscountFeePolicy(
            FeePolicy basePolicy,
            int discountPercent) {
        this.basePolicy = Objects.requireNonNull(
                basePolicy, "Базова політика не може бути null.");

        if (discountPercent < 0 || discountPercent > 100) {
            throw new IllegalArgumentException(
                    "Відсоток знижки має бути від 0 до 100.");
        }

        this.discountPercent = discountPercent;
    }

    @Override
    public BigDecimal calculate(AcademicRequest request) {
        BigDecimal baseFee = basePolicy.calculate(request);

        BigDecimal discountMultiplier = BigDecimal.valueOf(
                        100L - discountPercent)
                .divide(BigDecimal.valueOf(100L), 4, RoundingMode.HALF_UP);

        return baseFee.multiply(discountMultiplier)
                .setScale(2, RoundingMode.HALF_UP);
    }
}