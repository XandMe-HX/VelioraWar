package id.veliora.war.inventory;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

public final class InventoryBackup {
    private final ItemStack[] storage;
    private final ItemStack[] armor;
    private final ItemStack offhand;
    private final int level;
    private final float exp;
    private final int food;
    private final double health;
    private final GameMode gameMode;
    private final List<PotionEffect> effects;

    public InventoryBackup(ItemStack[] storage, ItemStack[] armor, ItemStack offhand,
                           int level, float exp, int food, double health, GameMode gameMode,
                           Collection<PotionEffect> effects) {
        this.storage = cloneItems(storage);
        this.armor = cloneItems(armor);
        this.offhand = offhand == null ? null : offhand.clone();
        this.level = level;
        this.exp = exp;
        this.food = food;
        this.health = health;
        this.gameMode = gameMode;
        this.effects = new ArrayList<>(effects);
    }

    public static InventoryBackup capture(Player player) {
        return new InventoryBackup(player.getInventory().getStorageContents(), player.getInventory().getArmorContents(),
                player.getInventory().getItemInOffHand(), player.getLevel(), player.getExp(), player.getFoodLevel(),
                player.getHealth(), player.getGameMode(), player.getActivePotionEffects());
    }

    public ItemStack[] storage() { return cloneItems(storage); }
    public ItemStack[] armor() { return cloneItems(armor); }
    public ItemStack offhand() { return offhand == null ? null : offhand.clone(); }
    public int level() { return level; }
    public float exp() { return exp; }
    public int food() { return food; }
    public double health() { return health; }
    public GameMode gameMode() { return gameMode; }
    public List<PotionEffect> effects() { return List.copyOf(effects); }

    public static String encode(ItemStack[] items) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             BukkitObjectOutputStream output = new BukkitObjectOutputStream(bytes)) {
            output.writeInt(items.length);
            for (ItemStack item : items) output.writeObject(item);
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("Gagal menyimpan inventory", exception);
        }
    }

    public static ItemStack[] decode(String encoded) {
        if (encoded == null || encoded.isBlank()) return new ItemStack[0];
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(Base64.getDecoder().decode(encoded));
             BukkitObjectInputStream input = new BukkitObjectInputStream(bytes)) {
            int length = input.readInt();
            ItemStack[] result = new ItemStack[length];
            for (int index = 0; index < length; index++) result[index] = (ItemStack) input.readObject();
            return result;
        } catch (IOException | ClassNotFoundException exception) {
            throw new IllegalStateException("Gagal membaca backup inventory", exception);
        }
    }

    private static ItemStack[] cloneItems(ItemStack[] source) {
        ItemStack[] result = new ItemStack[source.length];
        for (int index = 0; index < source.length; index++) {
            result[index] = source[index] == null ? null : source[index].clone();
        }
        return result;
    }
}
