package id.veliora.war.inventory;

import id.veliora.war.match.MatchMode;
import id.veliora.war.storage.ConfigManager;
import id.veliora.war.storage.PlayerDataStorage;
import id.veliora.war.util.TextUtil;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class LoadoutManager {
    private static final Set<Material> PURCHASED_WAR_MATERIALS = Set.of(
            Material.MACE, Material.TRIDENT, Material.ELYTRA, Material.GOLDEN_APPLE,
            Material.FIREWORK_ROCKET, Material.OBSIDIAN, Material.SPLASH_POTION, Material.NETHERITE_SWORD);
    private final ConfigManager configs;
    private final PlayerDataStorage playerData;
    private final EnumMap<MatchMode, Set<Material>> allowed = new EnumMap<>(MatchMode.class);

    public LoadoutManager(ConfigManager configs, PlayerDataStorage playerData) {
        this.configs = configs;
        this.playerData = playerData;
        rebuildAllowedMaterials();
    }

    public void reload() {
        rebuildAllowedMaterials();
    }

    public void apply(Player player, MatchMode mode) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(null);
        ConfigurationSection items = configs.file("modes.yml").getConfigurationSection("modes." + mode.id() + ".items");
        if (items == null) return;
        for (String key : items.getKeys(false)) {
            ConfigurationSection section = items.getConfigurationSection(key);
            if (section == null) continue;
            ItemStack stack = createItem(section);
            place(player, stack, section.getString("slot", "0"));
        }
        applyPurchasedItems(player);
        if (configs.config().getBoolean("loadouts.fill-empty-slots-with-totems", true)) fillEmptySlotsWithTotems(player);
        player.updateInventory();
    }

    private void fillEmptySlotsWithTotems(Player player) {
        for (int slot = 0; slot < player.getInventory().getStorageContents().length; slot++) {
            ItemStack current = player.getInventory().getItem(slot);
            if (current == null || current.getType().isAir()) {
                player.getInventory().setItem(slot, new ItemStack(Material.TOTEM_OF_UNDYING));
            }
        }
    }

    private void applyPurchasedItems(Player player) {
        java.util.UUID uuid = player.getUniqueId();
        if (owned(uuid, "mace")) player.getInventory().setItem(20, new ItemStack(Material.MACE));
        if (owned(uuid, "trident")) player.getInventory().setItem(21, new ItemStack(Material.TRIDENT));
        if (owned(uuid, "elytra")) player.getInventory().setItem(22, new ItemStack(Material.ELYTRA));
        if (owned(uuid, "golden_apple")) player.getInventory().setItem(23, new ItemStack(Material.GOLDEN_APPLE, 8));
        if (owned(uuid, "rocket")) player.getInventory().setItem(24, new ItemStack(Material.FIREWORK_ROCKET, 32));
        if (owned(uuid, "blocks")) player.getInventory().setItem(25, new ItemStack(Material.OBSIDIAN, 32));
        if (owned(uuid, "potion")) player.getInventory().setItem(26, new ItemStack(Material.SPLASH_POTION, 4));
        if (owned(uuid, "sword")) player.getInventory().setItem(27, new ItemStack(Material.NETHERITE_SWORD));
        int sharpness = owned(uuid, "sharpness_2") ? 2 : owned(uuid, "sharpness_1") ? 1 : 0;
        int density = owned(uuid, "density_1") ? 1 : 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null) continue;
            ItemMeta meta = stack.getItemMeta();
            if (stack.getType() == Material.NETHERITE_SWORD && sharpness > 0) meta.addEnchant(Enchantment.SHARPNESS, 5 + sharpness, true);
            if (stack.getType() == Material.MACE && density > 0) meta.addEnchant(Enchantment.DENSITY, 5 + density, true);
            stack.setItemMeta(meta);
        }
    }

    private boolean owned(java.util.UUID uuid, String id) { return playerData.intValue(uuid, "war-items." + id, 0) > 0; }

    public boolean isAllowed(MatchMode mode, Material material) {
        return material == Material.AIR || allowed.getOrDefault(mode, Set.of()).contains(material);
    }

    public Material icon(MatchMode mode) {
        return Material.matchMaterial(configs.file("modes.yml").getString("modes." + mode.id() + ".icon", "BARRIER"));
    }

    public String displayName(MatchMode mode) {
        return configs.file("modes.yml").getString("modes." + mode.id() + ".display-name", mode.id());
    }

    public java.util.List<String> description(MatchMode mode) {
        return configs.file("modes.yml").getStringList("modes." + mode.id() + ".description");
    }

    private ItemStack createItem(ConfigurationSection section) {
        Material material = Material.matchMaterial(section.getString("material", "STONE"));
        if (material == null) material = Material.STONE;
        ItemStack stack = new ItemStack(material, Math.max(1, section.getInt("amount", 1)));
        ItemMeta meta = stack.getItemMeta();
        if (section.contains("display-name")) meta.displayName(TextUtil.component(section.getString("display-name")));
        if (!section.getStringList("lore").isEmpty()) {
            meta.lore(section.getStringList("lore").stream().map(TextUtil::component).toList());
        }
        meta.setUnbreakable(section.getBoolean("unbreakable", false));
        for (String raw : section.getStringList("enchantments")) {
            String[] parts = raw.split(":", 2);
            Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(parts[0].toLowerCase(Locale.ROOT)));
            if (enchantment != null) meta.addEnchant(enchantment, parts.length > 1 ? parseInt(parts[1], 1) : 1, true);
        }
        if (meta instanceof PotionMeta potionMeta) {
            String color = section.getString("potion-color");
            if (color != null) {
                try { potionMeta.setColor(Color.fromRGB(Integer.parseInt(color, 16))); } catch (NumberFormatException ignored) {}
            }
            for (String raw : section.getStringList("potion-effects")) {
                String[] parts = raw.split(":");
                PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase(Locale.ROOT));
                if (type != null) potionMeta.addCustomEffect(new PotionEffect(type,
                        parts.length > 1 ? parseInt(parts[1], 200) : 200,
                        parts.length > 2 ? parseInt(parts[2], 0) : 0), true);
            }
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private void place(Player player, ItemStack stack, String slot) {
        switch (slot.toUpperCase(Locale.ROOT)) {
            case "HELMET" -> player.getInventory().setHelmet(stack);
            case "CHEST", "CHESTPLATE" -> player.getInventory().setChestplate(stack);
            case "LEGS", "LEGGINGS" -> player.getInventory().setLeggings(stack);
            case "BOOTS" -> player.getInventory().setBoots(stack);
            case "OFFHAND" -> player.getInventory().setItemInOffHand(stack);
            default -> {
                int index = parseInt(slot, -1);
                if (index >= 0 && index < player.getInventory().getStorageContents().length) player.getInventory().setItem(index, stack);
                else player.getInventory().addItem(stack);
            }
        }
    }

    private void rebuildAllowedMaterials() {
        allowed.clear();
        for (MatchMode mode : MatchMode.values()) {
            Set<Material> materials = new HashSet<>();
            ConfigurationSection items = configs.file("modes.yml").getConfigurationSection("modes." + mode.id() + ".items");
            if (items != null) {
                for (String key : items.getKeys(false)) {
                    Material material = Material.matchMaterial(items.getString(key + ".material", "AIR"));
                    if (material != null) materials.add(material);
                }
            }
            // Upgrades bought through War Items are legitimate in every war mode.
            materials.addAll(PURCHASED_WAR_MATERIALS);
            allowed.put(mode, Set.copyOf(materials));
        }
    }

    private int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (NumberFormatException ignored) { return fallback; }
    }
}
