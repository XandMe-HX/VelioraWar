package id.veliora.war.npc;

import id.veliora.war.VelioraWarPlugin;
import id.veliora.war.arena.Arena;
import id.veliora.war.arena.ArenaManager;
import id.veliora.war.storage.ConfigManager;
import id.veliora.war.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;

public final class RefillNpcManager implements NpcHook {
    private final ConfigManager configs;
    private final ArenaManager arenas;
    private final NamespacedKey npcKey;
    private final NamespacedKey hologramKey;

    public RefillNpcManager(VelioraWarPlugin plugin, ConfigManager configs, ArenaManager arenas) {
        this.configs = configs;
        this.arenas = arenas;
        this.npcKey = new NamespacedKey(plugin, "refill_npc");
        this.hologramKey = new NamespacedKey(plugin, "refill_hologram");
    }

    public void spawnAll() {
        Bukkit.getWorlds().forEach(world -> world.getEntities().stream()
                .filter(entity -> entity.getPersistentDataContainer().has(npcKey)
                        || entity.getPersistentDataContainer().has(hologramKey))
                .forEach(Entity::remove));
        arenas.all().stream().filter(arena -> arena.refillNpcLocation() != null).forEach(this::spawn);
    }

    public void set(Arena arena, Location location) {
        if (arena.refillNpcLocation() == null && configuredNpcCount() >= 2) {
            throw new IllegalStateException("Maksimal hanya 2 NPC refill yang dapat dibuat");
        }
        remove(arena.id());
        arena.refillNpcLocation(location);
        arenas.save();
        spawn(arena);
    }

    private long configuredNpcCount() {
        return arenas.all().stream().filter(current -> current.refillNpcLocation() != null).count();
    }

    public void spawn(Arena arena) {
        Location location = arena.refillNpcLocation();
        if (location == null || location.getWorld() == null) return;
        Villager npc = location.getWorld().spawn(location, Villager.class, villager -> {
            villager.setAI(false);
            villager.setInvulnerable(configs.config().getBoolean("npc.invulnerable", true));
            villager.setSilent(configs.config().getBoolean("npc.silent", true));
            villager.setCollidable(false);
            villager.setRemoveWhenFarAway(false);
            villager.setPersistent(true);
            villager.setProfession(Villager.Profession.WEAPONSMITH);
            villager.customName(TextUtil.component(configs.config().getString("npc.name", "&e&l[ REFIL ITEMS ]")));
            villager.setCustomNameVisible(true);
            villager.getPersistentDataContainer().set(npcKey, PersistentDataType.STRING, arena.id());
        });
        location.getWorld().spawn(location.clone().add(0, 2.2, 0), ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setMarker(true);
            stand.setInvulnerable(true);
            stand.setGravity(false);
            stand.customName(TextUtil.component(configs.config().getString("npc.hologram", "&b&lᴄʟɪᴄᴋ ᴛᴏ ʀᴇꜰɪʟ")));
            stand.setCustomNameVisible(true);
            stand.getPersistentDataContainer().set(hologramKey, PersistentDataType.STRING, arena.id());
        });
    }

    public void remove(String arenaId) {
        Bukkit.getWorlds().forEach(world -> world.getEntities().stream().filter(entity -> {
            String npc = entity.getPersistentDataContainer().get(npcKey, PersistentDataType.STRING);
            String hologram = entity.getPersistentDataContainer().get(hologramKey, PersistentDataType.STRING);
            return arenaId.equals(npc) || arenaId.equals(hologram);
        }).forEach(Entity::remove));
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
