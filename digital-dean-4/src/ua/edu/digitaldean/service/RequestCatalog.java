package ua.edu.digitaldean.service;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import ua.edu.digitaldean.model.AcademicRequest;
import ua.edu.digitaldean.model.RequestKey;
import ua.edu.digitaldean.model.StudentId;

public final class RequestCatalog<T extends AcademicRequest> {
    private final Map<RequestKey, T> byId = new LinkedHashMap<>();
    private final Queue<RequestKey> pending = new ArrayDeque<>();

    public boolean add(T request) {
        Objects.requireNonNull(request, "Заява не може бути null.");
        RequestKey key = new RequestKey(request.requestId());
        if (byId.putIfAbsent(key, request) != null) {
            return false;
        }
        pending.offer(key);
        return true;
    }

    public int addAll(Collection<? extends T> source) {
        Objects.requireNonNull(source, "Колекція заяв не може бути null.");
        for (AcademicRequest req : source) {
            Objects.requireNonNull(req, "Елемент у колекції заяв не може бути null.");
        }

        int addedCount = 0;
        Map<RequestKey, T> temporaryBatchMap = new LinkedHashMap<>();
        for (T request : source) {
            RequestKey key = new RequestKey(request.requestId());
            temporaryBatchMap.putIfAbsent(key, request);
        }

        for (Map.Entry<RequestKey, T> entry : temporaryBatchMap.entrySet()) {
            RequestKey key = entry.getKey();
            T request = entry.getValue();
            if (byId.putIfAbsent(key, request) == null) {
                pending.offer(key);
                addedCount++;
            }
        }
        return addedCount;
    }

    public void copyTo(Collection<? super T> destination) {
        Objects.requireNonNull(destination, "Приймач не може бути null.");
        destination.addAll(byId.values());
    }

    public Optional<T> find(RequestKey key) {
        Objects.requireNonNull(key, "Ключ пошуку не може бути null.");
        return Optional.ofNullable(byId.get(key));
    }

    public List<T> snapshot() {
        return List.copyOf(byId.values());
    }

    public Set<StudentId> studentIds() {
        LinkedHashSet<StudentId> ids = byId.values().stream()
                .map(AcademicRequest::studentId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(ids);
    }

    public List<RequestKey> keysSorted() {
        return byId.keySet().stream().sorted().toList();
    }

    public List<T> select(
            Predicate<? super T> filter,
            Comparator<? super T> order) {
        Objects.requireNonNull(filter, "Умова відбору не може бути null.");
        Objects.requireNonNull(order, "Компаратор не може бути null.");
        return byId.values().stream().filter(filter).sorted(order).toList();
    }

    public List<T> select(
            Predicate<? super T> filter,
            Comparator<? super T> order,
            int limit) {
        Objects.requireNonNull(filter, "Умова відбору не може бути null.");
        Objects.requireNonNull(order, "Компаратор не може бути null.");
        if (limit < 0) {
            throw new IllegalArgumentException("Ліміт не може бути від'ємним.");
        }
        return byId.values().stream()
                .filter(filter)
                .sorted(order)
                .limit(limit)
                .toList();
    }

    public Map<StudentId, List<T>> groupByStudent() {
        Map<StudentId, List<T>> grouped = byId.values().stream()
                .collect(Collectors.groupingBy(
                        AcademicRequest::studentId,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    list.sort(Comparator.comparing(req -> new RequestKey(req.requestId())));
                                    return List.copyOf(list);
                                }
                        )
                ));

        Map<StudentId, List<T>> sortedMap = new LinkedHashMap<>();
        grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(StudentId::value)))
                .forEach(entry -> sortedMap.put(entry.getKey(), entry.getValue()));

        return Collections.unmodifiableMap(sortedMap);
    }

    public Map<String, Long> countByType() {
        Map<String, Long> counted = byId.values().stream()
                .collect(Collectors.groupingBy(
                        AcademicRequest::type,
                        Collectors.counting()
                ));

        Map<String, Long> sortedMap = new LinkedHashMap<>();
        counted.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> sortedMap.put(entry.getKey(), entry.getValue()));

        return Collections.unmodifiableMap(sortedMap);
    }

    public Optional<T> pollPending() {
        RequestKey key = pending.poll();
        return key == null ? Optional.empty() : Optional.of(byId.get(key));
    }

    public List<T> processNext(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Ліміт не може бути від'ємним.");
        }
        List<T> batch = new java.util.ArrayList<>();
        for (int i = 0; i < limit; i++) {
            Optional<T> next = pollPending();
            if (next.isEmpty()) {
                break;
            }
            batch.add(next.get());
        }
        return List.copyOf(batch);
    }

    public int size() {
        return byId.size();
    }

    public int pendingCount() {
        return pending.size();
    }
}