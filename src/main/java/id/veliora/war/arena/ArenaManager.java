package id.veliora.war.arena;

import id.veliora.war.match.MatchMode;
import id.veliora.war.match.MatchSize;
import id.veliora.war.storage.ArenaStorage;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ArenaManager {
    private final ArenaStorage storage;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();
    private final Map<UUID, Location> firstPositions = new LinkedHashMap<>();
    private final Map<UUID, Location> secondPositions = new LinkedHashMap<>();

    public ArenaManager(ArenaStorage storage) {
        this.storage = storage;
        reload();
    }

    public void reload() {
        arenas.clear();
        arenas.putAll(storage.loadAll());
    }

    public void save() {
        storage.saveAll(arenas.values());
    }

    public void setPosition(Player player, int number) {
        if (number == 1) firstPositions.put(player.getUniqueId(), player.getLocation());
        else secondPositions.put(player.getUniqueId(), player.getLocation());
    }

    public Location position(Player player, int number) {
        Location location = number == 1 ? firstPositions.get(player.getUniqueId()) : secondPositions.get(player.getUniqueId());
        return location == null ? null : location.clone();
    }

    public Arena create(Player player, String id) {
        String normalized = normalize(id);
        if (arenas.containsKey(normalized)) return null;
        Location first = position(player, 1);
        Location second = position(player, 2);
        if (first == null || second == null) throw new IllegalStateException("positions-missing");
        ArenaRegion region = new ArenaRegion(first, second);
        boolean overlapping = arenas.values().stream().anyMatch(existing -> existing.region().intersects(region));
        if (overlapping) throw new IllegalArgumentException("Arena bertabrakan dengan region arena lain");
        Arena arena = new Arena(normalized, region);
        arenas.put(normalized, arena);
        save();
        return arena;
    }

    public Optional<Arena> get(String id) {
        return Optional.ofNullable(arenas.get(normalize(id)));
    }

    public Optional<Arena> at(Location location) {
        return arenas.values().stream().filter(arena -> arena.region().contains(location)).findFirst();
    }

    public Optional<Arena> available(MatchMode mode, MatchSize size) {
        return arenas.values().stream()
                .filter(Arena::enabled)
                .filter(arena -> arena.state() == ArenaState.WAITING)
                .filter(arena -> arena.mode() == mode)
                .filter(arena -> arena.size() == size)
                .min(Comparator.comparing(Arena::id));
    }

    public Optional<Arena> allMode() {
        return arenas.values().stream()
                .filter(Arena::enabled)
                .filter(arena -> arena.mode() == MatchMode.ALL_MODE)
                .findFirst();
    }

    public boolean delete(String id) {
        Arena removed = arenas.remove(normalize(id));
        if (removed != null) save();
        return removed != null;
    }

    public Collection<Arena> all() {
        return List.copyOf(arenas.values());
    }

    public List<String> ids() {
        return new ArrayList<>(arenas.keySet());
    }

    private String normalize(String id) {
        return id.toLowerCase().replace(' ', '_');
    }
}
