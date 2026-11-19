package ua.edu.digitaldean.test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import ua.edu.digitaldean.model.AcademicRequest;
import ua.edu.digitaldean.model.EnrollmentRequest;
import ua.edu.digitaldean.model.RequestKey;
import ua.edu.digitaldean.model.StudentId;
import ua.edu.digitaldean.model.TranscriptRequest;
import ua.edu.digitaldean.service.RequestAnalytics;
import ua.edu.digitaldean.service.RequestCatalog;
import ua.edu.digitaldean.service.RequestProcessor;
import ua.edu.digitaldean.service.RequestRow;
import ua.edu.digitaldean.service.StandardFeePolicy;

public final class SelfModificationVerifier {
    private static final StudentId ST_A = new StudentId("ST-2025-0042");
    private static final StudentId ST_B = new StudentId("ST-2025-0007");
    private static final StudentId ST_C = new StudentId("ST-2025-0099");

    private SelfModificationVerifier() {
    }

    public static void main(String[] args) {
        verifyCheck1CompoundSelectAndEmpty();
        verifyCheck2TotalsAndNestedGrouping();
        verifyCheck3PartitionAndFirstForStudent();
        verifyCheck4PipelineOrderExperiment();
        verifyCheck5ExceptionContractsBeforePipeline();
        verifyCheck6ImmutabilitySnapshotsAndQueueIntegrity();
        System.out.println("Усі 6 блоків перевірок самостійних змін (сценарії 10–23) виконано успішно без відхилень.");
    }

    private static RequestCatalog<AcademicRequest> newDemoCatalog() {
        RequestCatalog<AcademicRequest> catalog = new RequestCatalog<>();
        catalog.add(new TranscriptRequest("REQ-003", ST_A, "uk", 2));
        catalog.add(new EnrollmentRequest("REQ-001", ST_B, "CS205"));
        catalog.add(new TranscriptRequest("REQ-002", ST_B, "en", 1));
        catalog.add(new EnrollmentRequest("REQ-004", ST_A, "MA101"));
        catalog.add(new TranscriptRequest("REQ-005", ST_C, "en", 2));
        catalog.add(new TranscriptRequest("REQ-006", ST_C, "uk", 3));
        return catalog;
    }

    private static RequestAnalytics newAnalytics() {
        return new RequestAnalytics(new RequestProcessor(new StandardFeePolicy()));
    }

    private static void verifyCheck1CompoundSelectAndEmpty() {
        RequestAnalytics analytics = newAnalytics();
        List<RequestRow> rows = analytics.project(newDemoCatalog().snapshot());

        List<RequestRow> res10a = analytics.select(
                rows, Set.of(ST_A, ST_C), new BigDecimal("0.00"), new BigDecimal("80.00"), 10);
        assertKeys(res10a, List.of("REQ-003", "REQ-005", "REQ-004"), "Сценарій 10a");
        assertMoneyEquals(analytics.totalFee(res10a), "160.00", "Сценарій 10a сума");

        List<RequestRow> res10b = analytics.select(
                rows, Set.of(), new BigDecimal("0"), new BigDecimal("200"), 10);
        assertKeys(res10b, List.of("REQ-006", "REQ-003", "REQ-005", "REQ-002", "REQ-001", "REQ-004"), "Сценарій 10b");

        List<RequestRow> emptyRows = List.of();
        assertTrue(analytics.select(emptyRows, Set.of(ST_A), BigDecimal.ZERO, new BigDecimal("100"), 5).isEmpty(), "Сценарій 11 select");
        assertTrue(analytics.totalsByStudent(emptyRows).isEmpty(), "Сценарій 11 totalsByStudent");
        assertTrue(analytics.countByTypeAndStudent(emptyRows).isEmpty(), "Сценарій 11 countByTypeAndStudent");

        Map<Boolean, List<RequestRow>> emptyPart = analytics.partitionByFee(emptyRows, new BigDecimal("40.00"));
        assertEquals(List.of(Boolean.FALSE, Boolean.TRUE), new ArrayList<>(emptyPart.keySet()), "Сценарій 11 partition keys");
        assertTrue(emptyPart.get(Boolean.FALSE).isEmpty(), "Сценарій 11 partition false");
        assertTrue(emptyPart.get(Boolean.TRUE).isEmpty(), "Сценарій 11 partition true");

        assertTrue(analytics.firstForStudent(emptyRows, ST_A).isEmpty(), "Сценарій 11 firstForStudent");
        assertTrue(analytics.selectPrefixThenSort(emptyRows, BigDecimal.ZERO, 5).isEmpty(), "Сценарій 11 selectPrefixThenSort");
        System.out.println("[OK] Перевірка 1: складений відбір (Predicate.and) та обробка порожнього списку.");
    }

