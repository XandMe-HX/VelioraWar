package id.veliora.war.match;

import id.veliora.war.arena.Arena;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Match {
    private final Arena arena;
    private final MatchMode mode;
    private final MatchSize size;
    private final EnumMap<MatchTeam, LinkedHashSet<UUID>> teams = new EnumMap<>(MatchTeam.class);
    private final Set<UUID> eliminated = new LinkedHashSet<>();
    private final Map<UUID, Double> damage = new HashMap<>();
    private BukkitTask countdownTask;
    private BukkitTask timerTask;
    private int remainingSeconds;

    public Match(Arena arena) {
        this.arena = arena;
        this.mode = arena.mode();
        this.size = arena.size();
        teams.put(MatchTeam.RED, new LinkedHashSet<>());
        teams.put(MatchTeam.GREEN, new LinkedHashSet<>());
    }

    public Arena arena() { return arena; }
    public MatchMode mode() { return mode; }
    public MatchSize size() { return size; }

    public boolean add(UUID player, MatchTeam team) {
        if (team(player) != null || teams.get(team).size() >= size.playersPerTeam()) return false;
        return teams.get(team).add(player);
    }

    public void remove(UUID player) {
        teams.values().forEach(team -> team.remove(player));
        eliminated.remove(player);
        damage.remove(player);
    }

    public MatchTeam team(UUID player) {
        for (MatchTeam team : MatchTeam.values()) if (teams.get(team).contains(player)) return team;
        return null;
    }

    public Set<UUID> teamPlayers(MatchTeam team) {
        return Collections.unmodifiableSet(teams.get(team));
    }

    public Set<UUID> players() {
        LinkedHashSet<UUID> result = new LinkedHashSet<>(teams.get(MatchTeam.RED));
        result.addAll(teams.get(MatchTeam.GREEN));
        return result;
    }

    public int teamCount(MatchTeam team) { return teams.get(team).size(); }
    public boolean hasSpace(MatchTeam team) { return teamCount(team) < size.playersPerTeam(); }
    public boolean ready() {
        return teamCount(MatchTeam.RED) == size.playersPerTeam()
                && teamCount(MatchTeam.GREEN) == size.playersPerTeam();
    }

    public void eliminate(UUID player) { eliminated.add(player); }
    public boolean eliminated(UUID player) { return eliminated.contains(player); }
    public boolean allEliminated(MatchTeam team) {
        return !teams.get(team).isEmpty() && teams.get(team).stream().allMatch(eliminated::contains);
    }
    public void addDamage(UUID player, double amount) { damage.merge(player, Math.max(0, amount), Double::sum); }
    public double teamDamage(MatchTeam team) {
        return teams.get(team).stream().mapToDouble(player -> damage.getOrDefault(player, 0.0)).sum();
    }
    public BukkitTask countdownTask() { return countdownTask; }
    public void countdownTask(BukkitTask task) { this.countdownTask = task; }
    public BukkitTask timerTask() { return timerTask; }
    public void timerTask(BukkitTask task) { this.timerTask = task; }
    public int remainingSeconds() { return remainingSeconds; }
    public void remainingSeconds(int seconds) { this.remainingSeconds = seconds; }
    public void cancelTasks() {
        if (countdownTask != null) countdownTask.cancel();
        if (timerTask != null) timerTask.cancel();
    }
}
