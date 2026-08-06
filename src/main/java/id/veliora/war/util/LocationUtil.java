package id.veliora.war.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public final class LocationUtil {
    private LocationUtil() {}

    public static void save(ConfigurationSection section, Location location) {
        if (location == null || location.getWorld() == null) return;
        section.set("world", location.getWorld().getName());
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("yaw", location.getYaw());
        section.set("pitch", location.getPitch());
    }

    public static Location load(ConfigurationSection section) {
        if (section == null) return null;
        World world = Bukkit.getWorld(section.getString("world", ""));
        if (world == null) return null;
        return new Location(world,
                section.getDouble("x"), section.getDouble("y"), section.getDouble("z"),
                (float) section.getDouble("yaw"), (float) section.getDouble("pitch"));
    }
}
