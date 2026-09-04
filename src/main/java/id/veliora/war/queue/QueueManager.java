package id.veliora.war.queue;

import id.veliora.war.match.MatchMode;
import id.veliora.war.match.MatchSize;
import id.veliora.war.match.MatchTeam;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class QueueManager {
    private final Map<String, Deque<QueueEntry>> queues = new HashMap<>();

    public int enqueue(UUID player, MatchMode mode, MatchSize size, MatchTeam team, int maximum) {
        remove(player);
        Deque<QueueEntry> queue = queues.computeIfAbsent(key(mode, size), ignored -> new ArrayDeque<>());
        if (queue.size() >= maximum) return -1;
        queue.addLast(new QueueEntry(player, mode, size, team));
        return queue.size();
    }

    public Optional<QueueEntry> poll(MatchMode mode, MatchSize size) {
        Deque<QueueEntry> queue = queues.get(key(mode, size));
        if (queue == null) return Optional.empty();
        QueueEntry entry = queue.pollFirst();
        if (queue.isEmpty()) queues.remove(key(mode, size));
        return Optional.ofNullable(entry);
    }

    public void remove(UUID player) {
        queues.values().forEach(queue -> queue.removeIf(entry -> entry.playerId().equals(player)));
        queues.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public boolean contains(UUID player) {
        return queues.values().stream().anyMatch(queue -> queue.stream().anyMatch(entry -> entry.playerId().equals(player)));
    }

    public int count(MatchMode mode) {
        String prefix = mode.id() + ':';
        return queues.entrySet().stream().filter(entry -> entry.getKey().startsWith(prefix))
                .mapToInt(entry -> entry.getValue().size()).sum();
    }

    public void clear() {
        queues.clear();
    }

    public java.util.List<QueueEntry> entries() {
        return queues.values().stream().flatMap(Deque::stream).toList();
    }

    public int count(MatchMode mode, MatchSize size) {
        return queues.getOrDefault(key(mode, size), new ArrayDeque<>()).size();
    }

    private String key(MatchMode mode, MatchSize size) {
        return mode.id() + ':' + size.id();
    }
}
