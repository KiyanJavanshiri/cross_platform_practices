package ua.edu.digitaldean.service;

import java.math.BigDecimal;
import ua.edu.digitaldean.model.AcademicRequest;

@FunctionalInterface
public interface FeePolicy {
    BigDecimal calculate(AcademicRequest request);
}
