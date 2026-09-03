package id.veliora.war.match;

import id.veliora.war.VelioraWarPlugin;
import id.veliora.war.arena.Arena;
import id.veliora.war.arena.ArenaManager;
import id.veliora.war.arena.ArenaState;
import id.veliora.war.cooldown.CooldownManager;
import id.veliora.war.inventory.InventoryManager;
import id.veliora.war.inventory.LoadoutManager;
import id.veliora.war.protection.TemporaryBlockManager;
import id.veliora.war.queue.QueueEntry;
import id.veliora.war.queue.QueueManager;
import id.veliora.war.storage.ConfigManager;
import id.veliora.war.storage.MessageManager;
import id.veliora.war.storage.PlayerDataStorage;
import id.veliora.war.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class MatchManager {
    private final VelioraWarPlugin plugin;
    private final ConfigManager configs;
    private final ArenaManager arenas;
    private final MessageManager messages;
    private final InventoryManager inventories;
    private final LoadoutManager loadouts;
    private final TemporaryBlockManager temporaryBlocks;
    private final CooldownManager cooldowns;
    private final PlayerDataStorage playerData;
    private final QueueManager queue = new QueueManager();
    private final Map<String, Match> matchesByArena = new HashMap<>();
    private final Map<UUID, Match> matchesByPlayer = new HashMap<>();
    private final Map<UUID, Arena> allModePlayers = new HashMap<>();
    private final Set<UUID> internalTeleports = new HashSet<>();
    private final Map<UUID, Long> combatUntil = new HashMap<>();
    private final Map<UUID, Long> spawnProtectionUntil = new HashMap<>();
    private final Map<UUID, DuelRequest> duelRequests = new HashMap<>();

    private record DuelRequest(UUID challenger, MatchMode mode, long expiresAt) { }

    public MatchManager(VelioraWarPlugin plugin, ConfigManager configs, ArenaManager arenas,
                        MessageManager messages, InventoryManager inventories, LoadoutManager loadouts,
                        TemporaryBlockManager temporaryBlocks, CooldownManager cooldowns,
                        PlayerDataStorage playerData) {
        this.plugin = plugin;
        this.configs = configs;
        this.arenas = arenas;
        this.messages = messages;
        this.inventories = inventories;
        this.loadouts = loadouts;
        this.temporaryBlocks = temporaryBlocks;
        this.cooldowns = cooldowns;
        this.playerData = playerData;
    }

    public boolean joinTeam(Player player, MatchMode mode, MatchSize size, MatchTeam team) {
        return joinTeam(player, mode, size, team, true);
    }

    private boolean joinTeam(Player player, MatchMode mode, MatchSize size, MatchTeam team, boolean allowQueue) {
        if (!configs.config().getBoolean("settings.enabled", true)) {
            messages.send(player, "maintenance");
            return false;
        }
        if (isPlaying(player.getUniqueId())) {
            messages.send(player, "already-playing");
            return false;
        }
        long cooldown = cooldowns.remaining(player.getUniqueId(), "cheat-kick");
        if (cooldown <= 0) cooldown = cooldowns.remaining(player.getUniqueId(), "join");
        if (cooldown > 0) {
            messages.send(player, "cooldown", Map.of("time", id.veliora.war.util.TimeUtil.formatMillis(cooldown)));
            return false;
        }

        Match match = matchesByArena.values().stream()
                .filter(candidate -> candidate.mode() == mode && candidate.size() == size)
                .filter(candidate -> candidate.arena().state() == ArenaState.PREPARING)
                .filter(candidate -> candidate.hasSpace(team))
                .findFirst().orElse(null);
        if (match == null) {
            Optional<Arena> free = arenas.all().stream()
                    .filter(Arena::enabled)
                    .filter(arena -> arena.state() == ArenaState.WAITING)
                    .filter(arena -> arena.mode() == mode)
                    .filter(arena -> !matchesByArena.containsKey(arena.id()))
                    .findFirst();
            if (free.isPresent()) {
                match = new Match(free.get(), size);
                matchesByArena.put(free.get().id(), match);
                free.get().state(ArenaState.PREPARING);
            }
        }
        if (match == null) {
            if (!allowQueue || !configs.config().getBoolean("queue.enabled", true)) return false;
            int position = queue.enqueue(player.getUniqueId(), mode, size, team,
                    configs.config().getInt("queue.max-size", 100));
            if (position < 0) messages.send(player, "queue-full");
            else messages.send(player, "queued", Map.of("position", Integer.toString(position)));
            return false;
        }
        if (!match.add(player.getUniqueId(), team)) {
            messages.send(player, "team-full");
            return false;
        }
        if (!inventories.backup(player)) {
            match.remove(player.getUniqueId());
            messages.send(player, "already-playing");
            return false;
        }
        queue.remove(player.getUniqueId());
        matchesByPlayer.put(player.getUniqueId(), match);
        teleportInternal(player, match.arena().spawn(team));
        messages.send(player, "joined-team", Map.of("team", team.displayName()));
        if (match.ready()) startCountdown(match);
        return true;
    }

    public boolean isAllMode(UUID player) { return allModePlayers.containsKey(player); }

    public void tagCombat(Player player) {
        if (isPlaying(player.getUniqueId())) combatUntil.put(player.getUniqueId(), System.currentTimeMillis() + 10_000L);
    }

    public long combatRemaining(UUID player) {
        return Math.max(0L, combatUntil.getOrDefault(player, 0L) - System.currentTimeMillis());
    }

    public boolean leaveAllMode(Player player) {
        if (!isAllMode(player.getUniqueId())) return false;
        if (combatRemaining(player.getUniqueId()) > 0L) return false;
        leave(player, true);
        return true;
    }

    public boolean requestDuel(Player challenger, Player target, MatchMode mode) {
        if (!mode.isPlayable() || mode == MatchMode.ALL_MODE || challenger.equals(target)
                || isPlaying(challenger.getUniqueId()) || isPlaying(target.getUniqueId())
                || combatRemaining(challenger.getUniqueId()) > 0L || combatRemaining(target.getUniqueId()) > 0L
                || cooldowns.remaining(challenger.getUniqueId(), "duel") > 0L
                || cooldowns.remaining(target.getUniqueId(), "duel") > 0L
                || duelRequests.containsKey(target.getUniqueId())) return false;
        duelRequests.put(target.getUniqueId(), new DuelRequest(challenger.getUniqueId(), mode, System.currentTimeMillis() + 60_000L));
        return true;
    }

    public Optional<MatchMode> acceptDuel(Player target) {
        DuelRequest request = duelRequests.remove(target.getUniqueId());
        if (request == null || request.expiresAt() < System.currentTimeMillis()) return Optional.empty();
        Player challenger = Bukkit.getPlayer(request.challenger());
        if (challenger == null || !challenger.isOnline() || isPlaying(challenger.getUniqueId()) || isPlaying(target.getUniqueId())) return Optional.empty();
        if (!joinTeam(challenger, request.mode(), MatchSize.ONE_VS_ONE, MatchTeam.RED, false)) return Optional.empty();
        if (!joinTeam(target, request.mode(), MatchSize.ONE_VS_ONE, MatchTeam.GREEN, false)) {
            leave(challenger, false);
            return Optional.empty();
        }
        cooldowns.set(challenger.getUniqueId(), "duel", 60_000L);
        cooldowns.set(target.getUniqueId(), "duel", 60_000L);
        return Optional.of(request.mode());
    }

    public Optional<UUID> denyDuel(Player target) {
        DuelRequest request = duelRequests.remove(target.getUniqueId());
        if (request == null) return Optional.empty();
        cooldowns.set(target.getUniqueId(), "duel", 60_000L);
        cooldowns.set(request.challenger(), "duel", 60_000L);
        return Optional.of(request.challenger());
    }

    public Optional<UUID> cancelDuel(Player challenger) {
        for (Map.Entry<UUID, DuelRequest> entry : new ArrayList<>(duelRequests.entrySet())) {
            if (entry.getValue().challenger().equals(challenger.getUniqueId())) {
                duelRequests.remove(entry.getKey());
                cooldowns.set(challenger.getUniqueId(), "duel", 60_000L);
                cooldowns.set(entry.getKey(), "duel", 60_000L);
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    public boolean joinAllMode(Player player) {
        if (!configs.config().getBoolean("settings.enabled", true)) {
            messages.send(player, "maintenance");
            return false;
        }
        if (isPlaying(player.getUniqueId())) {
            messages.send(player, "already-playing");
            return false;
        }
        long cooldown = cooldowns.remaining(player.getUniqueId(), "cheat-kick");
        if (cooldown <= 0) cooldown = cooldowns.remaining(player.getUniqueId(), "join");
        if (cooldown > 0) {
            messages.send(player, "cooldown", Map.of("time", id.veliora.war.util.TimeUtil.formatMillis(cooldown)));
            return false;
        }
        Arena arena = arenas.allMode().orElse(null);
        if (arena == null || !arena.enabled() || !arena.isComplete()) {
            messages.send(player, "arena-not-found", Map.of("arena", "all_mode"));
            return false;
        }
        if (!inventories.backup(player)) return false;
        allModePlayers.put(player.getUniqueId(), arena);
        teleportInternal(player, arena.redSpawn());
        loadouts.apply(player, MatchMode.ALL_MODE);
        long protectionMillis = Math.max(0L,
                configs.config().getLong("all-mode.spawn-protection-seconds", 5L)) * 1000L;
        if (protectionMillis > 0L) {
            spawnProtectionUntil.put(player.getUniqueId(), System.currentTimeMillis() + protectionMillis);
            player.showTitle(Title.title(TextUtil.component("&a&lAMAN"),
                    TextUtil.component("&7Perlindungan spawn selama " + (protectionMillis / 1000L) + " detik")));
        }
        messages.send(player, "fair-play");
        messages.send(player, "joined-all-mode");
        return true;
    }

    private void startCountdown(Match match) {
        match.arena().state(ArenaState.COUNTDOWN);
        int seconds = Math.max(3, configs.config().getInt("match.countdown-seconds", 4));
        match.remainingSeconds(seconds);
        forEach(match, player -> {
            player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
            if (configs.config().getBoolean("match.freeze-blindness", true)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, seconds * 20 + 20, 0, false, false));
            }
            messages.send(player, "fair-play");
        });
        match.countdownTask(new BukkitRunnable() {
            @Override
            public void run() {
                if (!match.ready()) {
                    cancel();
                    match.arena().state(ArenaState.PREPARING);
                    return;
                }
                int remaining = match.remainingSeconds();
                if (remaining <= 0) {
                    cancel();
                    startActive(match);
                    return;
                }
                if (remaining <= 3) {
                    forEach(match, player -> {
                        player.showTitle(Title.title(TextUtil.component("&e&l" + remaining), Component.empty(),
                                Title.Times.times(Duration.ZERO, Duration.ofMillis(900), Duration.ofMillis(100))));
                        playConfiguredSound(player, "match.countdown-sound", Sound.BLOCK_NOTE_BLOCK_HAT,
                                0.8F, 1.0F + ((3 - remaining) * 0.2F));
                    });
                }
                match.remainingSeconds(remaining - 1);
            }
        }.runTaskTimer(plugin, 0L, 20L));
    }

    private void startActive(Match match) {
        match.arena().state(ArenaState.ACTIVE);
        forEach(match, player -> {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.setGameMode(GameMode.SURVIVAL);
            loadouts.apply(player, match.mode());
            player.showTitle(Title.title(TextUtil.component("&a&lGO!"), Component.empty()));
            playConfiguredSound(player, "match.start-sound", Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.15F);
            messages.send(player, "match-start");
        });
        match.remainingSeconds(configs.config().getInt("match.duration-seconds", 600));
        match.timerTask(new BukkitRunnable() {
            @Override
            public void run() {
                int remaining = match.remainingSeconds() - 1;
                match.remainingSeconds(remaining);
                int suddenAt = Math.max(1, configs.config().getInt("match.sudden-death.start-at-seconds", 60));
                if (remaining == suddenAt && configs.config().getBoolean("match.sudden-death.enabled", true)) {
                    startSuddenDeath(match);
                }
                if (remaining <= 0) {
                    cancel();
                    finish(match, timeoutResult(match));
                }
            }
        }.runTaskTimer(plugin, 20L, 20L));
    }

    private void startSuddenDeath(Match match) {
        if (match.suddenDeath()) return;
        match.suddenDeath(true);
        double health = Math.max(1.0D, configs.config().getDouble("match.sudden-death.health", 1.0D));
        forEach(match, player -> {
            if (match.eliminated(player.getUniqueId())) return;
            player.removePotionEffect(PotionEffectType.REGENERATION);
            player.removePotionEffect(PotionEffectType.ABSORPTION);
            player.setAbsorptionAmount(0.0D);
            double max = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) == null
                    ? 20.0D : player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
            player.setHealth(Math.min(max, health));
            player.showTitle(Title.title(TextUtil.component("&4&lSUDDEN DEATH"),
                    TextUtil.component("&c1 HP • Totem dan refill dinonaktifkan")));
            messages.send(player, "sudden-death");
        });
    }

    public void recordDamage(Player attacker, Player victim, double damage) {
        Match match = matchesByPlayer.get(attacker.getUniqueId());
        if (match == null || match != matchesByPlayer.get(victim.getUniqueId()) || match.arena().state() != ArenaState.ACTIVE) return;
        match.addDamage(attacker.getUniqueId(), damage);
    }

    public boolean friendly(Player first, Player second) {
        Match match = matchesByPlayer.get(first.getUniqueId());
        return match != null && match == matchesByPlayer.get(second.getUniqueId())
                && match.team(first.getUniqueId()) == match.team(second.getUniqueId());
    }

    public void eliminate(Player player) {
        Match match = matchesByPlayer.get(player.getUniqueId());
        if (match == null || match.eliminated(player.getUniqueId())) return;
        match.eliminate(player.getUniqueId());
        player.setGameMode(GameMode.SPECTATOR);
        Location spawn = match.arena().spawn(match.team(player.getUniqueId()));
        if (spawn != null) teleportInternal(player, spawn.clone().add(0, 2, 0));
        if (match.allEliminated(MatchTeam.RED)) finish(match, MatchResult.winner(MatchTeam.GREEN, messages.raw("match-elimination")));
        else if (match.allEliminated(MatchTeam.GREEN)) finish(match, MatchResult.winner(MatchTeam.RED, messages.raw("match-elimination")));
    }

    public void handleRespawn(Player player) {
        Arena allArena = allModePlayers.get(player.getUniqueId());
        if (allArena != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                teleportInternal(player, allArena.redSpawn());
                loadouts.apply(player, MatchMode.ALL_MODE);
            });
            return;
        }
        Match match = matchesByPlayer.get(player.getUniqueId());
        if (match != null) Bukkit.getScheduler().runTask(plugin, () -> eliminate(player));
    }

    public void handleVoid(Player player) {
        Arena allArena = allModePlayers.get(player.getUniqueId());
        if (allArena != null) {
            player.setFallDistance(0);
            double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) == null
                    ? 20.0 : player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
            player.setHealth(Math.min(20.0, maxHealth));
            teleportInternal(player, allArena.redSpawn());
            loadouts.apply(player, MatchMode.ALL_MODE);
            return;
        }
        Match match = matchesByPlayer.get(player.getUniqueId());
        if (match != null && !configs.config().getBoolean("void.count-as-death", false)) {
            player.setFallDistance(0);
            double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) == null
                    ? 20.0 : player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
            if (configs.config().getBoolean("void.restore-health", true)) {
                player.setHealth(maxHealth);
            }
            Location teamSpawn = match.arena().spawn(match.team(player.getUniqueId()));
            if (teamSpawn != null) teleportInternal(player, teamSpawn);
            return;
        }
        eliminate(player);
    }

    public void leave(Player player, boolean notify) {
        leaveInternal(player, notify, true);
    }

    public void disconnect(Player player) {
        leaveInternal(player, false, false);
        inventories.restoreWarAttributes(player);
    }

    private void leaveInternal(Player player, boolean notify, boolean restoreInventory) {
        UUID uuid = player.getUniqueId();
        combatUntil.remove(uuid);
        boolean wasQueued = queue.contains(uuid);
        queue.remove(uuid);
        Arena allArena = allModePlayers.remove(uuid);
        if (allArena != null) {
            if (restoreInventory) inventories.restore(player, configs.stay());
            cooldowns.set(uuid, "join", configs.config().getLong("cooldowns.join-seconds", 3) * 1000L);
            if (allModePlayers.values().stream().noneMatch(arena -> arena.id().equals(allArena.id()))) temporaryBlocks.restore(allArena);
            if (notify) messages.send(player, "left");
            return;
        }
        Match match = matchesByPlayer.remove(uuid);
        if (match == null) {
            if (wasQueued && notify) messages.send(player, "left");
            return;
        }
        MatchTeam oldTeam = match.team(uuid);
        if (match.arena().state() == ArenaState.ACTIVE) match.eliminate(uuid);
        else match.remove(uuid);
        if (restoreInventory) inventories.restore(player, configs.stay());
        cooldowns.set(uuid, "join", configs.config().getLong("cooldowns.join-seconds", 3) * 1000L);
        if (notify) messages.send(player, "left");

        if (match.arena().state() == ArenaState.ACTIVE && oldTeam != null && match.allEliminated(oldTeam)) {
            finish(match, MatchResult.winner(oldTeam == MatchTeam.RED ? MatchTeam.GREEN : MatchTeam.RED,
                    messages.raw("match-elimination")));
        } else if (match.players().isEmpty()) {
            destroyWaitingMatch(match);
        } else if (match.arena().state() == ArenaState.COUNTDOWN && !match.ready()) {
            match.cancelTasks();
            match.arena().state(ArenaState.PREPARING);
        }
    }

    public void kickForCheat(Player player) {
        leave(player, false);
        cooldowns.set(player.getUniqueId(), "cheat-kick",
                configs.config().getLong("cooldowns.cheat-kick-seconds", 86400) * 1000L);
        messages.send(player, "cheat-kicked");
    }

    private MatchResult timeoutResult(Match match) {
        double red = match.teamDamage(MatchTeam.RED);
        double green = match.teamDamage(MatchTeam.GREEN);
        String reason = messages.raw("match-timeout");
        if (Double.compare(red, green) == 0) return MatchResult.draw(reason);
        return MatchResult.winner(red > green ? MatchTeam.RED : MatchTeam.GREEN, reason);
    }

    private void finish(Match match, MatchResult result) {
        if (match.arena().state() == ArenaState.ENDING) return;
        match.arena().state(ArenaState.ENDING);
        match.cancelTasks();
        forEach(match, player -> {
            if (result.draw()) messages.send(player, "match-draw", Map.of("reason", result.reason()));
            else messages.send(player, "match-win", Map.of("winner", result.winner().displayName(), "reason", result.reason()));
        });
        int delay = Math.max(0, configs.config().getInt("match.end-delay-seconds", 3));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (UUID uuid : new ArrayList<>(match.players())) {
                matchesByPlayer.remove(uuid, match);
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    inventories.restore(player, configs.stay());
                    cooldowns.set(uuid, "join", configs.config().getLong("cooldowns.join-seconds", 3) * 1000L);
                    playerData.increment(uuid, result.draw() ? "stats.draws" :
                            (match.team(uuid) == result.winner() ? "stats.wins" : "stats.losses"));
                }
            }
            playerData.save();
            temporaryBlocks.restore(match.arena());
            matchesByArena.remove(match.arena().id(), match);
            match.arena().state(match.arena().enabled() ? ArenaState.WAITING : ArenaState.DISABLED);
            processQueue(match.mode(), match.size());
        }, delay * 20L);
    }

    private void destroyWaitingMatch(Match match) {
        match.cancelTasks();
        matchesByArena.remove(match.arena().id(), match);
        match.arena().state(match.arena().enabled() ? ArenaState.WAITING : ArenaState.DISABLED);
    }

    private void processQueue(MatchMode mode, MatchSize size) {
        int maximum = Math.min(8, size.playersPerTeam() * 2);
        for (int index = 0; index < maximum; index++) {
            Optional<QueueEntry> next = queue.poll(mode, size);
            if (next.isEmpty()) return;
            Player player = Bukkit.getPlayer(next.get().playerId());
            if (player != null && player.isOnline()) joinTeam(player, mode, size, next.get().team(), false);
        }
    }

    public java.util.Collection<Arena> arenasAt(Location location) {
        Set<Arena> active = new HashSet<>();
        for (Match match : matchesByArena.values()) {
            if (match.arena().region().contains(location)) active.add(match.arena());
        }
        for (Arena arena : allModePlayers.values()) {
            if (arena.region().contains(location)) active.add(arena);
        }
        return active;
    }

    public String modeStatus(MatchMode mode) {
        if (!configs.config().getBoolean("settings.enabled", true)) return "&e● MAINTENANCE";
        Arena arena = arenas.forMode(mode).orElse(null);
        if (arena == null || !arena.enabled() || !arena.isComplete()) return "&c● BELUM SIAP";
        Match match = matchesByArena.get(arena.id());
        if (match == null) return "&a● SIAP DIMAINKAN";
        return switch (arena.state()) {
            case ACTIVE, COUNTDOWN -> "&c● SEDANG BERMAIN";
            case PREPARING -> "&e● MENUNGGU PEMAIN";
            case ENDING -> "&6● MENYELESAIKAN MATCH";
            default -> "&a● SIAP DIMAINKAN";
        };
    }

    public String modePopulation(MatchMode mode) {
        Arena arena = arenas.forMode(mode).orElse(null);
        Match match = arena == null ? null : matchesByArena.get(arena.id());
        int inside = match == null ? 0 : match.players().size();
        int waiting = queue.count(mode);
        return "&7Di arena: &f" + inside + " &8• &7Antrean: &f" + waiting;
    }

    public boolean modeAvailable(MatchMode mode) {
        return configs.config().getBoolean("settings.enabled", true)
                && arenas.forMode(mode).filter(Arena::enabled).filter(Arena::isComplete).isPresent();
    }

    public boolean isWithinLand(Location location) {
        return location != null && arenas.at(location).isPresent();
    }

    public void teleportToStay(Player player) {
        Location stay = configs.stay();
        if (stay == null) return;
        player.setFallDistance(0.0F);
        double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) == null
                ? 20.0D : player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        if (!player.isDead()) player.setHealth(Math.min(maxHealth, Math.max(1.0D, player.getHealth())));
        teleportInternal(player, stay);
    }

    public boolean isSuddenDeath(UUID player) {
        Match match = matchesByPlayer.get(player);
        return match != null && match.suddenDeath();
    }

    public boolean hasSpawnProtection(UUID player) {
        long until = spawnProtectionUntil.getOrDefault(player, 0L);
        if (until <= System.currentTimeMillis()) {
            spawnProtectionUntil.remove(player);
            return false;
        }
        return true;
    }

    public boolean isPlaying(UUID player) {
        return matchesByPlayer.containsKey(player) || allModePlayers.containsKey(player) || queue.contains(player);
    }

    /** A participant physically controlled by a match. Queued players are deliberately excluded. */
    public boolean isParticipant(UUID player) {
        return matchesByPlayer.containsKey(player) || allModePlayers.containsKey(player);
    }

    /** Block-changing gameplay is legal only after the duel has actually started, or in All Mode. */
    public boolean mayModifyArena(UUID player) {
        if (allModePlayers.containsKey(player)) return true;
        Match match = matchesByPlayer.get(player);
        return match != null && match.arena().state() == ArenaState.ACTIVE && !match.eliminated(player);
    }

    public boolean isInArena(UUID player, Arena arena) {
        Match match = matchesByPlayer.get(player);
        return (match != null && match.arena().id().equals(arena.id()))
                || (allModePlayers.get(player) != null && allModePlayers.get(player).id().equals(arena.id()));
    }

    public boolean isFrozen(UUID player) {
        Match match = matchesByPlayer.get(player);
        return match != null && (match.arena().state() == ArenaState.PREPARING || match.arena().state() == ArenaState.COUNTDOWN);
    }

    public Optional<Arena> arena(UUID player) {
        Match match = matchesByPlayer.get(player);
        if (match != null) return Optional.of(match.arena());
        return Optional.ofNullable(allModePlayers.get(player));
    }

    public Optional<MatchMode> mode(UUID player) {
        Match match = matchesByPlayer.get(player);
        if (match != null) return Optional.of(match.mode());
        return allModePlayers.containsKey(player) ? Optional.of(MatchMode.ALL_MODE) : Optional.empty();
    }

    public boolean eliminated(UUID player) {
        Match match = matchesByPlayer.get(player);
        return match != null && match.eliminated(player);
    }

    public boolean consumeInternalTeleport(UUID player) {
        return internalTeleports.remove(player);
    }

    public void teleportInternal(Player player, Location location) {
        if (location == null) return;
        internalTeleports.add(player.getUniqueId());
        player.teleport(location);
        Bukkit.getScheduler().runTask(plugin, () -> internalTeleports.remove(player.getUniqueId()));
    }

    public void shutdown() {
        for (Match match : new ArrayList<>(matchesByArena.values())) {
            match.cancelTasks();
            for (UUID uuid : match.players()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) inventories.restore(player, configs.stay());
            }
            temporaryBlocks.restore(match.arena());
        }
        for (UUID uuid : new ArrayList<>(allModePlayers.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) inventories.restore(player, configs.stay());
        }
        matchesByArena.clear();
        matchesByPlayer.clear();
        allModePlayers.clear();
        combatUntil.clear();
        spawnProtectionUntil.clear();
        duelRequests.clear();
    }

    /** Ends every activity immediately for maintenance without recording a fake win/loss. */
    public int forceFinishAll() {
        Set<UUID> affected = new HashSet<>();
        for (Match match : new ArrayList<>(matchesByArena.values())) {
            match.cancelTasks();
            affected.addAll(match.players());
            temporaryBlocks.restore(match.arena());
            match.arena().state(ArenaState.DISABLED);
        }
        affected.addAll(allModePlayers.keySet());
        for (UUID uuid : affected) {
            queue.remove(uuid);
            combatUntil.remove(uuid);
            spawnProtectionUntil.remove(uuid);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                inventories.restore(player, configs.stay());
                player.sendMessage(TextUtil.component("&8[&bVelioraWar&8] &ePertandingan dihentikan karena maintenance."));
            }
        }
        matchesByArena.clear();
        matchesByPlayer.clear();
        allModePlayers.clear();
        queue.clear();
        duelRequests.clear();
        return affected.size();
    }

    private void playConfiguredSound(Player player, String path, Sound fallback, float volume, float pitch) {
        String configured = configs.config().getString(path, fallback.name());
        Sound sound;
        try {
            sound = Sound.valueOf(configured.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            sound = fallback;
        }
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private void forEach(Match match, java.util.function.Consumer<Player> action) {
        for (UUID uuid : match.players()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) action.accept(player);
        }
    }
}
