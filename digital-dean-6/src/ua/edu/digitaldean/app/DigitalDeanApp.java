package ua.edu.digitaldean.app;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
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
    private record Parameters(boolean empty, BigDecimal minimumFee, int limit) {
    }

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
        printReport(parameters);
    }

    private static Parameters parse(String[] args) {
        if (args == null) {
            throw new IllegalArgumentException("Аргументи відсутні.");
        }
        if (args.length == 0) {
            return new Parameters(false, new BigDecimal("40.00"), 3);
        }
        if (args.length == 1 && args[0].equals("--empty")) {
            return new Parameters(true, new BigDecimal("40.00"), 3);
        }
        if (args.length != 2
                || args[0] == null
                || args[1] == null
                || !args[0].matches("[0-9]+(?:\\.[0-9]{1,2})?")
                || !args[1].matches("0|[1-9][0-9]*")) {
            throw new IllegalArgumentException("Формат аргументів.");
        }
        int limit;
        try {
            limit = Integer.parseInt(args[1]);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Переповнення ліміту.", exception);
        }
        return new Parameters(false, new BigDecimal(args[0]), limit);
    }

    private static void printReport(Parameters parameters) {
        RequestCatalog<AcademicRequest> catalog = new RequestCatalog<>();
        if (!parameters.empty()) {
            for (AcademicRequest request : demoRequests()) {
                catalog.add(request);
            }
        }
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