    private static void verifyCheck2TotalsAndNestedGrouping() {
        RequestAnalytics analytics = newAnalytics();
        List<RequestRow> rows = analytics.project(newDemoCatalog().snapshot());

        Map<StudentId, BigDecimal> totals = analytics.totalsByStudent(rows);
        assertEquals(List.of(ST_B, ST_A, ST_C), new ArrayList<>(totals.keySet()), "Сценарій 12 порядок ключів");
        assertMoneyEquals(totals.get(ST_B), "40.00", "Сценарій 12 ST_B");
        assertMoneyEquals(totals.get(ST_A), "80.00", "Сценарій 12 ST_A");
        assertMoneyEquals(totals.get(ST_C), "200.00", "Сценарій 12 ST_C");

        List<RequestRow> zeroRows = rows.stream()
                .filter(r -> r.requestId().value().equals("REQ-001") || r.requestId().value().equals("REQ-004"))
                .toList();
        Map<StudentId, BigDecimal> zeroTotals = analytics.totalsByStudent(zeroRows);
        assertEquals(List.of(ST_B, ST_A), new ArrayList<>(zeroTotals.keySet()), "Сценарій 12 нульові ключі");
        assertMoneyEquals(zeroTotals.get(ST_B), "0.00", "Сценарій 12 нуль ST_B");
        assertMoneyEquals(zeroTotals.get(ST_A), "0.00", "Сценарій 12 нуль ST_A");

        Map<String, Map<StudentId, Long>> nested = analytics.countByTypeAndStudent(rows);
        assertEquals(List.of("Академічна довідка", "Зарахування на дисципліну"),
                new ArrayList<>(nested.keySet()), "Сценарій 13 зовнішні ключі");

        Map<StudentId, Long> transcripts = nested.get("Академічна довідка");
        assertEquals(List.of(ST_B, ST_A, ST_C), new ArrayList<>(transcripts.keySet()), "Сценарій 13 довідки студенти");
        assertEquals(1L, transcripts.get(ST_B), "Сценарій 13 довідки B");
        assertEquals(1L, transcripts.get(ST_A), "Сценарій 13 довідки A");
        assertEquals(2L, transcripts.get(ST_C), "Сценарій 13 довідки C");

        Map<StudentId, Long> enrollments = nested.get("Зарахування на дисципліну");
        assertEquals(List.of(ST_B, ST_A), new ArrayList<>(enrollments.keySet()), "Сценарій 13 зарахування студенти");
        assertEquals(1L, enrollments.get(ST_B), "Сценарій 13 зарахування B");
        assertEquals(1L, enrollments.get(ST_A), "Сценарій 13 зарахування A");
        System.out.println("[OK] Перевірка 2: суми за студентами (включно з 0.00) та дворівневе групування.");
    }

    private static void verifyCheck3PartitionAndFirstForStudent() {
        RequestAnalytics analytics = newAnalytics();
        List<RequestRow> rows = analytics.project(newDemoCatalog().snapshot());

        Map<Boolean, List<RequestRow>> part80 = analytics.partitionByFee(rows, new BigDecimal("80.00"));
        assertEquals(List.of(Boolean.FALSE, Boolean.TRUE), new ArrayList<>(part80.keySet()), "Сценарій 14 ключі");
        assertKeys(part80.get(Boolean.FALSE), List.of("REQ-001", "REQ-002", "REQ-004"), "Сценарій 14 false");
        assertKeys(part80.get(Boolean.TRUE), List.of("REQ-003", "REQ-005", "REQ-006"), "Сценарій 14 true");

        Map<Boolean, List<RequestRow>> part0 = analytics.partitionByFee(rows, BigDecimal.ZERO);
        assertEquals(List.of(Boolean.FALSE, Boolean.TRUE), new ArrayList<>(part0.keySet()), "Сценарій 15 ключі");
        assertTrue(part0.get(Boolean.FALSE).isEmpty(), "Сценарій 15 false порожній");
        assertKeys(part0.get(Boolean.TRUE),
                List.of("REQ-001", "REQ-002", "REQ-003", "REQ-004", "REQ-005", "REQ-006"), "Сценарій 15 true");

        assertEquals("REQ-003", analytics.firstForStudent(rows, ST_A).orElseThrow().requestId().value(), "Сценарій 16 A");
        assertEquals("REQ-001", analytics.firstForStudent(rows, ST_B).orElseThrow().requestId().value(), "Сценарій 16 B");
        assertEquals("REQ-005", analytics.firstForStudent(rows, ST_C).orElseThrow().requestId().value(), "Сценарій 16 C");
        assertTrue(analytics.firstForStudent(rows, new StudentId("ST-2025-9999")).isEmpty(), "Сценарій 16 відсутній");
        System.out.println("[OK] Перевірка 3: розбиття за порогом та пошук першої заяви студента за ключем.");
    }

