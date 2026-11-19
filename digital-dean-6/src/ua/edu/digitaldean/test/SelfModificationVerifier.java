package ua.edu.digitaldean.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import ua.edu.digitaldean.model.AcademicRequest;
import ua.edu.digitaldean.model.RequestKey;
import ua.edu.digitaldean.model.StudentId;
import ua.edu.digitaldean.model.TranscriptRequest;
import ua.edu.digitaldean.service.DiagnosticLog;
import ua.edu.digitaldean.service.DuplicateRequestException;
import ua.edu.digitaldean.service.ImportReport;
import ua.edu.digitaldean.service.RequestAnalytics;
import ua.edu.digitaldean.service.RequestCatalog;
import ua.edu.digitaldean.service.RequestDataException;
import ua.edu.digitaldean.service.RequestImporter;
import ua.edu.digitaldean.service.RequestProcessor;
import ua.edu.digitaldean.service.RequestRow;
import ua.edu.digitaldean.service.StandardFeePolicy;

public final class SelfModificationVerifier {
    private SelfModificationVerifier() {
    }

    public static void main(String[] args) throws Exception {
        verifyExceptionsAndCause();
        verifyReaderOwnershipAndSuppressed();
        verifyRegisterUniqueAtomicity();
        verifyReadAllAndTolerantState();
        verifyLoggerLevelsAndCustomHandler();
        verifyQueueAndAnalyticsRegression();
        System.out.println("Усі 6 автоматичних перевірок практичної роботи № 9 пройдено успішно.");
    }

    private static void verifyExceptionsAndCause() throws Exception {
        RequestImporter importer = new RequestImporter();
        RequestDataException ex = assertThrows(
                RequestDataException.class,
                () -> importer.read(new BufferedReader(new StringReader(
                        "REQ-003;ST-2025-0042;uk;2\nREQ-002;ST-2025-0007;en;two\n"))),
                "read з некоректним числом на 2 рядку");
        assertEquals(2, ex.lineNumber(), "Номер рядка помилки");
        assertTrue(ex.getCause() instanceof NumberFormatException, "Причина має бути NumberFormatException");
        assertEquals(0, ex.getSuppressed().length, "Кількість пригнічених винятків");
    }

    private static void verifyReaderOwnershipAndSuppressed() throws Exception {
        final int[] closeCalls = {0};
        final IOException readIoEx = new IOException("Збій читання");
        final IOException closeIoEx = new IOException("Збій закриття");

        BufferedReader customReader = new BufferedReader(new StringReader("REQ-001;ST-2025-0007;uk;1\n")) {
            @Override
            public String readLine() throws IOException {
                throw readIoEx;
            }

            @Override
            public void close() throws IOException {
                closeCalls[0]++;
                super.close();
                throw closeIoEx;
            }
        };

        RequestImporter importer = new RequestImporter();
        RequestCatalog<AcademicRequest> catalog = new RequestCatalog<>();
        IOException thrownMain = null;
        try (BufferedReader owned = customReader) {
            importer.read(owned);
        } catch (IOException caught) {
            thrownMain = caught;
        }

        assertEquals(1, closeCalls[0], "Власник закриває ресурс один раз");
        assertTrue(thrownMain == readIoEx, "Основний виняток — той самий об'єкт IOException читання");
        assertEquals(1, thrownMain.getSuppressed().length, "Один пригнічений виняток");
        assertTrue(thrownMain.getSuppressed()[0] == closeIoEx, "Пригнічений виняток — той самий об'єкт закриття");
        assertEquals(0, catalog.size(), "Реєстр порожній після помилки читання");
        assertEquals(0, catalog.pendingCount(), "Черга порожня після помилки читання");
    }

    private static void verifyRegisterUniqueAtomicity() throws Exception {
        RequestImporter importer = new RequestImporter();
        RequestCatalog<AcademicRequest> catalog = new RequestCatalog<>();
        TranscriptRequest r1 = new TranscriptRequest("REQ-002", new StudentId("ST-2025-0007"), "en", 1);
        TranscriptRequest r2 = new TranscriptRequest("REQ-002", new StudentId("ST-2025-0042"), "uk", 2);
        TranscriptRequest r3 = new TranscriptRequest("REQ-001", new StudentId("ST-2025-0007"), "uk", 1);
        TranscriptRequest r4 = new TranscriptRequest("REQ-001", new StudentId("ST-2025-0099"), "en", 1);

        DuplicateRequestException dupEx = assertThrows(
                DuplicateRequestException.class,
                () -> importer.registerUnique(List.of(r1, r2, r3, r4), catalog),
                "Повторення REQ-002 та REQ-001");
        assertEquals(new RequestKey("REQ-002"), dupEx.key(), "Перший дублікат у списку — REQ-002");
        assertEquals("Повторний ідентифікатор заяви.", dupEx.getMessage(), "Повідомлення винятку");
        assertEquals(0, catalog.size(), "Реєстр не змінено при помилці дублювання");
        assertEquals(0, catalog.pendingCount(), "Черга не змінена при помилці дублювання");

        int added = importer.registerUnique(List.of(r1, r3), catalog);
        assertEquals(2, added, "Додано рівно 2 унікальні заяви");
        assertEquals(2, catalog.size(), "Розмір реєстру після успішної реєстрації");
        assertEquals(2, catalog.pendingCount(), "Розмір черги після успішної реєстрації");
    }

