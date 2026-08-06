package id.veliora.war.gui;

import id.veliora.war.arena.Arena;
import id.veliora.war.arena.ArenaFlag;
import id.veliora.war.storage.ConfigManager;
import id.veliora.war.util.ItemBuilder;
import id.veliora.war.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class FlagGui {
    private final ConfigManager configs;

    public FlagGui(ConfigManager configs) {
        this.configs = configs;
    }

    public void open(Player player, Arena arena) {
        int size = configs.file("gui.yml").getInt("flag.size", 54);
        String title = configs.file("gui.yml").getString("flag.title", "&8Flag Arena")
                .replace("{arena}", arena.id());
        Inventory inventory = Bukkit.createInventory(null, size, TextUtil.component(title));
        MainMenuGui.fill(inventory, Material.matchMaterial(configs.file("gui.yml").getString("flag.filler", "GRAY_STAINED_GLASS_PANE")));
        int slot = 10;
        for (ArenaFlag flag : ArenaFlag.values()) {
            if (slot % 9 == 8) slot += 2;
            boolean enabled = arena.flag(flag);
            inventory.setItem(slot++, new ItemBuilder(enabled ? Material.LIME_DYE : Material.GRAY_DYE)
                    .name((enabled ? "&a" : "&7") + flag.key())
                    .lore("&7Status: " + (enabled ? "&aON" : "&cOFF"), "&eKlik untuk mengubah")
                    .action("flag:" + arena.id() + ":" + flag.key()).build());
        }
        player.openInventory(inventory);
    }
}