    private static void verifyCheck4PipelineOrderExperiment() {
        RequestAnalytics analytics = newAnalytics();
        List<RequestRow> rows = analytics.project(newDemoCatalog().snapshot());
        BigDecimal threshold = new BigDecimal("40.00");

        List<RequestRow> s3 = analytics.select(rows, threshold, 3);
        List<RequestRow> p3 = analytics.selectPrefixThenSort(rows, threshold, 3);
        assertKeys(s3, List.of("REQ-006", "REQ-003", "REQ-005"), "Сценарій 17 sorted->limit");
        assertMoneyEquals(analytics.totalFee(s3), "280.00", "Сценарій 17 сума 1");
        assertMoneyEquals(analytics.averageFee(s3).orElseThrow(), "93.33", "Сценарій 17 середнє 1");

        assertKeys(p3, List.of("REQ-003", "REQ-005", "REQ-002"), "Сценарій 17 limit->sorted");
        assertMoneyEquals(analytics.totalFee(p3), "200.00", "Сценарій 17 сума 2");
        assertMoneyEquals(analytics.averageFee(p3).orElseThrow(), "66.67", "Сценарій 17 середнє 2");

        List<RequestRow> s2 = analytics.select(rows, threshold, 2);
        List<RequestRow> p2 = analytics.selectPrefixThenSort(rows, threshold, 2);
        assertKeys(s2, List.of("REQ-006", "REQ-003"), "Сценарій 18 sorted->limit");
        assertMoneyEquals(analytics.totalFee(s2), "200.00", "Сценарій 18 сума 1");
        assertMoneyEquals(analytics.averageFee(s2).orElseThrow(), "100.00", "Сценарій 18 середнє 1");

        assertKeys(p2, List.of("REQ-003", "REQ-002"), "Сценарій 18 limit->sorted");
        assertMoneyEquals(analytics.totalFee(p2), "120.00", "Сценарій 18 сума 2");
        assertMoneyEquals(analytics.averageFee(p2).orElseThrow(), "60.00", "Сценарій 18 середнє 2");

        List<RequestRow> s0 = analytics.select(rows, threshold, 0);
        List<RequestRow> p0 = analytics.selectPrefixThenSort(rows, threshold, 0);
        assertTrue(s0.isEmpty() && p0.isEmpty(), "Сценарій 19 порожні списки");
        assertMoneyEquals(analytics.totalFee(s0), "0.00", "Сценарій 19 сума 1");
        assertMoneyEquals(analytics.totalFee(p0), "0.00", "Сценарій 19 сума 2");
        assertTrue(analytics.averageFee(s0).isEmpty() && analytics.averageFee(p0).isEmpty(), "Сценарій 19 відсутні середні");
        System.out.println("[OK] Перевірка 4: експеримент порівняння порядку sorted і limit (ліміти 3, 2, 0).");
    }

