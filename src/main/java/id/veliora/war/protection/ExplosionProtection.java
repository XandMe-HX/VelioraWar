package id.veliora.war.protection;

import id.veliora.war.arena.Arena;
import id.veliora.war.arena.ArenaFlag;

public final class ExplosionProtection {
    public boolean mayDamageBlocks(Arena arena) {
        return arena.flag(ArenaFlag.EXPLOSION_BLOCK_DAMAGE);
    }

    public boolean mayDamageEntities(Arena arena) {
        return arena.flag(ArenaFlag.EXPLOSION_DAMAGE);
    }
}
