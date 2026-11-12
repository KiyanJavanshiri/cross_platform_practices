package ua.edu.digitaldean.app;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import ua.edu.digitaldean.model.AcademicRequest;
import ua.edu.digitaldean.model.EnrollmentRequest;
import ua.edu.digitaldean.model.RequestKey;
import ua.edu.digitaldean.model.StudentId;
import ua.edu.digitaldean.model.TranscriptRequest;
import ua.edu.digitaldean.service.ProcessingResult;
import ua.edu.digitaldean.service.RequestCatalog;
import ua.edu.digitaldean.service.RequestProcessor;
import ua.edu.digitaldean.service.StandardFeePolicy;

public final class DigitalDeanApp {
    private DigitalDeanApp() {
    }

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                runDemo(new RequestKey("REQ-002"));
                System.exit(0);
            }

            String mode = args[0];

            if (mode.startsWith("REQ-") || mode.matches("req-\\d{3}")) {
                if (args.length > 1) {
                    throw new IllegalArgumentException("Очікується не більш як один ідентифікатор заяви.");
                }
                runDemo(new RequestKey(mode));
                System.exit(0);
            }

            switch (mode) {
                case "--report" -> {
                    if (args.length != 4) {
                        throw new IllegalArgumentException("Некоректні аргументи для звіту.");
                    }
                    StudentId studentId = new StudentId(args[1]);
                    BigDecimal minFee = parseMinFee(args[2]);
                    int limit = parseLimit(args[3]);
                    runReport(studentId, minFee, limit);
                }
                case "--take" -> {
                    if (args.length != 2) {
                        throw new IllegalArgumentException("Некоректні аргументи для черги.");
                    }
                    int limit = parseLimit(args[1]);
                    runTake(limit);
                }
                case "--empty" -> {
                    if (args.length != 1) {
                        throw new IllegalArgumentException("Некоректні аргументи для порожнього реєстру.");
                    }
                    runEmpty();
                }
                default -> throw new IllegalArgumentException("Невідомий режим.");
            }
            System.exit(0);
        } catch (IllegalArgumentException exception) {
            System.err.println("Помилка: некоректні аргументи.");
            System.exit(2);
        }
    }

    private static BigDecimal parseMinFee(String value) {
        if (!value.matches("^\\d+(\\.\\d{1,2})?$")) {
            throw new IllegalArgumentException("Некоректний формат мін. збору.");
        }
        return new BigDecimal(value);
    }

    private static int parseLimit(String value) {
        if (!value.matches("^(0|[1-9]\\d*)$")) {
            throw new IllegalArgumentException("Некоректний формат ліміту.");
        }
        long parsed = Long.parseLong(value);
        if (parsed > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Переповнення ліміту int.");
        }
        return (int) parsed;
    }

    private static RequestCatalog<AcademicRequest> createDefaultCatalog() {
        StudentId first = new StudentId("ST-2025-0042");
        StudentId second = new StudentId("ST-2025-0007");
        List<AcademicRequest> requests = List.of(
                new TranscriptRequest("REQ-003", first, "uk", 2),
                new EnrollmentRequest("REQ-001", second, "CS205"),
                new TranscriptRequest("REQ-002", second, "en", 1),
                new EnrollmentRequest("REQ-004", first, "MA101"));
        RequestCatalog<AcademicRequest> catalog = new RequestCatalog<>();
        for (AcademicRequest request : requests) {
            catalog.add(request);
        }
        return catalog;
    }

    private static void runDemo(RequestKey query) {
        RequestCatalog<AcademicRequest> catalog = createDefaultCatalog();
        boolean duplicateAdded = catalog.add(
                new TranscriptRequest("REQ-002", new StudentId("ST-2025-0042"), "uk", 5));
        System.out.println("Дублікат додано: " + duplicateAdded);
        System.out.println("Порядок реєстру: " + ids(catalog.snapshot()));
        System.out.println("Студенти: " + catalog.studentIds().stream()
                .map(StudentId::value).collect(Collectors.joining(", ")));
        System.out.println("Ключі за Comparable: " + catalog.keysSorted().stream()
                .map(RequestKey::value).collect(Collectors.joining(", ")));
        System.out.println("Пошук " + query.value() + ": " + catalog.find(query)
                .map(AcademicRequest::type).orElse("НЕ ЗНАЙДЕНО"));

        Comparator<AcademicRequest> byFeeDescThenKeyAsc = Comparator
                .comparing(AcademicRequest::baseFee, Comparator.reverseOrder())
                .thenComparing(request -> new RequestKey(request.requestId()));

        List<AcademicRequest> paid = catalog.select(
                request -> request.baseFee().signum() > 0, byFeeDescThenKeyAsc);
        System.out.println("Платні заяви за збором: " + ids(paid));
        RequestProcessor processor = new RequestProcessor(new StandardFeePolicy());
        List<ProcessingResult> results = processor.processAll(paid);
        for (ProcessingResult result : results) {
            System.out.println(result.formattedLine());
        }
        System.out.println("Опрацьовано платних заяв: " + results.size());
        System.out.println("Загальна сума зборів: "
                + processor.totalFee(results).toPlainString() + " UAH");
        List<String> taken = new ArrayList<>();
        Optional<AcademicRequest> next = catalog.pollPending();
        while (next.isPresent()) {
            taken.add(next.orElseThrow().requestId());
            next = catalog.pollPending();
        }
        System.out.println("FIFO: " + String.join(", ", taken));
        System.out.println("Черга після опрацювання: " + catalog.pollPending()
                .map(AcademicRequest::requestId).orElse("ПОРОЖНЯ"));
        System.out.println("Заяв у реєстрі: " + catalog.size());
    }

    private static void runReport(StudentId studentId, BigDecimal minFee, int limit) {
        RequestCatalog<AcademicRequest> catalog = createDefaultCatalog();
        Comparator<AcademicRequest> comparator = Comparator
                .comparing(AcademicRequest::baseFee, Comparator.reverseOrder())
                .thenComparing(req -> new RequestKey(req.requestId()));

        List<AcademicRequest> selected = catalog.select(
                req -> req.studentId().equals(studentId) && req.baseFee().compareTo(minFee) >= 0,
                comparator,
                limit
        );

        RequestProcessor processor = new RequestProcessor(new StandardFeePolicy());
        List<ProcessingResult> results = processor.processAll(selected);
        for (ProcessingResult res : results) {
            System.out.println(res.formattedLine());
        }
        BigDecimal total = processor.totalFee(results);
        System.out.println("Відібрано заяв: " + results.size());
        System.out.println("Загальна сума зборів: " + total.toPlainString() + " UAH");
    }

    private static void runTake(int limit) {
        RequestCatalog<AcademicRequest> catalog = createDefaultCatalog();
        List<AcademicRequest> batch = catalog.processNext(limit);
        if (batch.isEmpty()) {
            System.out.println("Отримано з черги: ПОРОЖНЯ");
        } else {
            String batchIds = batch.stream().map(AcademicRequest::requestId).collect(Collectors.joining(", "));
            System.out.println("Отримано з черги: " + batchIds);
        }
        System.out.println("Залишилось у черзі: " + catalog.pendingCount());
        System.out.println("Заяв у реєстрі: " + catalog.size());
    }

    private static void runEmpty() {
        RequestCatalog<AcademicRequest> catalog = new RequestCatalog<>();
        System.out.println("Заяв у реєстрі: " + catalog.size());
        System.out.println("Студентів: " + catalog.studentIds().size());
        System.out.println("Груп студентів: " + catalog.groupByStudent().size());
        System.out.println("Типів заяв: " + catalog.countByType().size());
        System.out.println("Пошук REQ-002: " + catalog.find(new RequestKey("REQ-002")).map(AcademicRequest::type).orElse("НЕ ЗНАЙДЕНО"));
        System.out.println("Відібрано заяв: " + catalog.select(req -> true, Comparator.comparing(r -> new RequestKey(r.requestId()))).size());
        System.out.println("Черга: " + (catalog.pollPending().isEmpty() ? "ПОРОЖНЯ" : "НЕПОРОЖНЯ"));
    }

    private static String ids(List<? extends AcademicRequest> requests) {
        return requests.stream().map(AcademicRequest::requestId)
                .collect(Collectors.joining(", "));
    }
}