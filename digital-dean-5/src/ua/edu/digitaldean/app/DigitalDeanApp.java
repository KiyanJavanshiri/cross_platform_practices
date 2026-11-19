package ua.edu.digitaldean.app;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import ua.edu.digitaldean.model.AcademicRequest;
import ua.edu.digitaldean.model.EnrollmentRequest;
import ua.edu.digitaldean.model.StudentId;
import ua.edu.digitaldean.model.TranscriptRequest;
import ua.edu.digitaldean.service.RequestAnalytics;
import ua.edu.digitaldean.service.RequestCatalog;
import ua.edu.digitaldean.service.RequestProcessor;
import ua.edu.digitaldean.service.RequestRow;
import ua.edu.digitaldean.service.StandardFeePolicy;

public final class DigitalDeanApp {
    private enum Mode {
        STANDARD,
        RANGE,
        COMPARE
    }

    private record Parameters(
            Mode mode,
            boolean empty,
            Set<StudentId> allowedStudents,
            BigDecimal minimumFee,
            BigDecimal maximumFee,
            int limit) {
    }

    private static final String FEE_PATTERN = "[0-9]+(?:\\.[0-9]{1,2})?";
    private static final String LIMIT_PATTERN = "0|[1-9][0-9]*";

    private DigitalDeanApp() {
    }

    public static void main(String[] args) {
        Parameters parameters;
        try {
            parameters = parse(args);
        } catch (IllegalArgumentException exception) {
            System.err.println("Помилка: некоректні аргументи.");
            System.exit(2);
            return;
        }

        switch (parameters.mode()) {
            case STANDARD -> printStandardReport(parameters);
            case RANGE -> printRangeReport(parameters);
            case COMPARE -> printCompareReport(parameters);
        }
    }

    private static Parameters parse(String[] args) {
        if (args == null) {
            throw new IllegalArgumentException("Масив аргументів не може бути null.");
        }
        for (String arg : args) {
            if (arg == null) {
                throw new IllegalArgumentException("Аргумент не може бути null.");
            }
        }

        if (args.length == 0) {
            return new Parameters(
                    Mode.STANDARD, false, Set.of(), new BigDecimal("40.00"), null, 3);
        }
        if (args.length == 1 && args[0].equals("--empty")) {
            return new Parameters(
                    Mode.STANDARD, true, Set.of(), new BigDecimal("40.00"), null, 3);
        }
        if (args.length == 2
                && args[0].matches(FEE_PATTERN)
                && args[1].matches(LIMIT_PATTERN)) {
            return new Parameters(
                    Mode.STANDARD,
                    false,
                    Set.of(),
                    new BigDecimal(args[0]),
                    null,
                    parseLimit(args[1]));
        }
        // [ЗМІНА 7] Режим --range STUDENT MIN MAX LIMIT
        if (args.length == 5 && args[0].equals("--range")) {
            Set<StudentId> students = args[1].equals("ALL")
                    ? Set.of()
                    : Set.of(new StudentId(args[1]));
            if (!args[2].matches(FEE_PATTERN)
                    || !args[3].matches(FEE_PATTERN)
                    || !args[4].matches(LIMIT_PATTERN)) {
                throw new IllegalArgumentException("Некоректні числові параметри --range.");
            }
            BigDecimal minFee = new BigDecimal(args[2]);
            BigDecimal maxFee = new BigDecimal(args[3]);
            if (minFee.compareTo(maxFee) > 0) {
                throw new IllegalArgumentException("MIN не може перевищувати MAX.");
            }
            int limit = parseLimit(args[4]);
            return new Parameters(Mode.RANGE, false, students, minFee, maxFee, limit);
        }
        // [ЗМІНА 7] Режим --compare MIN LIMIT
        if (args.length == 3 && args[0].equals("--compare")) {
            if (!args[1].matches(FEE_PATTERN) || !args[2].matches(LIMIT_PATTERN)) {
                throw new IllegalArgumentException("Некоректні числові параметри --compare.");
            }
            BigDecimal minFee = new BigDecimal(args[1]);
            int limit = parseLimit(args[2]);
            return new Parameters(Mode.COMPARE, false, Set.of(), minFee, null, limit);
        }

        throw new IllegalArgumentException("Невідомий формат аргументів.");
    }

    private static int parseLimit(String rawLimit) {
        try {
            return Integer.parseInt(rawLimit);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Переповнення ліміту.", ex);
        }
    }

    private static RequestCatalog<AcademicRequest> createDemoCatalog(boolean empty) {
        RequestCatalog<AcademicRequest> catalog = new RequestCatalog<>();
        if (!empty) {
            for (AcademicRequest request : demoRequests()) {
                catalog.add(request);
            }
        }
        return catalog;
    }

