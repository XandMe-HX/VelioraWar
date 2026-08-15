package id.veliora.war.storage;

import id.veliora.war.VelioraWarPlugin;
import id.veliora.war.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
        plugin.getConfig().options().copyDefaults(true);
        // Detektor auto-totem lokal sudah dipindahkan dari desain; hapus sisa config lama.
        plugin.getConfig().set("anti-cheat.auto-totem", null);
        plugin.saveConfig();
        for (String name : new String[]{"arenas.yml", "modes.yml", "gui.yml", "messages.yml", "playerdata.yml"}) {
            File file = new File(plugin.getDataFolder(), name);
            if (!file.exists()) plugin.saveResource(name, false);
            YamlConfiguration loaded = YamlConfiguration.loadConfiguration(file);
            try (InputStream input = plugin.getResource(name)) {
                if (input != null) {
                    YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                            new InputStreamReader(input, StandardCharsets.UTF_8));
                    loaded.setDefaults(defaults);
                    loaded.options().copyDefaults(true);
                    loaded.save(file);
                }
            } catch (IOException exception) {
                plugin.getLogger().warning("Gagal menggabungkan default " + name + ": " + exception.getMessage());
            }
            custom.put(name, loaded);
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
        saveLocation("warp", location);
    }

    public Location stay() {
        Location location = LocationUtil.load(config().getConfigurationSection("stay"));
        return location == null ? warp() : location;
    }

    public void stay(Location location) {
        saveLocation("stay", location);
    }

    private void saveLocation(String path, Location location) {
        config().set(path, null);
        ConfigurationSection section = config().createSection(path);
        LocationUtil.save(section, location);
        plugin.saveConfig();
    }
}
