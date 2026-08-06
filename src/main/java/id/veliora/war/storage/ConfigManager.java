package id.veliora.war.storage;

import id.veliora.war.VelioraWarPlugin;
import id.veliora.war.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ConfigManager {
    private final VelioraWarPlugin plugin;
    private final Map<String, YamlConfiguration> custom = new LinkedHashMap<>();

    public ConfigManager(VelioraWarPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        for (String name : new String[]{"arenas.yml", "modes.yml", "gui.yml", "messages.yml", "playerdata.yml"}) {
            File file = new File(plugin.getDataFolder(), name);
            if (!file.exists()) plugin.saveResource(name, false);
            custom.put(name, YamlConfiguration.loadConfiguration(file));
        }
    }

    public void reload() {
        plugin.reloadConfig();
        custom.clear();
        load();
    }

    public FileConfiguration config() {
        return plugin.getConfig();
    }

    public YamlConfiguration file(String name) {
        YamlConfiguration yaml = custom.get(name);
        if (yaml == null) throw new IllegalArgumentException("Config tidak dikenal: " + name);
        return yaml;
    }

    public void save(String name) {
        try {
            file(name).save(new File(plugin.getDataFolder(), name));
        } catch (IOException exception) {
            plugin.getLogger().severe("Gagal menyimpan " + name + ": " + exception.getMessage());
        }
    }

    public Location warp() {
        return LocationUtil.load(config().getConfigurationSection("warp"));
    }

    public void warp(Location location) {
        config().set("warp", null);
        ConfigurationSection section = config().createSection("warp");
        LocationUtil.save(section, location);
        plugin.saveConfig();
    }
}
