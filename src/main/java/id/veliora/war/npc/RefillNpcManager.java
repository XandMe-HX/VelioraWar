package id.veliora.war.npc;

import id.veliora.war.VelioraWarPlugin;
import id.veliora.war.arena.Arena;
import id.veliora.war.arena.ArenaManager;
import id.veliora.war.storage.ConfigManager;
import id.veliora.war.util.LocationUtil;
import id.veliora.war.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RefillNpcManager implements NpcHook {
    private final VelioraWarPlugin plugin;
    private final ConfigManager configs;
    private final ArenaManager arenas;
    private final NamespacedKey npcKey;
    private final NamespacedKey hologramKey;
    private BukkitTask lookTask;
    private final Map<UUID, String> activeNpcs = new HashMap<>();

    public RefillNpcManager(VelioraWarPlugin plugin, ConfigManager configs, ArenaManager arenas) {
        this.plugin = plugin;
        this.configs = configs;
        this.arenas = arenas;
        this.npcKey = new NamespacedKey(plugin, "refill_npc");
        this.hologramKey = new NamespacedKey(plugin, "refill_hologram");
        startLookTask();
    }

    public void spawnAll() {
        activeNpcs.clear();
        Bukkit.getWorlds().forEach(world -> world.getEntities().stream()
                .filter(entity -> entity.getPersistentDataContainer().has(npcKey)
                        || entity.getPersistentDataContainer().has(hologramKey))
                .forEach(Entity::remove));
        for (int slot = 1; slot <= 2; slot++) {
            Location location = configuredLocation(slot);
            if (location != null) spawn(location, "global-" + slot);
        }
    }

    public void set(int slot, Location location) {
        validateSlot(slot);
        remove("global-" + slot);
        configs.config().set("npc.locations." + slot, null);
        org.bukkit.configuration.ConfigurationSection section =
                configs.config().createSection("npc.locations." + slot);
        LocationUtil.save(section, location);
        plugin.saveConfig();
        spawn(location, "global-" + slot);
    }

    public void removeSlot(int slot) {
        validateSlot(slot);
        remove("global-" + slot);
        configs.config().set("npc.locations." + slot, null);
        plugin.saveConfig();
    }

    public void removeAll() {
        remove("global-1");
        remove("global-2");
        configs.config().set("npc.locations", null);
        plugin.saveConfig();
    }

    /** Compatibility for old arena-bound NPC data. New NPCs are global. */
    public void set(Arena arena, Location location) {
        set(1, location);
    }

    public void spawn(Arena arena) {
        // Old arena-bound NPCs are deliberately not respawned after migration.
    }

    private void spawn(Location location, String id) {
        if (location == null || location.getWorld() == null) return;
        location.getWorld().spawn(location, Villager.class, villager -> {
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.setSilent(configs.config().getBoolean("npc.silent", true));
            villager.setCollidable(false);
            villager.setRemoveWhenFarAway(false);
            villager.setPersistent(true);
            villager.setCanPickupItems(false);
            villager.setProfession(Villager.Profession.WEAPONSMITH);
            villager.customName(TextUtil.component(configs.config().getString("npc.name", "&e&l[ REFILL ITEMS ]")));
            villager.setCustomNameVisible(true);
            villager.getPersistentDataContainer().set(npcKey, PersistentDataType.STRING, id);
            activeNpcs.put(villager.getUniqueId(), id);
        });
        location.getWorld().spawn(location.clone().add(0, 2.2, 0), ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setMarker(true);
            stand.setInvulnerable(true);
            stand.setGravity(false);
            stand.setCollidable(false);
            stand.customName(TextUtil.component(configs.config().getString("npc.hologram", "&b&lCLICK TO REFILL")));
            stand.setCustomNameVisible(true);
            stand.getPersistentDataContainer().set(hologramKey, PersistentDataType.STRING, id);
        });
    }

    public void remove(String id) {
        Bukkit.getWorlds().forEach(world -> world.getEntities().stream().filter(entity -> {
            String npc = entity.getPersistentDataContainer().get(npcKey, PersistentDataType.STRING);
            String hologram = entity.getPersistentDataContainer().get(hologramKey, PersistentDataType.STRING);
            return id.equals(npc) || id.equals(hologram);
        }).forEach(Entity::remove));
        activeNpcs.values().removeIf(id::equals);
    }

    private void startLookTask() {
        if (lookTask != null) lookTask.cancel();
        lookTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            double range = Math.max(2.0D, configs.config().getDouble("npc.look-range", 10.0D));
            activeNpcs.entrySet().removeIf(entry -> {
                Entity entity = Bukkit.getEntity(entry.getKey());
                if (!(entity instanceof Villager villager) || !entity.isValid()) return true;
                String id = entry.getValue();
                Location home = home(id);
                if (home != null && home.getWorld() == villager.getWorld()
                        && villager.getLocation().distanceSquared(home) > 0.09D) {
                    villager.teleport(home);
                }
                villager.setVelocity(new Vector(0, 0, 0));
                Player nearest = villager.getWorld().getPlayers().stream()
                        .filter(Player::isOnline)
                        .filter(player -> player.getLocation().distanceSquared(villager.getLocation()) <= range * range)
                        .min(Comparator.comparingDouble(player ->
                                player.getLocation().distanceSquared(villager.getLocation())))
                        .orElse(null);
                if (nearest != null) face(villager, nearest);
                return false;
            });
        }, 10L, 10L);
    }

    private void face(Villager villager, Player player) {
        Location from = villager.getEyeLocation();
        Location to = player.getEyeLocation();
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        villager.setRotation(yaw, pitch);
    }

    private Location home(String id) {
        if (!id.startsWith("global-")) return null;
        try {
            return configuredLocation(Integer.parseInt(id.substring("global-".length())));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Location configuredLocation(int slot) {
        return LocationUtil.load(configs.config().getConfigurationSection("npc.locations." + slot));
    }

    private void validateSlot(int slot) {
        if (slot < 1 || slot > 2) throw new IllegalArgumentException("Nomor NPC hanya 1 atau 2");
    }

    @Override
    public boolean isRefillNpc(Entity entity) {
        return entity.getPersistentDataContainer().has(npcKey, PersistentDataType.STRING);
    }

    @Override
    public String arenaId(Entity entity) {
        return entity.getPersistentDataContainer().get(npcKey, PersistentDataType.STRING);
    }
}
