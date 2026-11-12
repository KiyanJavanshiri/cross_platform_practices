package ua.edu.digitaldean.model;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;

public final class TranscriptRequest extends AcademicRequest {
    private static final Set<String> LANGUAGES = Set.of("uk", "en");
    private static final BigDecimal FEE_PER_COPY = BigDecimal.valueOf(40L);
    private final String language;
    private final int copies;
    public TranscriptRequest(
            String requestId,
            StudentId studentId,
            String language,
            int copies) {
        super(requestId, studentId);
        if (language == null || language.isBlank()) {
            throw new IllegalArgumentException(
                    "Мова довідки не може бути порожньою.");

        }
        String normalizedLanguage = language.strip().toLowerCase(Locale.ROOT);
        if (!LANGUAGES.contains(normalizedLanguage)) {
            throw new IllegalArgumentException(
                    "Мова довідки має бути uk або en.");

        }
        if (copies < 1 || copies > 5) {
            throw new IllegalArgumentException(
                    "Кількість примірників має бути від 1 до 5.");

        }
        this.language = normalizedLanguage;
        this.copies = copies;
    }
    public String language() {
        return language;
    }
    public int copies() {
        return copies;
    }
    @Override
    public String type() {
        return "Академічна довідка";
    }
    @Override
    public String details() {
        return "мова: " + language + "; примірників: " + copies;
    }
    @Override
    public BigDecimal baseFee() {
        return FEE_PER_COPY.multiply(BigDecimal.valueOf(copies));
    }
}