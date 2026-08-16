package id.veliora.war.arena;

import org.bukkit.Location;

public final class ArenaRegion {
    private final String world;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    public ArenaRegion(Location first, Location second) {
        if (first.getWorld() == null || second.getWorld() == null || !first.getWorld().equals(second.getWorld())) {
            throw new IllegalArgumentException("Kedua posisi arena harus berada di world yang sama");
        }
        this.world = first.getWorld().getName();
        this.minX = Math.min(first.getBlockX(), second.getBlockX());
        this.minY = first.getWorld().getMinHeight();
        this.minZ = Math.min(first.getBlockZ(), second.getBlockZ());
        this.maxX = Math.max(first.getBlockX(), second.getBlockX());
        this.maxY = first.getWorld().getMaxHeight() - 1;
        this.maxZ = Math.max(first.getBlockZ(), second.getBlockZ());
    }

    public ArenaRegion(String world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.world = world;
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
    }

    public boolean contains(Location location) {
        return location.getWorld() != null
                && location.getWorld().getName().equals(world)
                && location.getX() >= minX && location.getX() < maxX + 1
                && location.getZ() >= minZ && location.getZ() < maxZ + 1;
    }

    public boolean intersects(ArenaRegion other) {
        return world.equals(other.world)
                && minX <= other.maxX && maxX >= other.minX
                && minZ <= other.maxZ && maxZ >= other.minZ;
    }

    public String world() { return world; }
    public int minX() { return minX; }
    public int minY() { return minY; }
    public int minZ() { return minZ; }
    public int maxX() { return maxX; }
    public int maxY() { return maxY; }
    public int maxZ() { return maxZ; }
}
