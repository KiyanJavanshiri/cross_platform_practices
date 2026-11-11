package ua.edu.digitaldean.model;

import java.math.BigDecimal;

public final class DormitoryRequest extends AcademicRequest {
    private final String building;
    private final int months;

    public DormitoryRequest(
            String requestId,
            StudentId studentId,
            String building,
            int months) {
        super(requestId, studentId);

        if (building == null || building.isBlank()) {
            throw new IllegalArgumentException(
                    "Номер корпусу не може бути порожнім.");
        }

        if (months < 1 || months > 12) {
            throw new IllegalArgumentException(
                    "Кількість місяців має бути від 1 до 12.");
        }

        this.building = building.strip();
        this.months = months;
    }

    public String building() {
        return building;
    }

    public int months() {
        return months;
    }

    @Override
    public String type() {
        return "Поселення в гуртожиток";
    }

    @Override
    public String details() {
        return "корпус: " + building + "; місяців: " + months;
    }

    @Override
    public BigDecimal baseFee() {
        return BigDecimal.valueOf(25L)
                .multiply(BigDecimal.valueOf(months));
    }
}