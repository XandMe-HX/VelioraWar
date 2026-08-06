package id.veliora.war.listener;

import id.veliora.war.arena.Arena;
import id.veliora.war.arena.ArenaFlag;
import id.veliora.war.arena.ArenaManager;
import id.veliora.war.protection.ExplosionProtection;
import id.veliora.war.protection.TemporaryBlockManager;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public final class ExplosionListener implements Listener {
    private final ArenaManager arenas;
    private final ExplosionProtection protection;
    private final TemporaryBlockManager temporaryBlocks;

    public ExplosionListener(ArenaManager arenas, ExplosionProtection protection,
                             TemporaryBlockManager temporaryBlocks) {
        this.arenas = arenas;
        this.protection = protection;
        this.temporaryBlocks = temporaryBlocks;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        Arena arena = arenas.at(event.getLocation()).orElse(null);
        if (arena == null) return;
        protect(arena, event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        Arena arena = arenas.at(event.getBlock().getLocation()).orElse(null);
        if (arena == null) return;
        protect(arena, event.blockList());
    }

    private void protect(Arena arena, java.util.List<Block> blocks) {
        if (!protection.mayDamageBlocks(arena)) {
            blocks.clear();
            return;
        }
        if (arena.flag(ArenaFlag.TEMPORARY_BLOCK)) blocks.forEach(block -> temporaryBlocks.capture(arena, block));
    }
}