    private static void verifyCheck5ExceptionContractsBeforePipeline() {
        RequestAnalytics analytics = newAnalytics();
        List<RequestRow> emptyList = List.of();
        List<RequestRow> listWithNull = Collections.singletonList(null);
        Set<StudentId> setWithNull = Collections.singleton(null);

        assertThrows(NullPointerException.class,
                () -> analytics.select(null, Set.of(), BigDecimal.ZERO, BigDecimal.TEN, 1), "select null rows");
        assertThrows(NullPointerException.class,
                () -> analytics.select(listWithNull, Set.of(), BigDecimal.ZERO, BigDecimal.TEN, 0), "select null row element");
        assertThrows(NullPointerException.class,
                () -> analytics.select(emptyList, null, BigDecimal.ZERO, BigDecimal.TEN, 0), "select null students");
        assertThrows(NullPointerException.class,
                () -> analytics.select(emptyList, setWithNull, BigDecimal.ZERO, BigDecimal.TEN, 0), "select null student element");
        assertThrows(NullPointerException.class,
                () -> analytics.select(emptyList, Set.of(), null, BigDecimal.TEN, 0), "select null min");
        assertThrows(NullPointerException.class,
                () -> analytics.select(emptyList, Set.of(), BigDecimal.ZERO, null, 0), "select null max");

        assertThrows(IllegalArgumentException.class,
                () -> analytics.select(emptyList, Set.of(), new BigDecimal("-0.01"), BigDecimal.TEN, 0), "select negative min");
        assertThrows(IllegalArgumentException.class,
                () -> analytics.select(emptyList, Set.of(), new BigDecimal("50.00"), new BigDecimal("40.00"), 0), "select min > max");
        assertThrows(IllegalArgumentException.class,
                () -> analytics.select(emptyList, Set.of(), BigDecimal.ZERO, BigDecimal.TEN, -1), "select negative limit");

        assertThrows(NullPointerException.class, () -> analytics.totalsByStudent(null), "totalsByStudent null");
        assertThrows(NullPointerException.class, () -> analytics.totalsByStudent(listWithNull), "totalsByStudent null elem");
        assertThrows(NullPointerException.class, () -> analytics.countByTypeAndStudent(null), "countByTypeAndStudent null");
        assertThrows(NullPointerException.class, () -> analytics.countByTypeAndStudent(listWithNull), "countByTypeAndStudent null elem");

        assertThrows(NullPointerException.class, () -> analytics.partitionByFee(null, BigDecimal.ZERO), "partitionByFee null rows");
        assertThrows(NullPointerException.class, () -> analytics.partitionByFee(listWithNull, BigDecimal.ZERO), "partitionByFee null elem");
        assertThrows(NullPointerException.class, () -> analytics.partitionByFee(emptyList, null), "partitionByFee null threshold");
        assertThrows(IllegalArgumentException.class, () -> analytics.partitionByFee(emptyList, new BigDecimal("-0.01")), "partitionByFee negative");

        assertThrows(NullPointerException.class, () -> analytics.firstForStudent(null, ST_A), "firstForStudent null rows");
        assertThrows(NullPointerException.class, () -> analytics.firstForStudent(listWithNull, ST_A), "firstForStudent null elem");
        assertThrows(NullPointerException.class, () -> analytics.firstForStudent(emptyList, null), "firstForStudent null student");

        assertThrows(NullPointerException.class, () -> analytics.selectPrefixThenSort(null, BigDecimal.ZERO, 0), "selectPrefixThenSort null rows");
        assertThrows(NullPointerException.class, () -> analytics.selectPrefixThenSort(listWithNull, BigDecimal.ZERO, 0), "selectPrefixThenSort null elem");
        assertThrows(NullPointerException.class, () -> analytics.selectPrefixThenSort(emptyList, null, 0), "selectPrefixThenSort null min");
        assertThrows(IllegalArgumentException.class, () -> analytics.selectPrefixThenSort(emptyList, new BigDecimal("-0.01"), 0), "selectPrefixThenSort negative min");
        assertThrows(IllegalArgumentException.class, () -> analytics.selectPrefixThenSort(emptyList, BigDecimal.ZERO, -1), "selectPrefixThenSort negative limit");

        System.out.println("[OK] Перевірка 5: контракти винятків (NullPointerException, IllegalArgumentException) до обходу.");
    }

