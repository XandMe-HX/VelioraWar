package id.veliora.war.listener;

import id.veliora.war.arena.Arena;
import id.veliora.war.arena.ArenaFlag;
import id.veliora.war.arena.ArenaManager;
import id.veliora.war.match.MatchManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public final class DamageListener implements Listener {
    private final ArenaManager arenas;
    private final MatchManager matches;
    private final java.util.Map<java.util.UUID, java.util.UUID> crystalOwners = new java.util.HashMap<>();

    public DamageListener(ArenaManager arenas, MatchManager matches) {
        this.arenas = arenas;
        this.matches = matches;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Arena regionArena = arenas.at(victim.getLocation()).orElse(null);
        Arena activityArena = matches.arena(victim.getUniqueId()).orElse(null);
        if (regionArena != null && activityArena == null) {
            event.setCancelled(true);
            return;
        }
        if (activityArena == null) return;
        // A recent nearby anchor click is not proof of the source of this explosion.
        if (matches.isFrozen(victim.getUniqueId()) || matches.hasSpawnProtection(victim.getUniqueId())
                || matches.eliminated(victim.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && !activityArena.flag(ArenaFlag.FALL_DAMAGE)) {
            event.setCancelled(true);
            return;
        }
        if ((event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                || event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION)) {
            boolean allowed = activityArena.flag(ArenaFlag.EXPLOSION_DAMAGE)
                    && activityArena.flag(ArenaFlag.PVP)
                    && activityArena.region().contains(victim.getLocation());
            if (event instanceof EntityDamageByEntityEvent hit) {
                Player source = attacker(hit);
                if (source != null && (!matches.isInArena(source.getUniqueId(), activityArena)
                        || matches.isFrozen(source.getUniqueId()) || matches.hasSpawnProtection(source.getUniqueId())
                        || (!source.equals(victim) && matches.friendly(source, victim)))) allowed = false;
                if (allowed && source != null && !source.equals(victim)) {
                    matches.recordDamage(source, victim, event.getFinalDamage());
                    matches.tagCombat(source);
                }
            }
            event.setCancelled(!allowed);
        }
        if (!event.isCancelled() && event.getFinalDamage() > 0.0D) matches.tagCombat(victim);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPvp(EntityDamageByEntityEvent event) {
        // Explosions are handled once in onDamage, with the same flag and self-damage rules.
        if (event.getEntity() instanceof Player
                && (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                || event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION)) return;
        if (event.getEntity() instanceof EnderCrystal crystal) {
            Player owner = attacker(event);
            if (!event.isCancelled() && owner != null && matches.isParticipant(owner.getUniqueId())) {
                crystalOwners.put(crystal.getUniqueId(), owner.getUniqueId());
                org.bukkit.Bukkit.getScheduler().runTaskLater(
                        org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()),
                        () -> crystalOwners.remove(crystal.getUniqueId()), 2L);
            }
            return;
        }
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
                || matches.isFrozen(attacker.getUniqueId()) || matches.hasSpawnProtection(victim.getUniqueId())
                || matches.hasSpawnProtection(attacker.getUniqueId()) || matches.eliminated(victim.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (matches.friendly(attacker, victim)) {
            event.setCancelled(true);
            return;
        }
        // VelioraWar owns PvP inside an active match. Region plugins such as RedProtect may
        // protect the land globally, but their cancellation must not disable a valid duel.
        event.setCancelled(false);
        matches.recordDamage(attacker, victim, event.getFinalDamage());
        matches.tagCombat(attacker);
        matches.tagCombat(victim);
    }

    private Player attacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) return player;
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
        if (event.getDamager() instanceof EnderCrystal crystal) {
            java.util.UUID owner = crystalOwners.get(crystal.getUniqueId());
            return owner == null ? null : org.bukkit.Bukkit.getPlayer(owner);
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHeal(org.bukkit.event.entity.EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player player && matches.isParticipant(player.getUniqueId())) {
            matches.limitHealing(player, event);
        }
    }

}
