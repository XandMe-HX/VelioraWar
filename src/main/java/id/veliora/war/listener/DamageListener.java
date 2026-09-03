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
    private record RecentBlast(java.util.UUID owner, Location location, long time) {}
    private final ArenaManager arenas;
    private final MatchManager matches;
    private final java.util.Map<java.util.UUID, java.util.UUID> crystalOwners = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, RecentBlast> anchorBlasts = new java.util.HashMap<>();

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
        if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION && friendlyAnchorBlast(victim)) {
            event.setCancelled(true);
            return;
        }
        if (matches.isFrozen(victim.getUniqueId()) || matches.hasSpawnProtection(victim.getUniqueId())
                || matches.eliminated(victim.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && !activityArena.flag(ArenaFlag.FALL_DAMAGE)) {
            event.setCancelled(true);
            return;
        }
        if (event.getFinalDamage() > 0.0D) matches.tagCombat(victim);
        if ((event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                || event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION)
                && !activityArena.flag(ArenaFlag.EXPLOSION_DAMAGE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPvp(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof EnderCrystal crystal) {
            Player owner = attacker(event);
            if (owner != null && matches.isPlaying(owner.getUniqueId())) crystalOwners.put(crystal.getUniqueId(), owner.getUniqueId());
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnchorUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null
                || event.getClickedBlock().getType() != Material.RESPAWN_ANCHOR
                || !matches.isPlaying(event.getPlayer().getUniqueId())) return;
        anchorBlasts.put(event.getPlayer().getUniqueId(),
                new RecentBlast(event.getPlayer().getUniqueId(), event.getClickedBlock().getLocation(), System.currentTimeMillis()));
    }

    private boolean friendlyAnchorBlast(Player victim) {
        long now = System.currentTimeMillis();
        anchorBlasts.values().removeIf(blast -> now - blast.time() > 2500L);
        for (RecentBlast blast : anchorBlasts.values()) {
            if (blast.location().getWorld() != victim.getWorld()
                    || blast.location().distanceSquared(victim.getLocation()) > 144.0D) continue;
            Player owner = org.bukkit.Bukkit.getPlayer(blast.owner());
            if (owner != null && matches.friendly(owner, victim)) return true;
        }
        return false;
    }
}
