package id.veliora.war.protection;

import id.veliora.war.arena.Arena;
import id.veliora.war.arena.ArenaFlag;
import id.veliora.war.arena.ArenaManager;
import id.veliora.war.match.MatchManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Optional;

public final class RegionProtection {
    private final ArenaManager arenas;
    private final MatchManager matches;

    public RegionProtection(ArenaManager arenas, MatchManager matches) {
        this.arenas = arenas;
        this.matches = matches;
    }

    public Optional<Arena> arena(Location location) {
        return arenas.at(location);
    }

    public boolean canPlace(Player player, Arena arena) {
        return matches.isInArena(player.getUniqueId(), arena) && arena.flag(ArenaFlag.BLOCK_PLACE);
    }

    public boolean canBreak(Player player, Arena arena) {
        return matches.isInArena(player.getUniqueId(), arena) && arena.flag(ArenaFlag.BLOCK_BREAK);
    }
}
