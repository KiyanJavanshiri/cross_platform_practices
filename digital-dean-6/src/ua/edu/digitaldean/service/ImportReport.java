package ua.edu.digitaldean.service;

import java.util.List;
import java.util.Objects;
import ua.edu.digitaldean.model.TranscriptRequest;

public record ImportReport(
        List<TranscriptRequest> accepted,
        List<RequestDataException> rejected) {
    public ImportReport {
        Objects.requireNonNull(accepted, "Список прийнятих заяв не може бути null.");
        Objects.requireNonNull(rejected, "Список відхилених помилок не може бути null.");
        accepted = List.copyOf(accepted);
        rejected = List.copyOf(rejected);
    }
}