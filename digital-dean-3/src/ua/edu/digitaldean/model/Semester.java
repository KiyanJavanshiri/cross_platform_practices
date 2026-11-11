package ua.edu.digitaldean.model;

public record Semester(int year, int term) {
    public Semester {
        if (year < 2020 || year > 2100) {
            throw new IllegalArgumentException(
                    "Рік семестру має бути від 2020 до 2100.");
        }

        if (term != 1 && term != 2) {
            throw new IllegalArgumentException(
                    "Номер семестру має бути 1 або 2.");
        }
    }

    @Override
    public String toString() {
        return year + "/" + term;
    }
}