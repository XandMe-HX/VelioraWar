package id.veliora.war.listener;

import id.veliora.war.arena.Arena;
import id.veliora.war.arena.ArenaFlag;
import id.veliora.war.inventory.LoadoutManager;
import id.veliora.war.match.MatchManager;
import id.veliora.war.match.MatchMode;
import id.veliora.war.protection.RegionProtection;
import id.veliora.war.protection.TemporaryBlockManager;
import id.veliora.war.storage.ConfigManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

public final class BlockListener implements Listener {
    private final ConfigManager configs;
    private final RegionProtection regions;
    private final MatchManager matches;
    private final LoadoutManager loadouts;
    private final TemporaryBlockManager temporaryBlocks;

    public BlockListener(ConfigManager configs, RegionProtection regions, MatchManager matches, LoadoutManager loadouts,
                         TemporaryBlockManager temporaryBlocks) {
        this.configs = configs;
        this.regions = regions;
        this.matches = matches;
        this.loadouts = loadouts;
        this.temporaryBlocks = temporaryBlocks;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Arena land = regions.arena(event.getBlock().getLocation()).orElse(null);
        if (land == null) return;
        Player player = event.getPlayer();
        if (regions.maintenanceEditor(player)) return;
        Arena activity = matches.arena(player.getUniqueId()).orElse(null);
        MatchMode mode = matches.mode(player.getUniqueId()).orElse(null);
        if (activity == null || !regions.canPlace(player, land) || mode == null
                || !loadouts.isAllowed(mode, event.getBlockPlaced().getType())) {
            event.setCancelled(true);
            return;
        }
        if (activity.flag(ArenaFlag.TEMPORARY_BLOCK)) {
            temporaryBlocks.capture(activity, event.getBlockPlaced(), event.getBlockReplacedState().getBlockData());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        org.bukkit.block.Block target = event.getBlockClicked().getRelative(event.getBlockFace());
        Arena land = regions.arena(target.getLocation()).orElse(null);
        if (land == null) return;
        if (regions.maintenanceEditor(event.getPlayer())) return;
        Arena activity = matches.arena(event.getPlayer().getUniqueId()).orElse(null);
        MatchMode mode = matches.mode(event.getPlayer().getUniqueId()).orElse(null);
        if (activity == null || !regions.canPlace(event.getPlayer(), land) || mode == null
                || !loadouts.isAllowed(mode, event.getBucket())) {
            event.setCancelled(true);
            return;
        }
        if (configs.config().getBoolean("protection.restore-liquid-flow", true)
                && activity.flag(ArenaFlag.TEMPORARY_BLOCK)) temporaryBlocks.capture(activity, target);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        Arena land = regions.arena(event.getBlockClicked().getLocation()).orElse(null);
        if (land == null) return;
        if (regions.maintenanceEditor(event.getPlayer())) return;
        Arena activity = matches.arena(event.getPlayer().getUniqueId()).orElse(null);
        if (activity == null || !regions.canBreak(event.getPlayer(), land)) {
            event.setCancelled(true);
            return;
        }
        if (configs.config().getBoolean("protection.restore-liquid-flow", true)
                && activity.flag(ArenaFlag.TEMPORARY_BLOCK)) temporaryBlocks.capture(activity, event.getBlockClicked());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLiquidFlow(BlockFromToEvent event) {
        Arena land = regions.arena(event.getBlock().getLocation()).orElse(null);
        if (land == null) return;
        if (!land.flag(ArenaFlag.LIQUID_FLOW)) {
            event.setCancelled(true);
            return;
        }
        if (!configs.config().getBoolean("protection.restore-liquid-flow", true)) return;
        for (Arena arena : matches.arenasAt(event.getBlock().getLocation())) {
            if (arena.flag(ArenaFlag.TEMPORARY_BLOCK)) temporaryBlocks.capture(arena, event.getToBlock());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Arena land = regions.arena(event.getBlock().getLocation()).orElse(null);
        if (land == null) return;
        if (regions.maintenanceEditor(event.getPlayer())) return;
        Arena activity = matches.arena(event.getPlayer().getUniqueId()).orElse(null);
        if (activity == null || !regions.canBreak(event.getPlayer(), land)) {
            event.setCancelled(true);
            return;
        }
        if (activity.flag(ArenaFlag.TEMPORARY_BLOCK)) temporaryBlocks.capture(activity, event.getBlock());
        event.setDropItems(false);
        event.setExpToDrop(0);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        Arena land = regions.arena(event.getClickedBlock().getLocation()).orElse(null);
        if (land == null) return;
        if (regions.maintenanceEditor(event.getPlayer())) return;
        Arena activity = matches.arena(event.getPlayer().getUniqueId()).orElse(null);
        if (activity == null || !land.flag(ArenaFlag.INTERACT)) {
            event.setCancelled(true);
            return;
        }
        Material type = event.getClickedBlock().getType();
        if (type == Material.RESPAWN_ANCHOR || type == Material.OBSIDIAN || type == Material.BEDROCK) {
            if (activity.flag(ArenaFlag.TEMPORARY_BLOCK)) temporaryBlocks.capture(activity, event.getClickedBlock());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        regions.arena(event.getBlock().getLocation()).ifPresent(land -> {
            if (!land.flag(ArenaFlag.FIRE_SPREAD)) event.setCancelled(true);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        regions.arena(event.getBlock().getLocation()).ifPresent(land -> {
            if (!land.flag(ArenaFlag.FIRE_SPREAD)) event.setCancelled(true);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        Arena land = regions.arena(event.getBlock().getLocation()).orElse(null);
        if (land != null && !land.flag(ArenaFlag.PISTON)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        Arena land = regions.arena(event.getBlock().getLocation()).orElse(null);
        if (land != null && !land.flag(ArenaFlag.PISTON)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMobSpawn(CreatureSpawnEvent event) {
        Arena land = regions.arena(event.getLocation()).orElse(null);
        if (land != null && !land.flag(ArenaFlag.MOB_SPAWN)
                && event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Arena land = regions.arena(event.getItem().getLocation()).orElse(null);
        if (land != null && !land.flag(ArenaFlag.ITEM_PICKUP)) event.setCancelled(true);
    }
}