    private static void verifyReadAllAndTolerantState() throws Exception {
        RequestImporter importer = new RequestImporter();
        String input = "\nREQ-003;ST-2025-0042;uk;2\nREQ-002;ST-2025-0007;en;two\n";
        ImportReport report = importer.readAll(new BufferedReader(new StringReader(input)));

        assertEquals(1, report.accepted().size(), "Кількість прийнятих заяв");
        assertEquals("REQ-003", report.accepted().get(0).requestId(), "Прийнята заява");
        assertEquals(2, report.rejected().size(), "Кількість відхилених рядків");
        assertEquals(1, report.rejected().get(0).lineNumber(), "Перший відхилений рядок — порожній (1)");
        assertTrue(report.rejected().get(0).getCause() instanceof IllegalArgumentException, "Причина рядка 1");
        assertEquals(3, report.rejected().get(1).lineNumber(), "Другий відхилений рядок — нечислове copies (3)");
        assertTrue(report.rejected().get(1).getCause() instanceof NumberFormatException, "Причина рядка 3");

        assertThrows(
                UnsupportedOperationException.class,
                () -> report.accepted().add(new TranscriptRequest(
                        "REQ-009", new StudentId("ST-2025-0099"), "uk", 1)),
                "Список accepted незмінний");
    }

    private static void verifyLoggerLevelsAndCustomHandler() {
        Logger infoLogger = DiagnosticLog.create(Level.INFO);
        assertEquals(1, infoLogger.getHandlers().length, "Один власний обробник на журналі");

        List<LogRecord> captured = new ArrayList<>();
        Handler spyHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (isLoggable(record)) {
                    captured.add(record);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        spyHandler.setLevel(Level.FINE);

        Logger fineLogger = DiagnosticLog.create(Level.FINE);
        for (Handler h : fineLogger.getHandlers()) {
            fineLogger.removeHandler(h);
        }
        fineLogger.addHandler(spyHandler);

        IllegalArgumentException cause = new NumberFormatException("bad number");
        RequestDataException payload = new RequestDataException(2, cause);
        fineLogger.fine("IMPORT_START");
        fineLogger.log(Level.WARNING, "IMPORT_LINE_REJECTED line=2", payload);

        assertEquals(2, captured.size(), "Отримано два записи журналу");
        assertEquals(Level.FINE, captured.get(0).getLevel(), "Рівень першого запису");
        assertEquals("IMPORT_START", captured.get(0).getMessage(), "Повідомлення першого запису");
        assertEquals(Level.WARNING, captured.get(1).getLevel(), "Рівень другого запису");
        assertTrue(captured.get(1).getThrown() == payload, "Збережено той самий об'єкт винятку в LogRecord");
    }

    private static void verifyQueueAndAnalyticsRegression() {
        RequestCatalog<AcademicRequest> catalog = new RequestCatalog<>();
        catalog.add(new TranscriptRequest("REQ-003", new StudentId("ST-2025-0042"), "uk", 2));
        catalog.add(new TranscriptRequest("REQ-002", new StudentId("ST-2025-0007"), "en", 1));
        catalog.add(new TranscriptRequest("REQ-006", new StudentId("ST-2025-0099"), "uk", 3));

        RequestAnalytics analytics = new RequestAnalytics(new RequestProcessor(new StandardFeePolicy()));
        List<RequestRow> rows = analytics.project(catalog.snapshot());
        assertMoneyEquals(analytics.totalFee(rows), "240.00", "Сума демонстраційного імпорту");

        List<String> drainedKeys = new ArrayList<>();
        Optional<AcademicRequest> next;
        while ((next = catalog.pollPending()).isPresent()) {
            drainedKeys.add(next.get().requestId());
        }
        assertEquals(List.of("REQ-003", "REQ-002", "REQ-006"), drainedKeys, "Порядок вичерпування черги");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(label + ": очікувалось [" + expected + "], отримано [" + actual + "]");
        }
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label + ": умова хибна");
        }
    }

    private static void assertMoneyEquals(BigDecimal actual, String expectedPlain, String label) {
        if (actual == null || actual.scale() != 2 || actual.compareTo(new BigDecimal(expectedPlain)) != 0) {
            throw new AssertionError(label + ": очікувалось " + expectedPlain + ", отримано " + actual);
        }
    }

    private static <T extends Throwable> T assertThrows(
            Class<T> expectedType, ThrowingRunnable action, String label) throws Exception {
        try {
            action.run();
        } catch (Throwable t) {
            if (expectedType.isInstance(t)) {
                return expectedType.cast(t);
            }
            throw new AssertionError(label + ": очікувався " + expectedType.getSimpleName()
                    + ", отримано " + t.getClass().getSimpleName(), t);
        }
        throw new AssertionError(label + ": очікувався виняток " + expectedType.getSimpleName());
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}