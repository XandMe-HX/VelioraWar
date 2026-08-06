package id.veliora.war.npc;

import org.bukkit.entity.Entity;

public interface NpcHook {
    boolean isRefillNpc(Entity entity);
    String arenaId(Entity entity);
}
