package id.veliora.war.arena;

import id.veliora.war.match.MatchMode;
import id.veliora.war.match.MatchSize;
import id.veliora.war.match.MatchTeam;
import org.bukkit.Location;

import java.util.EnumMap;
import java.util.Map;

public final class Arena {
    private final String id;
    private ArenaRegion region;
    private MatchMode mode;
    private MatchSize size;
    private Location redSpawn;
    private Location greenSpawn;
    private Location refillNpcLocation;
    private boolean enabled;
    private ArenaState state;
    private final EnumMap<ArenaFlag, Boolean> flags = new EnumMap<>(ArenaFlag.class);

    public Arena(String id, ArenaRegion region) {
        this.id = id.toLowerCase();
        this.region = region;
        this.state = ArenaState.DISABLED;
        for (ArenaFlag flag : ArenaFlag.values()) flags.put(flag, flag.defaultValue());
    }

    public String id() { return id; }
    public ArenaRegion region() { return region; }
    public void region(ArenaRegion region) { this.region = region; }
    public MatchMode mode() { return mode; }
    public void mode(MatchMode mode) { this.mode = mode; }
    public MatchSize size() { return size; }
    public void size(MatchSize size) { this.size = size; }
    public Location redSpawn() { return redSpawn == null ? null : redSpawn.clone(); }
    public void redSpawn(Location location) { this.redSpawn = cloneLocation(location); }
    public Location greenSpawn() { return greenSpawn == null ? null : greenSpawn.clone(); }
    public void greenSpawn(Location location) { this.greenSpawn = cloneLocation(location); }
    public Location refillNpcLocation() { return refillNpcLocation == null ? null : refillNpcLocation.clone(); }
    public void refillNpcLocation(Location location) { this.refillNpcLocation = cloneLocation(location); }
    public boolean enabled() { return enabled; }
    public void enabled(boolean enabled) {
        this.enabled = enabled;
        this.state = enabled ? ArenaState.WAITING : ArenaState.DISABLED;
    }
    public ArenaState state() { return state; }
    public void state(ArenaState state) { this.state = state; }
    public boolean flag(ArenaFlag flag) { return flags.getOrDefault(flag, flag.defaultValue()); }
    public void flag(ArenaFlag flag, boolean value) { flags.put(flag, value); }
    public Map<ArenaFlag, Boolean> flags() { return Map.copyOf(flags); }
    public Location spawn(MatchTeam team) { return team == MatchTeam.RED ? redSpawn() : greenSpawn(); }

    public boolean isComplete() {
        if (region == null || mode == null || size == null || redSpawn == null) return false;
        return mode == MatchMode.ALL_MODE || greenSpawn != null;
    }

    private static Location cloneLocation(Location location) {
        return location == null ? null : location.clone();
    }
}
