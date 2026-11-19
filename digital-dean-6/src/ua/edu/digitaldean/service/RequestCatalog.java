package ua.edu.digitaldean.service;
import java.util.ArrayDeque;
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
    public Optional<T> pollPending() {
        RequestKey key = pending.poll();
        return key == null ? Optional.empty() : Optional.of(byId.get(key));
    }
    public int size() {
        return byId.size();
    }
    public int pendingCount() {
        return pending.size();
    }
}