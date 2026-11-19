package ua.edu.digitaldean.service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import ua.edu.digitaldean.model.AcademicRequest;
import ua.edu.digitaldean.model.StudentId;
import ua.edu.digitaldean.model.RequestKey;
public final class RequestAnalytics {
    private static final Comparator<RequestRow> REPORT_ORDER = Comparator
            .comparing(RequestRow::fee).reversed()
            .thenComparing(RequestRow::requestId);
    private final RequestProcessor processor;
    public RequestAnalytics(RequestProcessor processor) {
        this.processor = Objects.requireNonNull(
                processor, "Процесор не може бути null.");

    }
    public List<RequestRow> project(
            Collection<? extends AcademicRequest> source) {
        List<? extends AcademicRequest> snapshot = List.copyOf(
                Objects.requireNonNull(source, "Джерело не може бути null."));
        return snapshot.stream().map(request -> new RequestRow(
                new RequestKey(request.requestId()),
                request.studentId(), request.type(),
                processor.process(request).fee())).toList();

    }
    public List<RequestRow> select(
            List<RequestRow> rows, BigDecimal minimumFee, int limit) {
        List<RequestRow> snapshot = checkedRows(rows);
        Objects.requireNonNull(minimumFee, "Поріг не може бути null.");
        if (minimumFee.signum() < 0 || limit < 0) {
            throw new IllegalArgumentException("Поріг і ліміт мають бути невід’ємними.");
        }
        return snapshot.stream()
                .filter(row -> row.fee().compareTo(minimumFee) >= 0)
                .sorted(REPORT_ORDER).limit(limit).toList();

    }
    public BigDecimal totalFee(List<RequestRow> rows) {
        return checkedRows(rows).stream().map(RequestRow::fee)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);

    }
    public Optional<BigDecimal> averageFee(List<RequestRow> rows) {
        List<RequestRow> snapshot = checkedRows(rows);
        long count = snapshot.stream().count();
        if (count == 0) return Optional.empty();
        return Optional.of(totalFee(snapshot).divide(
                BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP));

    }
    public Optional<RequestRow> highestFee(List<RequestRow> rows) {
        return checkedRows(rows).stream().min(REPORT_ORDER);
    }
    public Map<String, Long> countByType(List<RequestRow> rows) {
        Map<String, Long> counts = checkedRows(rows).stream().collect(
                Collectors.groupingBy(
                        RequestRow::type, TreeMap::new, Collectors.counting()));

        return Collections.unmodifiableMap(counts);
    }
    public Set<StudentId> students(List<RequestRow> rows) {
        LinkedHashSet<StudentId> ids = checkedRows(rows).stream()
                .map(RequestRow::studentId)
                .sorted(Comparator.comparing(StudentId::value))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(ids);
    }
    private static List<RequestRow> checkedRows(List<RequestRow> rows) {
        return List.copyOf(Objects.requireNonNull(
                rows, "Список рядків не може бути null."));

    }
}