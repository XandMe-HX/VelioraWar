package id.veliora.war.inventory;

import id.veliora.war.storage.PlayerDataStorage;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public final class InventoryManager {
    private final PlayerDataStorage playerData;
    private final Map<UUID, InventoryBackup> backups = new HashMap<>();

    public InventoryManager(PlayerDataStorage playerData) {
        this.playerData = playerData;
    }

    public boolean hasBackup(UUID uuid) {
        return backups.containsKey(uuid) || playerData.string(uuid, "backup.storage") != null;
    }

    public boolean backup(Player player) {
        if (hasBackup(player.getUniqueId())) return false;
        InventoryBackup backup = InventoryBackup.capture(player);
        backups.put(player.getUniqueId(), backup);
        persist(player.getUniqueId(), backup);
        clearForWar(player);
        return true;
    }

    public void clearForWar(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(null);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        player.setFireTicks(0);
        player.setFallDistance(0);
        player.setLevel(0);
        player.setExp(0);
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.setGameMode(GameMode.SURVIVAL);
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH) == null
                ? 20.0 : player.getAttribute(Attribute.MAX_HEALTH).getValue();
        player.setHealth(Math.min(maxHealth, 20.0));
    }

    public boolean restore(Player player, Location destination) {
        UUID uuid = player.getUniqueId();
        InventoryBackup backup = backups.remove(uuid);
        if (backup == null) backup = load(uuid);
        if (backup == null) return false;

        player.getInventory().clear();
        player.getInventory().setStorageContents(normalizeStorage(backup.storage()));
        player.getInventory().setArmorContents(backup.armor());
        player.getInventory().setItemInOffHand(backup.offhand());
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        for (PotionEffect effect : backup.effects()) player.addPotionEffect(effect);
        player.setLevel(backup.level());
        player.setExp(backup.exp());
        player.setFoodLevel(Math.max(0, Math.min(20, backup.food())));
        player.setGameMode(backup.gameMode());
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH) == null
                ? 20.0 : player.getAttribute(Attribute.MAX_HEALTH).getValue();
        player.setHealth(Math.max(1.0, Math.min(maxHealth, backup.health())));
        if (destination != null) player.teleport(destination);
        clearPersisted(uuid);
        return true;
    }

    public void restorePending(Player player, Location destination) {
        if (hasBackup(player.getUniqueId())) restore(player, destination);
    }

    private void persist(UUID uuid, InventoryBackup backup) {
        playerData.set(uuid, "backup.storage", InventoryBackup.encode(backup.storage()));
        playerData.set(uuid, "backup.armor", InventoryBackup.encode(backup.armor()));
        playerData.set(uuid, "backup.offhand", InventoryBackup.encode(new ItemStack[]{backup.offhand()}));
        playerData.set(uuid, "backup.level", backup.level());
        playerData.set(uuid, "backup.exp", backup.exp());
        playerData.set(uuid, "backup.food", backup.food());
        playerData.set(uuid, "backup.health", backup.health());
        playerData.set(uuid, "backup.game-mode", backup.gameMode().name());
        playerData.set(uuid, "backup.effects", encodeEffects(backup.effects()));
        playerData.save();
    }

    private InventoryBackup load(UUID uuid) {
        String storage = playerData.string(uuid, "backup.storage");
        if (storage == null) return null;
        ItemStack[] offhand = InventoryBackup.decode(playerData.string(uuid, "backup.offhand"));
        GameMode mode;
        try {
            mode = GameMode.valueOf(playerData.string(uuid, "backup.game-mode"));
        } catch (Exception ignored) {
            mode = GameMode.SURVIVAL;
        }
        return new InventoryBackup(InventoryBackup.decode(storage),
                InventoryBackup.decode(playerData.string(uuid, "backup.armor")),
                offhand.length == 0 ? null : offhand[0],
                playerData.intValue(uuid, "backup.level", 0),
                (float) playerData.doubleValue(uuid, "backup.exp", 0),
                playerData.intValue(uuid, "backup.food", 20),
                playerData.doubleValue(uuid, "backup.health", 20), mode,
                decodeEffects(playerData.stringList(uuid, "backup.effects")));
    }

    private List<String> encodeEffects(List<PotionEffect> effects) {
        List<String> result = new ArrayList<>();
        for (PotionEffect effect : effects) {
            result.add(effect.getType().getKey().getKey() + ":" + effect.getDuration() + ":" + effect.getAmplifier()
                    + ":" + effect.isAmbient() + ":" + effect.hasParticles() + ":" + effect.hasIcon());
        }
        return result;
    }

    private List<PotionEffect> decodeEffects(List<String> values) {
        List<PotionEffect> result = new ArrayList<>();
        for (String value : values) {
            String[] parts = value.split(":");
            if (parts.length < 3) continue;
            PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase(java.util.Locale.ROOT));
            if (type == null) continue;
            try {
                int duration = Integer.parseInt(parts[1]);
                int amplifier = Integer.parseInt(parts[2]);
                boolean ambient = parts.length > 3 && Boolean.parseBoolean(parts[3]);
                boolean particles = parts.length <= 4 || Boolean.parseBoolean(parts[4]);
                boolean icon = parts.length <= 5 || Boolean.parseBoolean(parts[5]);
                result.add(new PotionEffect(type, Math.max(1, duration), Math.max(0, amplifier), ambient, particles, icon));
            } catch (NumberFormatException ignored) { }
        }
        return result;
    }

    private ItemStack[] normalizeStorage(ItemStack[] items) {
        ItemStack[] result = new ItemStack[36];
        System.arraycopy(items, 0, result, 0, Math.min(items.length, result.length));
        return result;
    }

    private void clearPersisted(UUID uuid) {
        playerData.set(uuid, "backup", null);
        playerData.save();
    }
}
