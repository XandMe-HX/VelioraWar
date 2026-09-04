package id.veliora.war.listener;

import id.veliora.war.VelioraWarPlugin;
import id.veliora.war.arena.Arena;
import id.veliora.war.arena.ArenaFlag;
import id.veliora.war.cooldown.CooldownManager;
import id.veliora.war.gui.RefillGui;
import id.veliora.war.inventory.InventoryManager;
import id.veliora.war.match.MatchManager;
import id.veliora.war.npc.RefillNpcManager;
import id.veliora.war.storage.ConfigManager;
import id.veliora.war.storage.MessageManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerListener implements Listener {
    private final VelioraWarPlugin plugin;
    private final ConfigManager configs;
    private final MessageManager messages;
    private final MatchManager matches;
    private final InventoryManager inventories;
    private final CooldownManager cooldowns;
    private final RefillNpcManager npcs;
    private final RefillGui refillGui;
    private final Map<UUID, Location> freezeAnchors = new HashMap<>();

    public PlayerListener(VelioraWarPlugin plugin, ConfigManager configs, MessageManager messages,
                          MatchManager matches, InventoryManager inventories, CooldownManager cooldowns,
                          RefillNpcManager npcs, RefillGui refillGui) {
        this.plugin = plugin;
        this.configs = configs;
        this.messages = messages;
        this.matches = matches;
        this.inventories = inventories;
        this.cooldowns = cooldowns;
        this.npcs = npcs;
        this.refillGui = refillGui;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        freezeAnchors.remove(event.getPlayer().getUniqueId());
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> inventories.restorePending(event.getPlayer(), configs.stay()), 10L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        freezeAnchors.remove(event.getPlayer().getUniqueId());
        matches.disconnect(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMove(PlayerMoveEvent event) {
        if (!changedPosition(event.getFrom(), event.getTo())) return;
        Player player = event.getPlayer();
        Arena arena = matches.arena(player.getUniqueId()).orElse(null);
        Location destination = event.getTo();
        if (configs.config().getBoolean("void.enabled", true)
                && destination != null
                && destination.getY() <= configs.config().getDouble("void.y-level", -64.0D)) {
            if (arena != null && arena.flag(ArenaFlag.VOID_TELEPORT)) {
                matches.handleVoid(player);
            } else if (arena == null && matches.isWithinLand(player.getLocation())) {
                matches.teleportToStay(player);
            }
            return;
        }
        // Player biasa tidak memiliki arena aktif. Jangan pernah membaca region dari nilai null.
        if (arena == null) {
            freezeAnchors.remove(player.getUniqueId());
            return;
        }

        if (configs.config().getBoolean("match.freeze-players", true)
                && matches.isFrozen(player.getUniqueId())) {
            Location anchor = freezeAnchors.computeIfAbsent(player.getUniqueId(),
                    ignored -> event.getFrom().clone());
            Location locked = anchor.clone();
            if (configs.config().getBoolean("match.freeze-allow-look", true) && event.getTo() != null) {
                locked.setYaw(event.getTo().getYaw());
                locked.setPitch(event.getTo().getPitch());
            }
            event.setTo(locked);
            player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
            return;
        }

        freezeAnchors.remove(player.getUniqueId());
        if (!arena.region().contains(event.getTo())) {
            event.setTo(event.getFrom());
            if (cooldowns.tryUse(player.getUniqueId(), "outside-message", 2000)) messages.send(player, "outside-arena");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Arena arena = matches.arena(player.getUniqueId()).orElse(null);
        if (arena == null || !matches.isParticipant(player.getUniqueId())) return;

        // Teleport yang dipanggil oleh VelioraWar (spawn, void, hasil match) harus tetap jalan.
        if (matches.consumeInternalTeleport(player.getUniqueId())) {
            freezeAnchors.remove(player.getUniqueId());
            return;
        }

        // Izinkan setback kecil dari anticheat/plugin selama tujuannya tetap di land.
        // Ini mencegah loop koreksi posisi dan spam pesan saat pemain sedang dibekukan.
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN
                && event.getTo() != null && arena.region().contains(event.getTo())
                && event.getFrom().getWorld() == event.getTo().getWorld()
                && event.getFrom().distanceSquared(event.getTo()) <= 64.0D) {
            if (matches.isFrozen(player.getUniqueId())) {
                freezeAnchors.put(player.getUniqueId(), event.getTo().clone());
            }
            return;
        }

        // Pemain tidak boleh meloloskan diri dari freeze memakai teleport apa pun.
        if (matches.isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // Ender pearl adalah bagian dari kit. Izinkan hanya bila titik akhirnya masih di claim arena.
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL
                && configs.config().getBoolean("war-lock.allow-ender-pearl-inside-arena", true)
                && event.getTo() != null && arena.region().contains(event.getTo())) return;

        // /tp, /home, chorus fruit, plugin lain, dan pearl keluar claim tetap diblokir.
        if (!configs.config().getBoolean("war-lock.block-external-teleport", true)) return;
        event.setCancelled(true);
        long cooldown = Math.max(250L, configs.config().getLong("war-lock.message-cooldown-milliseconds", 2000L));
        if (cooldowns.tryUse(player.getUniqueId(), "teleport-blocked-message", cooldown)) {
            messages.send(player, "teleport-blocked");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        Arena arena = matches.arena(player.getUniqueId()).orElse(null);
        if (arena == null || !matches.isParticipant(player.getUniqueId())) return;

        // /vgwar leave hanya boleh dipakai di All Mode dan selalu menghormati combat-tag.
        String command = event.getMessage().trim().toLowerCase(java.util.Locale.ROOT);
        if (command.equals("/vgwar")) return;
        if (command.equals("/vgwar leave") || command.equals("/vgwar keluar")) {
            event.setCancelled(true);
            if (!matches.isAllMode(player.getUniqueId())) {
                matches.leave(player, true);
                return;
            }
            long remaining = matches.combatRemaining(player.getUniqueId());
            if (remaining > 0L) {
                player.sendMessage(id.veliora.war.util.TextUtil.component("&8[&bVelioraWar&8] &cTunggu " + ((remaining + 999L) / 1000L) + " detik setelah combat sebelum keluar."));
                return;
            }
            matches.leaveAllMode(player);
            return;
        }
        if (configs.config().getBoolean("gsit-block.enabled", true)
                && (command.startsWith("/sit") || command.startsWith("/lay") || command.startsWith("/crawl")
                || command.startsWith("/bellyflop") || command.startsWith("/spin") || command.startsWith("/cartwheel")
                || command.startsWith("/pose") || command.startsWith("/emote") || command.startsWith("/gsit"))) {
            event.setCancelled(true);
            messages.send(player, "gsit-blocked");
            return;
        }
        if (arena.flag(ArenaFlag.ALLOW_COMMAND)) return;
        if (allowedDuringWar(command)) return;
        if (configs.config().getBoolean("war-lock.block-commands", true)) {
            event.setCancelled(true);
            messages.send(player, "command-blocked");
        }
    }

    private boolean allowedDuringWar(String command) {
        for (String allowed : configs.config().getStringList("war-lock.allowed-commands")) {
            String normalized = allowed.startsWith("/") ? allowed.toLowerCase(java.util.Locale.ROOT)
                    : "/" + allowed.toLowerCase(java.util.Locale.ROOT);
            if (command.equals(normalized) || command.startsWith(normalized + " ")) return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent event) {
        matches.arena(event.getPlayer().getUniqueId()).ifPresent(arena -> {
            if (!arena.flag(ArenaFlag.ITEM_DROP)) event.setCancelled(true);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTotem(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        matches.arena(player.getUniqueId()).ifPresent(arena -> {
            if (!arena.flag(ArenaFlag.ALLOW_TOTEM)) event.setCancelled(true);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player) || !event.isGliding()) return;
        matches.arena(player.getUniqueId()).ifPresent(arena -> {
            if (!arena.flag(ArenaFlag.ALLOW_ELYTRA)) event.setCancelled(true);
        });
    }

    /**
     * GSit mounts the player on an internal seat entity. Cancelling the mount stops
     * sit/lay/crawl without blocking normal bucket, pearl, bow, or shield use.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMount(EntityMountEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || !configs.config().getBoolean("gsit-block.enabled", true)
                || matches.arena(player.getUniqueId()).isEmpty()) return;
        event.setCancelled(true);
        long cooldown = Math.max(250L, configs.config().getLong("gsit-block.message-cooldown-milliseconds", 1500L));
        if (cooldowns.tryUse(player.getUniqueId(), "gsit-blocked-message", cooldown)) messages.send(player, "gsit-blocked");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.ENDER_CHEST) return;
        if (matches.isPlaying(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onNpcDamage(EntityDamageEvent event) {
        if (!npcs.isRefillNpc(event.getEntity())) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onNpcClick(PlayerInteractEntityEvent event) {
        if (!npcs.isRefillNpc(event.getRightClicked())) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        Arena arena = matches.arena(player.getUniqueId()).orElse(null);
        if (arena == null || arena.mode() != id.veliora.war.match.MatchMode.ALL_MODE
                || !matches.isInArena(player.getUniqueId(), arena)) {
            messages.send(player, "npc-refill-all-only");
            return;
        }
        if (matches.isSuddenDeath(player.getUniqueId())) {
            messages.send(player, "npc-refill-sudden");
            return;
        }
        refillGui.open(player, arena);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        Arena arena = matches.arena(player.getUniqueId()).orElse(null);
        if (arena == null) return;
        boolean keepInventory = arena.flag(ArenaFlag.KEEP_INVENTORY);
        event.setKeepInventory(keepInventory);
        if (keepInventory) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
        if (matches.mode(player.getUniqueId()).orElse(null) != id.veliora.war.match.MatchMode.ALL_MODE) matches.eliminate(player);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isDead()) player.spigot().respawn();
        }, 2L);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        matches.handleRespawn(event.getPlayer());
    }

    private boolean changedPosition(Location from, Location to) {
        return to != null && (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ());
    }
}
