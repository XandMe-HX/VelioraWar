package id.veliora.war.arena;

import id.veliora.war.match.MatchMode;
import id.veliora.war.match.MatchSize;
import id.veliora.war.storage.ArenaStorage;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
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
        Map<String, Arena> loaded = storage.loadAll();
        arenas.clear();
        if (loaded.isEmpty()) return;
        migrateToModeProfiles(loaded);
    }

    private void migrateToModeProfiles(Map<String, Arena> loaded) {
        ArenaRegion global = unionRegion(loaded.values());
        for (MatchMode mode : MatchMode.values()) {
            Arena source = loaded.get(mode.id());
            if (source == null) {
                source = loaded.values().stream().filter(arena -> arena.mode() == mode).findFirst().orElse(null);
            }
            Arena profile = new Arena(mode.id(), global);
            profile.mode(mode);
            profile.size(mode == MatchMode.ALL_MODE ? MatchSize.UNLIMITED : MatchSize.FOUR_VS_FOUR);
            if (source != null) copySettings(source, profile);
            arenas.put(profile.id(), profile);
        }
        save();
    }

    private ArenaRegion unionRegion(Collection<Arena> source) {
        Arena first = source.iterator().next();
        String world = first.region().world();
        int minX = first.region().minX();
        int minY = first.region().minY();
        int minZ = first.region().minZ();
        int maxX = first.region().maxX();
        int maxY = first.region().maxY();
        int maxZ = first.region().maxZ();
        for (Arena arena : source) {
            if (!world.equals(arena.region().world())) continue;
            minX = Math.min(minX, arena.region().minX());
            minY = Math.min(minY, arena.region().minY());
            minZ = Math.min(minZ, arena.region().minZ());
            maxX = Math.max(maxX, arena.region().maxX());
            maxY = Math.max(maxY, arena.region().maxY());
            maxZ = Math.max(maxZ, arena.region().maxZ());
        }
        return new ArenaRegion(world, minX, minY, minZ, maxX, maxY, maxZ);
    }

    private void copySettings(Arena source, Arena target) {
        target.redSpawn(source.redSpawn());
        target.greenSpawn(source.greenSpawn());
        target.refillNpcLocation(source.refillNpcLocation());
        for (ArenaFlag flag : ArenaFlag.values()) target.flag(flag, source.flag(flag));
        target.enabled(source.enabled() && target.isComplete());
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

    public void claimGlobal(Player player) {
        Location first = position(player, 1);
        Location second = position(player, 2);
        if (first == null || second == null) throw new IllegalStateException("Tentukan pos1 dan pos2 terlebih dahulu");
        ArenaRegion region = new ArenaRegion(first, second);
        Map<MatchMode, Arena> previous = new EnumMap<>(MatchMode.class);
        for (Arena arena : arenas.values()) {
            if (arena.mode() != null) previous.putIfAbsent(arena.mode(), arena);
        }
        arenas.clear();
        for (MatchMode mode : MatchMode.values()) {
            Arena profile = new Arena(mode.id(), region);
            profile.mode(mode);
            profile.size(mode == MatchMode.ALL_MODE ? MatchSize.UNLIMITED : MatchSize.FOUR_VS_FOUR);
            Arena old = previous.get(mode);
            if (old != null) copySettings(old, profile);
            profile.enabled(false);
            arenas.put(profile.id(), profile);
        }
        save();
    }

    public Optional<Arena> forMode(MatchMode mode) {
        return Optional.ofNullable(arenas.get(mode.id()));
    }

    public Optional<Arena> get(String id) {
        MatchMode mode = MatchMode.from(id).orElse(null);
        if (mode != null) return forMode(mode);
        return Optional.ofNullable(arenas.get(normalize(id)));
    }

    public Optional<Arena> at(Location location) {
        return arenas.values().stream().filter(arena -> arena.region().contains(location)).findFirst();
    }

    public Optional<Arena> available(MatchMode mode, MatchSize size) {
        return forMode(mode).filter(Arena::enabled).filter(arena -> arena.state() == ArenaState.WAITING);
    }

    public Optional<Arena> allMode() {
        return forMode(MatchMode.ALL_MODE);
    }

    public boolean hasLand() {
        return !arenas.isEmpty();
    }

    public boolean maintenance() {
        return arenas.values().stream().noneMatch(Arena::enabled);
    }

    public boolean hasActiveMatch() {
        return arenas.values().stream().anyMatch(arena ->
                arena.state() != ArenaState.WAITING && arena.state() != ArenaState.DISABLED);
    }

    public int enableCompleteProfiles() {
        int enabled = 0;
        for (Arena arena : arenas.values()) {
            boolean complete = arena.isComplete();
            arena.enabled(complete);
            if (complete) enabled++;
        }
        save();
        return enabled;
    }

    public void disableAll() {
        if (hasActiveMatch()) throw new IllegalStateException("Masih ada pertandingan aktif. Tunggu selesai sebelum maintenance");
        arenas.values().forEach(arena -> arena.enabled(false));
        save();
    }

    public void deleteLand() {
        if (hasActiveMatch()) throw new IllegalStateException("Masih ada pertandingan aktif. Land tidak boleh dihapus");
        arenas.clear();
        save();
    }

    public boolean delete(String id) {
        Arena removed = get(id).map(arena -> arenas.remove(arena.id())).orElse(null);
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
        return id.toLowerCase().replace(' ', '_').replace('-', '_');
    }
}
