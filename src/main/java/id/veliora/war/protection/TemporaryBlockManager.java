package id.veliora.war.protection;

import id.veliora.war.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TemporaryBlockManager {
    private final Map<String, Map<BlockKey, BlockData>> changes = new HashMap<>();

    public void capture(Arena arena, Block block) {
        capture(arena, block, block.getBlockData());
    }

    public void capture(Arena arena, Block block, BlockData originalData) {
        changes.computeIfAbsent(arena.id(), ignored -> new LinkedHashMap<>())
                .putIfAbsent(BlockKey.of(block), originalData.clone());
    }

    public void restore(Arena arena) {
        Map<BlockKey, BlockData> blocks = changes.remove(arena.id());
        if (blocks == null) return;
        for (Map.Entry<BlockKey, BlockData> entry : blocks.entrySet()) {
            World world = Bukkit.getWorld(entry.getKey().world());
            if (world != null) world.getBlockAt(entry.getKey().x(), entry.getKey().y(), entry.getKey().z())
                    .setBlockData(entry.getValue(), false);
        }
    }

    public void restoreAll(Iterable<Arena> arenas) {
        for (Arena arena : arenas) restore(arena);
        changes.clear();
    }

    private record BlockKey(String world, int x, int y, int z) {
        static BlockKey of(Block block) {
            return new BlockKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        }
    }
}
