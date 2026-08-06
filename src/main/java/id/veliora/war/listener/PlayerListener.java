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
                () -> inventories.restorePending(event.getPlayer(), configs.warp()), 10L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        matches.leave(event.getPlayer(), false);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMove(PlayerMoveEvent event) {
        if (!changedPosition(event.getFrom(), event.getTo())) return;
        Player player = event.getPlayer();
        Arena arena = matches.arena(player.getUniqueId()).orElse(null);
        if (arena == null) return;
        if (matches.isFrozen(player.getUniqueId())) {
            Location look = event.getFrom().clone();
            look.setYaw(event.getTo().getYaw());
            look.setPitch(event.getTo().getPitch());
            event.setTo(look);
            return;
        }
        if (configs.config().getBoolean("void.enabled", true)
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

        // Selama berada dalam war, pemain tidak boleh memakai /tp, /home, /spawn,
        // teleport plugin lain, ender pearl, atau cara teleport lain untuk kabur.
        // Semua perpindahan lokasi war hanya dilakukan lewat MatchManager.
        if (!configs.config().getBoolean("war-lock.block-external-teleport", true)) return;
        event.setCancelled(true);
        messages.send(player, "teleport-blocked");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Arena arena = matches.arena(event.getPlayer().getUniqueId()).orElse(null);
        if (arena != null && configs.config().getBoolean("war-lock.block-commands", true)) {
            event.setCancelled(true);
            messages.send(event.getPlayer(), "command-blocked");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent event) {
        matches.arena(event.getPlayer().getUniqueId()).ifPresent(arena -> {
            if (!arena.flag(ArenaFlag.ITEM_DROP)) event.setCancelled(true);
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
        Arena arena = plugin.arenaManager().get(npcs.arenaId(event.getRightClicked())).orElse(null);
        if (arena != null && matches.isInArena(event.getPlayer().getUniqueId(), arena)) refillGui.open(event.getPlayer(), arena);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        Arena arena = matches.arena(player.getUniqueId()).orElse(null);
        if (arena == null) return;
        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
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
