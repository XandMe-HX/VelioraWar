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
import org.bukkit.event.entity.EntityResurrectEvent;
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

import java.util.Map;

public final class PlayerListener implements Listener {
    private final VelioraWarPlugin plugin;
    private final ConfigManager configs;
    private final MessageManager messages;
    private final MatchManager matches;
    private final InventoryManager inventories;
    private final CooldownManager cooldowns;
    private final RefillNpcManager npcs;
    private final RefillGui refillGui;

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
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> inventories.restorePending(event.getPlayer(), configs.stay()), 10L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        matches.disconnect(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMove(PlayerMoveEvent event) {
        if (!changedPosition(event.getFrom(), event.getTo())) return;
        Player player = event.getPlayer();
        Arena arena = matches.arena(player.getUniqueId()).orElse(null);
        if (arena == null) return;
        if (configs.config().getBoolean("match.freeze-players", true) && matches.isFrozen(player.getUniqueId())) {
            player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
            Location frozen = event.getFrom().clone();
            if (configs.config().getBoolean("match.freeze-allow-look", true)) {
                frozen.setYaw(event.getTo().getYaw());
                frozen.setPitch(event.getTo().getPitch());
            }
            event.setTo(frozen);
            return;
        }
        if (configs.config().getBoolean("void.enabled", true)
                && arena.flag(ArenaFlag.VOID_TELEPORT)
                && event.getTo().getY() <= configs.config().getDouble("void.y-level", -64)) {
            event.setCancelled(true);
            matches.handleVoid(player);
            return;
        }
        if (!arena.region().contains(event.getTo())) {
            event.setTo(event.getFrom());
            if (cooldowns.tryUse(player.getUniqueId(), "outside-message", 2000)) messages.send(player, "outside-arena");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Arena arena = matches.arena(player.getUniqueId()).orElse(null);
        if (arena == null) return;

        // Teleport yang dipanggil oleh VelioraWar (spawn, void, hasil match) harus tetap jalan.
        if (matches.consumeInternalTeleport(player.getUniqueId())) return;

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
        if (arena == null) return;

        // A player must always have one safe escape route from an activity.
        String command = event.getMessage().trim().toLowerCase(java.util.Locale.ROOT);
        if (command.equals("/vgwar leave") || command.equals("/vgwar keluar")) {
            event.setCancelled(true);
            matches.leave(player, true);
            return;
        }
        if (arena.flag(ArenaFlag.ALLOW_COMMAND)) return;
        if (configs.config().getBoolean("war-lock.block-commands", true)) {
            event.setCancelled(true);
            messages.send(player, "command-blocked");
        }
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.ENDER_CHEST) return;
        if (matches.isPlaying(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onNpcClick(PlayerInteractEntityEvent event) {
        if (!npcs.isRefillNpc(event.getRightClicked())) return;
        event.setCancelled(true);
        Arena arena = matches.arena(event.getPlayer().getUniqueId()).orElse(null);
        if (arena != null && arena.mode() == id.veliora.war.match.MatchMode.ALL_MODE
                && matches.isInArena(event.getPlayer().getUniqueId(), arena)) {
            refillGui.open(event.getPlayer(), arena);
        }
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
