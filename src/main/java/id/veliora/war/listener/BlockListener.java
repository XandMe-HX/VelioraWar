package id.veliora.war.listener;

import id.veliora.war.arena.Arena;
import id.veliora.war.arena.ArenaFlag;
import id.veliora.war.inventory.LoadoutManager;
import id.veliora.war.match.MatchManager;
import id.veliora.war.match.MatchMode;
import id.veliora.war.protection.RegionProtection;
import id.veliora.war.protection.TemporaryBlockManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

public final class BlockListener implements Listener {
    private final RegionProtection regions;
    private final MatchManager matches;
    private final LoadoutManager loadouts;
    private final TemporaryBlockManager temporaryBlocks;

    public BlockListener(RegionProtection regions, MatchManager matches, LoadoutManager loadouts,
                         TemporaryBlockManager temporaryBlocks) {
        this.regions = regions;
        this.matches = matches;
        this.loadouts = loadouts;
        this.temporaryBlocks = temporaryBlocks;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Arena arena = regions.arena(event.getBlock().getLocation()).orElse(null);
        if (arena == null) return;
        Player player = event.getPlayer();
        MatchMode mode = matches.mode(player.getUniqueId()).orElse(null);
        if (!regions.canPlace(player, arena) || mode == null || !loadouts.isAllowed(mode, event.getBlockPlaced().getType())) {
            event.setCancelled(true);
            return;
        }
        if (arena.flag(ArenaFlag.TEMPORARY_BLOCK)) {
            temporaryBlocks.capture(arena, event.getBlockPlaced(), event.getBlockReplacedState().getBlockData());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        org.bukkit.block.Block target = event.getBlockClicked().getRelative(event.getBlockFace());
        Arena arena = regions.arena(target.getLocation()).orElse(null);
        if (arena == null) return;
        MatchMode mode = matches.mode(event.getPlayer().getUniqueId()).orElse(null);
        if (!regions.canPlace(event.getPlayer(), arena) || mode == null || !loadouts.isAllowed(mode, event.getBucket())) {
            event.setCancelled(true);
            return;
        }
        if (arena.flag(ArenaFlag.TEMPORARY_BLOCK)) temporaryBlocks.capture(arena, target);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        Arena arena = regions.arena(event.getBlockClicked().getLocation()).orElse(null);
        if (arena == null) return;
        if (!regions.canBreak(event.getPlayer(), arena)) {
            event.setCancelled(true);
            return;
        }
        if (arena.flag(ArenaFlag.TEMPORARY_BLOCK)) temporaryBlocks.capture(arena, event.getBlockClicked());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLiquidFlow(BlockFromToEvent event) {
        Arena arena = regions.arena(event.getBlock().getLocation()).orElse(null);
        if (arena == null || !arena.flag(ArenaFlag.TEMPORARY_BLOCK)) return;
        temporaryBlocks.capture(arena, event.getToBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Arena arena = regions.arena(event.getBlock().getLocation()).orElse(null);
        if (arena == null) return;
        if (!regions.canBreak(event.getPlayer(), arena)) {
            event.setCancelled(true);
            return;
        }
        if (arena.flag(ArenaFlag.TEMPORARY_BLOCK)) temporaryBlocks.capture(arena, event.getBlock());
        event.setDropItems(false);
        event.setExpToDrop(0);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        Arena arena = regions.arena(event.getClickedBlock().getLocation()).orElse(null);
        if (arena == null) return;
        if (!matches.isInArena(event.getPlayer().getUniqueId(), arena)) {
            event.setCancelled(true);
            return;
        }
        Material type = event.getClickedBlock().getType();
        if (type == Material.RESPAWN_ANCHOR || type == Material.OBSIDIAN || type == Material.BEDROCK) {
            if (arena.flag(ArenaFlag.TEMPORARY_BLOCK)) temporaryBlocks.capture(arena, event.getClickedBlock());
        }
    }
}
