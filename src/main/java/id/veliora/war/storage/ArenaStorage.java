package id.veliora.war.storage;

import id.veliora.war.arena.Arena;
import id.veliora.war.arena.ArenaFlag;
import id.veliora.war.arena.ArenaRegion;
import id.veliora.war.match.MatchMode;
import id.veliora.war.match.MatchSize;
import id.veliora.war.util.LocationUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ArenaStorage {
    private final ConfigManager configs;

    public ArenaStorage(ConfigManager configs) {
        this.configs = configs;
    }

    public Map<String, Arena> loadAll() {
        Map<String, Arena> result = new LinkedHashMap<>();
        ConfigurationSection root = configs.file("arenas.yml").getConfigurationSection("arenas");
        if (root == null) return result;

        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            ConfigurationSection regionSection = section == null ? null : section.getConfigurationSection("region");
            if (section == null || regionSection == null) continue;
            String world = regionSection.getString("world");
            if (world == null) continue;
            ArenaRegion region = new ArenaRegion(world,
                    regionSection.getInt("min-x"), regionSection.getInt("min-y"), regionSection.getInt("min-z"),
                    regionSection.getInt("max-x"), regionSection.getInt("max-y"), regionSection.getInt("max-z"));
            Arena arena = new Arena(id, region);
            MatchMode.from(section.getString("mode")).ifPresent(arena::mode);
            MatchSize.from(section.getString("size")).ifPresent(arena::size);
            arena.redSpawn(LocationUtil.load(section.getConfigurationSection("spawns.red")));
            arena.greenSpawn(LocationUtil.load(section.getConfigurationSection("spawns.green")));
            arena.refillNpcLocation(LocationUtil.load(section.getConfigurationSection("refill-npc")));
            ConfigurationSection flags = section.getConfigurationSection("flags");
            if (flags != null) {
                for (ArenaFlag flag : ArenaFlag.values()) arena.flag(flag, flags.getBoolean(flag.key(), flag.defaultValue()));
            }
            arena.enabled(section.getBoolean("enabled", false) && arena.isComplete());
            result.put(arena.id(), arena);
        }
        return result;
    }

    public void saveAll(Iterable<Arena> arenas) {
        YamlConfiguration yaml = configs.file("arenas.yml");
        yaml.set("arenas", null);
        ConfigurationSection root = yaml.createSection("arenas");
        for (Arena arena : arenas) saveInto(root.createSection(arena.id()), arena);
        configs.save("arenas.yml");
    }

    private void saveInto(ConfigurationSection section, Arena arena) {
        section.set("enabled", arena.enabled());
        section.set("mode", arena.mode() == null ? null : arena.mode().id());
        section.set("size", arena.size() == null ? null : arena.size().id());

        ArenaRegion region = arena.region();
        ConfigurationSection regionSection = section.createSection("region");
        regionSection.set("world", region.world());
        regionSection.set("min-x", region.minX());
        regionSection.set("min-y", region.minY());
        regionSection.set("min-z", region.minZ());
        regionSection.set("max-x", region.maxX());
        regionSection.set("max-y", region.maxY());
        regionSection.set("max-z", region.maxZ());

        if (arena.redSpawn() != null) LocationUtil.save(section.createSection("spawns.red"), arena.redSpawn());
        if (arena.greenSpawn() != null) LocationUtil.save(section.createSection("spawns.green"), arena.greenSpawn());
        if (arena.refillNpcLocation() != null) LocationUtil.save(section.createSection("refill-npc"), arena.refillNpcLocation());

        ConfigurationSection flags = section.createSection("flags");
        for (ArenaFlag flag : ArenaFlag.values()) flags.set(flag.key(), arena.flag(flag));
    }
}
