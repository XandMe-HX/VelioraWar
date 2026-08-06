package id.veliora.war.listener;

import id.veliora.war.arena.Arena;
import id.veliora.war.arena.ArenaFlag;
import id.veliora.war.arena.ArenaManager;
import id.veliora.war.match.MatchManager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public final class DamageListener implements Listener {
    private final ArenaManager arenas;
    private final MatchManager matches;

    public DamageListener(ArenaManager arenas, MatchManager matches) {
        this.arenas = arenas;
        this.matches = matches;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Arena regionArena = arenas.at(victim.getLocation()).orElse(null);
        Arena activityArena = matches.arena(victim.getUniqueId()).orElse(null);
        if (regionArena != null && activityArena == null) {
            event.setCancelled(true);
            return;
        }
        if (activityArena == null) return;
        if (matches.isFrozen(victim.getUniqueId()) || matches.eliminated(victim.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && !activityArena.flag(ArenaFlag.FALL_DAMAGE)) {
            event.setCancelled(true);
            return;
        }
        if ((event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                || event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION)
                && !activityArena.flag(ArenaFlag.EXPLOSION_DAMAGE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = attacker(event);
        if (attacker == null) return;
        Arena victimArena = matches.arena(victim.getUniqueId()).orElse(null);
        Arena attackerArena = matches.arena(attacker.getUniqueId()).orElse(null);
        if (victimArena == null || attackerArena == null || !victimArena.id().equals(attackerArena.id())) {
            if (arenas.at(victim.getLocation()).isPresent()) event.setCancelled(true);
            return;
        }
        if (!victimArena.flag(ArenaFlag.PVP) || matches.isFrozen(victim.getUniqueId())
                || matches.isFrozen(attacker.getUniqueId()) || matches.eliminated(victim.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (matches.friendly(attacker, victim)) {
            event.setCancelled(true);
            return;
        }
        matches.recordDamage(attacker, victim, event.getFinalDamage());
    }

    private Player attacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) return player;
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
        return null;
    }
}