    private static void verifyCheck6ImmutabilitySnapshotsAndQueueIntegrity() {
        RequestAnalytics analytics = newAnalytics();
        RequestCatalog<AcademicRequest> catalog = newDemoCatalog();
        List<RequestRow> rows = analytics.project(catalog.snapshot());

        RequestRow dummyRow = new RequestRow(
                new RequestKey("REQ-999"), ST_A, "Академічна довідка", new BigDecimal("40.00"));
        StudentId dummyStudent = new StudentId("ST-2025-8888");

        List<RequestRow> compSel = analytics.select(rows, Set.of(), BigDecimal.ZERO, new BigDecimal("200"), 10);
        assertThrows(UnsupportedOperationException.class, () -> compSel.add(dummyRow), "add to select");

        List<RequestRow> prefSel = analytics.selectPrefixThenSort(rows, BigDecimal.ZERO, 10);
        assertThrows(UnsupportedOperationException.class, () -> prefSel.add(dummyRow), "add to selectPrefixThenSort");

        Map<StudentId, BigDecimal> totals = analytics.totalsByStudent(rows);
        assertThrows(UnsupportedOperationException.class, () -> totals.put(dummyStudent, BigDecimal.ZERO), "put to totalsByStudent");

        Map<String, Map<StudentId, Long>> nested = analytics.countByTypeAndStudent(rows);
        assertThrows(UnsupportedOperationException.class, () -> nested.put("Новий тип", Map.of()), "put to outer nested");
        Map<StudentId, Long> inner = nested.get("Академічна довідка");
        assertThrows(UnsupportedOperationException.class, () -> inner.put(dummyStudent, 1L), "put to inner nested");

        Map<Boolean, List<RequestRow>> part = analytics.partitionByFee(rows, new BigDecimal("80.00"));
        assertThrows(UnsupportedOperationException.class, () -> part.put(Boolean.TRUE, List.of()), "put to partition map");
        assertThrows(UnsupportedOperationException.class, () -> part.get(Boolean.FALSE).add(dummyRow), "add to partition false list");
        assertThrows(UnsupportedOperationException.class, () -> part.get(Boolean.TRUE).add(dummyRow), "add to partition true list");

        List<RequestRow> mutableRows = new ArrayList<>(rows);
        Map<StudentId, BigDecimal> totalsSnapshot = analytics.totalsByStudent(mutableRows);
        Map<String, Map<StudentId, Long>> nestedSnapshot = analytics.countByTypeAndStudent(mutableRows);
        mutableRows.clear();
        assertEquals(3, totalsSnapshot.size(), "Сценарій 22 totals збережено після clear");
        assertEquals(2, nestedSnapshot.size(), "Сценарій 22 nested збережено після clear");

        catalog.add(new TranscriptRequest("REQ-007", ST_A, "uk", 1));
        assertEquals(3, totalsSnapshot.size(), "Сценарій 22 старі totals не змінились після add(REQ-007)");
        Map<StudentId, BigDecimal> updatedTotals = analytics.totalsByStudent(analytics.project(catalog.snapshot()));
        assertMoneyEquals(updatedTotals.get(ST_A), "120.00", "Сценарій 22 нові totals враховують REQ-007");

        RequestCatalog<AcademicRequest> freshCatalog = newDemoCatalog();
        List<RequestRow> freshRows = analytics.project(freshCatalog.snapshot());
        analytics.select(freshRows, Set.of(ST_A), BigDecimal.ZERO, new BigDecimal("200"), 5);
        analytics.totalsByStudent(freshRows);
        analytics.countByTypeAndStudent(freshRows);
        analytics.partitionByFee(freshRows, new BigDecimal("40.00"));
        analytics.firstForStudent(freshRows, ST_C);
        analytics.selectPrefixThenSort(freshRows, new BigDecimal("40.00"), 2);

        assertEquals(6, freshCatalog.size(), "Сценарій 23 size");
        assertEquals(6, freshCatalog.pendingCount(), "Сценарій 23 pendingCount");
        assertEquals(List.of("REQ-003", "REQ-001", "REQ-002", "REQ-004", "REQ-005", "REQ-006"),
                freshCatalog.snapshot().stream().map(AcademicRequest::requestId).toList(), "Сценарій 23 порядок snapshot");

        List<String> polledOrder = new ArrayList<>();
        Optional<AcademicRequest> next;
        while ((next = freshCatalog.pollPending()).isPresent()) {
            polledOrder.add(next.get().requestId());
        }
        assertEquals(List.of("REQ-003", "REQ-001", "REQ-002", "REQ-004", "REQ-005", "REQ-006"),
                polledOrder, "Сценарій 23 порядок вичерпування черги");
        assertEquals(0, freshCatalog.pendingCount(), "Сценарій 23 черга вичерпана");
        System.out.println("[OK] Перевірка 6: заборона модифікації вкладених структур, знімки та цілісність черги.");
    }

    private static void assertKeys(List<RequestRow> rows, List<String> expectedKeys, String label) {
        List<String> actual = rows.stream().map(r -> r.requestId().value()).toList();
        assertEquals(expectedKeys, actual, label);
    }

    private static void assertMoneyEquals(BigDecimal actual, String expectedPlain, String label) {
        if (actual == null) {
            throw new AssertionError(label + ": очікувалось " + expectedPlain + ", отримано null");
        }
        if (actual.scale() != 2) {
            throw new AssertionError(label + ": очікувався масштаб 2, отримано " + actual.scale());
        }
        BigDecimal expected = new BigDecimal(expectedPlain);
        if (actual.compareTo(expected) != 0) {
            throw new AssertionError(label + ": очікувалось " + expectedPlain + ", отримано " + actual.toPlainString());
        }
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

    private static void assertThrows(Class<? extends Throwable> expectedType, Runnable action, String label) {
        try {
            action.run();
        } catch (Throwable t) {
            if (expectedType.isInstance(t)) {
                return;
            }
            throw new AssertionError(label + ": очікувався виняток " + expectedType.getSimpleName()
                    + ", але викинуто " + t.getClass().getSimpleName(), t);
        }
        throw new AssertionError(label + ": очікувався виняток " + expectedType.getSimpleName() + ", але винятку не було");
    }
}