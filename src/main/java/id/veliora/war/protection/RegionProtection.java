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

    public boolean maintenanceEditor(Player player) {
        return arenas.maintenance() && player.hasPermission("veliorawar.admin");
    }

    public boolean canPlace(Player player, Arena ignoredRegionProfile) {
        if (maintenanceEditor(player)) return true;
        Arena activity = matches.arena(player.getUniqueId()).orElse(null);
        return activity != null && matches.mayModifyArena(player.getUniqueId())
                && activity.flag(ArenaFlag.BLOCK_PLACE);
    }

    public boolean canBreak(Player player, Arena ignoredRegionProfile) {
        if (maintenanceEditor(player)) return true;
        Arena activity = matches.arena(player.getUniqueId()).orElse(null);
        return activity != null && matches.mayModifyArena(player.getUniqueId())
                && activity.flag(ArenaFlag.BLOCK_BREAK);
    }
}
