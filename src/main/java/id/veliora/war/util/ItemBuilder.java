package id.veliora.war.util;

import id.veliora.war.VelioraWarPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;

public final class ItemBuilder {
    public static final NamespacedKey ACTION_KEY = new NamespacedKey(VelioraWarPlugin.getInstance(), "gui_action");
    private final ItemStack item;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, amount));
        return this;
    }

    public ItemBuilder name(String name) {
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TextUtil.component(name));
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder lore(String... lore) {
        return lore(Arrays.asList(lore));
    }

    public ItemBuilder lore(List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        meta.lore(lore.stream().map(TextUtil::component).toList());
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder action(String action) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder hideFlags() {
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return this;
    }

    public ItemStack build() {
        return item.clone();
    }

    public static String action(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        return stack.getItemMeta().getPersistentDataContainer().get(ACTION_KEY, PersistentDataType.STRING);
    }
}
