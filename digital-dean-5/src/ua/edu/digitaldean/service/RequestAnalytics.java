package ua.edu.digitaldean.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import ua.edu.digitaldean.model.AcademicRequest;
import ua.edu.digitaldean.model.RequestKey;
import ua.edu.digitaldean.model.StudentId;

public final class RequestAnalytics {
    private static final Comparator<RequestRow> REPORT_ORDER = Comparator
            .comparing(RequestRow::fee).reversed()
            .thenComparing(RequestRow::requestId);

    private static final Comparator<StudentId> STUDENT_ORDER =
            Comparator.comparing(StudentId::value);

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
                request.studentId(),
                request.type(),
                processor.process(request).fee())).toList();
    }

    public List<RequestRow> select(
            List<RequestRow> rows, BigDecimal minimumFee, int limit) {
        List<RequestRow> snapshot = checkedRows(rows);
        Objects.requireNonNull(minimumFee, "Поріг не може бути null.");
        if (minimumFee.signum() < 0 || limit < 0) {
            throw new IllegalArgumentException(
                    "Поріг і ліміт мають бути невід’ємними.");
        }
        return snapshot.stream()
                .filter(row -> row.fee().compareTo(minimumFee) >= 0)
                .sorted(REPORT_ORDER)
                .limit(limit)
                .toList();
    }

    public List<RequestRow> select(
            List<RequestRow> rows,
            Set<StudentId> allowedStudents,
            BigDecimal minimumFee,
            BigDecimal maximumFee,
            int limit) {
        List<RequestRow> snapshot = checkedRows(rows);
        Set<StudentId> checkedStudents = Set.copyOf(
                Objects.requireNonNull(allowedStudents, "Множина студентів не може бути null."));
        Objects.requireNonNull(minimumFee, "Мінімальний збір не може бути null.");
        Objects.requireNonNull(maximumFee, "Максимальний збір не може бути null.");
        if (minimumFee.signum() < 0
                || minimumFee.compareTo(maximumFee) > 0
                || limit < 0) {
            throw new IllegalArgumentException(
                    "Некоректні межі збору або ліміт.");
        }

        Predicate<RequestRow> studentPredicate = row ->
                checkedStudents.isEmpty() || checkedStudents.contains(row.studentId());
        Predicate<RequestRow> minFeePredicate = row ->
                row.fee().compareTo(minimumFee) >= 0;
        Predicate<RequestRow> maxFeePredicate = row ->
                row.fee().compareTo(maximumFee) <= 0;

        Predicate<RequestRow> combinedPredicate = studentPredicate
                .and(minFeePredicate)
                .and(maxFeePredicate);

        return snapshot.stream()
                .filter(combinedPredicate)
                .sorted(REPORT_ORDER)
                .limit(limit)
                .toList();
    }

    public Map<StudentId, BigDecimal> totalsByStudent(List<RequestRow> rows) {
        List<RequestRow> snapshot = checkedRows(rows);
        Map<StudentId, BigDecimal> grouped = snapshot.stream()
                .collect(Collectors.groupingBy(
                        RequestRow::studentId,
                        () -> new TreeMap<>(STUDENT_ORDER),
                        Collectors.reducing(
                                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                                RequestRow::fee,
                                (a, b) -> a.add(b).setScale(2, RoundingMode.HALF_UP)
                        )
                ));
        return Collections.unmodifiableMap(grouped);
    }

    public Map<String, Map<StudentId, Long>> countByTypeAndStudent(List<RequestRow> rows) {
        List<RequestRow> snapshot = checkedRows(rows);
        Map<String, Map<StudentId, Long>> rawGrouped = snapshot.stream()
                .collect(Collectors.groupingBy(
                        RequestRow::type,
                        TreeMap::new,
                        Collectors.groupingBy(
                                RequestRow::studentId,
                                () -> new TreeMap<>(STUDENT_ORDER),
                                Collectors.counting()
                        )
                ));
        Map<String, Map<StudentId, Long>> unmodifiableOuter = new LinkedHashMap<>();
        for (Map.Entry<String, Map<StudentId, Long>> entry : rawGrouped.entrySet()) {
            unmodifiableOuter.put(entry.getKey(), Collections.unmodifiableMap(entry.getValue()));
        }
        return Collections.unmodifiableMap(unmodifiableOuter);
    }

    public Map<Boolean, List<RequestRow>> partitionByFee(
            List<RequestRow> rows, BigDecimal threshold) {
        List<RequestRow> snapshot = checkedRows(rows);
        Objects.requireNonNull(threshold, "Поріг не може бути null.");
        if (threshold.signum() < 0) {
            throw new IllegalArgumentException("Поріг не може бути від’ємним.");
        }
        Map<Boolean, List<RequestRow>> partitioned = snapshot.stream()
                .collect(Collectors.partitioningBy(
                        row -> row.fee().compareTo(threshold) >= 0
                ));

        List<RequestRow> belowThreshold = partitioned.get(Boolean.FALSE).stream()
                .sorted(Comparator.comparing(RequestRow::requestId))
                .toList();
        List<RequestRow> atOrAboveThreshold = partitioned.get(Boolean.TRUE).stream()
                .sorted(Comparator.comparing(RequestRow::requestId))
                .toList();

        Map<Boolean, List<RequestRow>> orderedResult = new LinkedHashMap<>();
        orderedResult.put(Boolean.FALSE, belowThreshold);
        orderedResult.put(Boolean.TRUE, atOrAboveThreshold);
        return Collections.unmodifiableMap(orderedResult);
    }

    public Optional<RequestRow> firstForStudent(
            List<RequestRow> rows, StudentId student) {
        List<RequestRow> snapshot = checkedRows(rows);
        Objects.requireNonNull(student, "Ідентифікатор студента не може бути null.");
        return snapshot.stream()
                .filter(row -> row.studentId().equals(student))
                .min(Comparator.comparing(RequestRow::requestId));
    }

    public List<RequestRow> selectPrefixThenSort(
            List<RequestRow> rows, BigDecimal minimumFee, int limit) {
        List<RequestRow> snapshot = checkedRows(rows);
        Objects.requireNonNull(minimumFee, "Поріг не може бути null.");
        if (minimumFee.signum() < 0 || limit < 0) {
            throw new IllegalArgumentException(
                    "Поріг і ліміт мають бути невід’ємними.");
        }
        return snapshot.stream()
                .filter(row -> row.fee().compareTo(minimumFee) >= 0)
                .limit(limit)
                .sorted(REPORT_ORDER)
                .toList();
    }

    public BigDecimal totalFee(List<RequestRow> rows) {
        return checkedRows(rows).stream().map(RequestRow::fee)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
    }

    public Optional<BigDecimal> averageFee(List<RequestRow> rows) {
        List<RequestRow> snapshot = checkedRows(rows);
        long count = snapshot.stream().count();
        if (count == 0) {
            return Optional.empty();
        }
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