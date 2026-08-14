package id.veliora.war.storage;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.UUID;
import java.util.List;

public final class PlayerDataStorage {
    private final ConfigManager configs;

    public PlayerDataStorage(ConfigManager configs) {
        this.configs = configs;
    }

    private YamlConfiguration data() {
        return configs.file("playerdata.yml");
    }

    private String path(UUID uuid, String key) {
        return "players." + uuid + "." + key;
    }

    public void set(UUID uuid, String key, Object value) {
        data().set(path(uuid, key), value);
    }

    public String string(UUID uuid, String key) {
        return data().getString(path(uuid, key));
    }

    public List<String> stringList(UUID uuid, String key) {
        return data().getStringList(path(uuid, key));
    }

    public long longValue(UUID uuid, String key, long fallback) {
        return data().getLong(path(uuid, key), fallback);
    }

    public double doubleValue(UUID uuid, String key, double fallback) {
        return data().getDouble(path(uuid, key), fallback);
    }

    public int intValue(UUID uuid, String key, int fallback) {
        return data().getInt(path(uuid, key), fallback);
    }

    public void increment(UUID uuid, String key) {
        set(uuid, key, intValue(uuid, key, 0) + 1);
    }

    public void save() {
        configs.save("playerdata.yml");
    }
}