    private static void printStandardReport(Parameters parameters) {
        RequestCatalog<AcademicRequest> catalog = createDemoCatalog(parameters.empty());
        int pendingBefore = catalog.pendingCount();
        RequestAnalytics analytics = new RequestAnalytics(
                new RequestProcessor(new StandardFeePolicy()));
        List<RequestRow> rows = analytics.project(catalog.snapshot());
        List<RequestRow> selected = analytics.select(
                rows, parameters.minimumFee(), parameters.limit());

        System.out.println("Заяв у реєстрі: " + catalog.size());
        System.out.println("Мінімальний збір: "
                + parameters.minimumFee().setScale(2, RoundingMode.HALF_UP).toPlainString() + " UAH");
        System.out.println("Ліміт: " + parameters.limit());
        System.out.println("Вибрані заяви:");
        for (RequestRow row : selected) {
            System.out.println(row.formattedLine());
        }
        System.out.println("Відібрано заяв: " + selected.size());
        System.out.println("Сума вибірки: " + analytics.totalFee(selected).toPlainString() + " UAH");
        System.out.println("Середнє вибірки: " + analytics.averageFee(selected)
                .map(value -> value.toPlainString() + " UAH").orElse("НЕМАЄ ДАНИХ"));
        System.out.println("Найбільший збір у вибірці: " + analytics.highestFee(selected)
                .map(row -> row.requestId().value()).orElse("НЕМАЄ ДАНИХ"));
        String students = analytics.students(selected).stream()
                .map(StudentId::value).collect(Collectors.joining(", "));
        System.out.println("Студенти вибірки: " + (students.isEmpty() ? "НЕМАЄ" : students));
        String counts = analytics.countByType(selected).entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
        System.out.println("Кількість за типами у вибірці: " + (counts.isEmpty() ? "НЕМАЄ" : counts));
        System.out.println("Сума всіх заяв: " + analytics.totalFee(rows).toPlainString() + " UAH");
        System.out.println("Черга до звіту: " + pendingBefore);
        System.out.println("Черга після звіту: " + catalog.pendingCount());
    }

    private static void printRangeReport(Parameters parameters) {
        RequestCatalog<AcademicRequest> catalog = createDemoCatalog(false);
        RequestAnalytics analytics = new RequestAnalytics(
                new RequestProcessor(new StandardFeePolicy()));
        List<RequestRow> rows = analytics.project(catalog.snapshot());
        List<RequestRow> selected = analytics.select(
                rows,
                parameters.allowedStudents(),
                parameters.minimumFee(),
                parameters.maximumFee(),
                parameters.limit());

        for (RequestRow row : selected) {
            System.out.println(row.formattedLine());
        }
        System.out.println("Відібрано заяв: " + selected.size());
        System.out.println("Сума вибірки: " + analytics.totalFee(selected).toPlainString() + " UAH");
        System.out.println("Середнє вибірки: " + analytics.averageFee(selected)
                .map(value -> value.toPlainString() + " UAH").orElse("НЕМАЄ ДАНИХ"));
        System.out.println("Найбільший збір у вибірці: " + analytics.highestFee(selected)
                .map(row -> row.requestId().value()).orElse("НЕМАЄ ДАНИХ"));
    }

    private static void printCompareReport(Parameters parameters) {
        RequestCatalog<AcademicRequest> catalog = createDemoCatalog(false);
        RequestAnalytics analytics = new RequestAnalytics(
                new RequestProcessor(new StandardFeePolicy()));
        List<RequestRow> rows = analytics.project(catalog.snapshot());

        List<RequestRow> sortBeforeLimit = analytics.select(
                rows, parameters.minimumFee(), parameters.limit());
        List<RequestRow> limitBeforeSort = analytics.selectPrefixThenSort(
                rows, parameters.minimumFee(), parameters.limit());

        String keysFirst = sortBeforeLimit.stream()
                .map(row -> row.requestId().value())
                .collect(Collectors.joining(", "));
        String keysSecond = limitBeforeSort.stream()
                .map(row -> row.requestId().value())
                .collect(Collectors.joining(", "));

        System.out.println("Сортування перед лімітом: " + (keysFirst.isEmpty() ? "ПОРОЖНЯ" : keysFirst));
        System.out.println("Сума: " + analytics.totalFee(sortBeforeLimit).toPlainString() + " UAH");
        System.out.println("Ліміт перед сортуванням: " + (keysSecond.isEmpty() ? "ПОРОЖНЯ" : keysSecond));
        System.out.println("Сума: " + analytics.totalFee(limitBeforeSort).toPlainString() + " UAH");
    }

    private static List<AcademicRequest> demoRequests() {
        StudentId first = new StudentId("ST-2025-0042");
        StudentId second = new StudentId("ST-2025-0007");
        StudentId third = new StudentId("ST-2025-0099");
        return List.of(
                new TranscriptRequest("REQ-003", first, "uk", 2),
                new EnrollmentRequest("REQ-001", second, "CS205"),
                new TranscriptRequest("REQ-002", second, "en", 1),
                new EnrollmentRequest("REQ-004", first, "MA101"),
                new TranscriptRequest("REQ-005", third, "en", 2),
                new TranscriptRequest("REQ-006", third, "uk", 3));
    }
}