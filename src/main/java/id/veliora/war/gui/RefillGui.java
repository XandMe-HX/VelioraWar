package id.veliora.war.gui;

import id.veliora.war.arena.Arena;
import id.veliora.war.cooldown.CooldownManager;
import id.veliora.war.storage.ConfigManager;
import id.veliora.war.util.ItemBuilder;
import id.veliora.war.util.TextUtil;
import id.veliora.war.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class RefillGui {
    private final ConfigManager configs;
    private final CooldownManager cooldowns;

    public RefillGui(ConfigManager configs, CooldownManager cooldowns) {
        this.configs = configs;
        this.cooldowns = cooldowns;
    }

    public void open(Player player, Arena arena) {
        int size = configs.file("gui.yml").getInt("refill.size", 27);
        Inventory inventory = Bukkit.createInventory(null, size,
                TextUtil.component(configs.file("gui.yml").getString("refill.title", "&8Refill Items")));
        MainMenuGui.fill(inventory, Material.matchMaterial(configs.file("gui.yml").getString("refill.filler", "BLACK_STAINED_GLASS_PANE")));
        String key = "refill:" + arena.id();
        long remaining = cooldowns.remaining(player.getUniqueId(), key);
        int slot = configs.file("gui.yml").getInt("refill.claim-slot", 13);
        if (remaining <= 0) {
            inventory.setItem(slot, new ItemBuilder(Material.LIME_CONCRETE).name("&a&lKLAIM REFILL")
                    .lore("&7Klik untuk mengisi ulang loadout", "&7Cooldown: &f" + configs.config().getInt("cooldowns.refill-seconds", 60) + " detik")
                    .action("refill:" + arena.id()).build());
        } else {
            inventory.setItem(slot, new ItemBuilder(Material.BARRIER).name("&c&lMASIH COOLDOWN")
                    .lore("&7Tunggu &f" + TimeUtil.formatMillis(remaining)).build());
        }
        player.openInventory(inventory);
    }
}
