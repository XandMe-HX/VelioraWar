package id.veliora.war.cooldown;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class CooldownManager {
    private final Map<String, Long> cooldowns = new HashMap<>();

    public boolean ready(UUID uuid, String type) {
        return remaining(uuid, type) <= 0;
    }

    public long remaining(UUID uuid, String type) {
        return Math.max(0, cooldowns.getOrDefault(key(uuid, type), 0L) - System.currentTimeMillis());
    }

    public void set(UUID uuid, String type, long durationMillis) {
        cooldowns.put(key(uuid, type), System.currentTimeMillis() + Math.max(0, durationMillis));
    }

    public boolean tryUse(UUID uuid, String type, long durationMillis) {
        if (!ready(uuid, type)) return false;
        set(uuid, type, durationMillis);
        return true;
    }

    public void cleanup() {
        long now = System.currentTimeMillis();
        Iterator<Long> iterator = cooldowns.values().iterator();
        while (iterator.hasNext()) if (iterator.next() <= now) iterator.remove();
    }

    private String key(UUID uuid, String type) {
        return uuid + ":" + type.toLowerCase();
    }
}
