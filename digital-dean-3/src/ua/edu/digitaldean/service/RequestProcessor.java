package ua.edu.digitaldean.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import ua.edu.digitaldean.model.AcademicRequest;

public final class RequestProcessor {
    private final FeePolicy feePolicy;
    public RequestProcessor(FeePolicy feePolicy) {
        this.feePolicy = Objects.requireNonNull(
                feePolicy, "Політика збору не може бути null.");
    }
    public ProcessingResult process(AcademicRequest request) {
        Objects.requireNonNull(request, "Заява не може бути null.");
        return new ProcessingResult(
                request.summary(), feePolicy.calculate(request));
    }
    public List<ProcessingResult> processAll(
            List<? extends AcademicRequest> requests) {
        Objects.requireNonNull(requests, "Список заяв не може бути null.");
        List<ProcessingResult> results = new ArrayList<>();
        for (AcademicRequest request : requests) {
            results.add(process(request));
        }
        return List.copyOf(results);
    }
    public BigDecimal totalFee(List<ProcessingResult> results) {
        Objects.requireNonNull(results, "Список результатів не може бути null.");
        BigDecimal total = BigDecimal.ZERO;
        for (ProcessingResult result : results) {
            total = total.add(result.fee());
        }
        return total.setScale(2);
    }
}
