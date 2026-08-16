package id.veliora.war.gui;

import id.veliora.war.match.MatchMode;
import id.veliora.war.match.MatchSize;
import id.veliora.war.storage.ConfigManager;
import id.veliora.war.util.ItemBuilder;
import id.veliora.war.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class TeamSelectGui {
    private final ConfigManager configs;

    public TeamSelectGui(ConfigManager configs) {
        this.configs = configs;
    }

    public void open(Player player, MatchMode mode, MatchSize size) {
        int inventorySize = Math.max(45, configs.file("gui.yml").getInt("team.size", 45));
        String title = configs.file("gui.yml").getString("team.title", "&#48D9FF&lPilih Team &8• {size}")
                .replace("{mode}", mode.id()).replace("{size}", size.id().toUpperCase());
        Inventory inventory = Bukkit.createInventory(new GuiHolder(), inventorySize, TextUtil.component(title));
        MainMenuGui.fill(inventory, Material.BLACK_STAINED_GLASS_PANE);
        inventory.setItem(11, new ItemBuilder(Material.RED_BANNER).name("&#FF5A66&lTEAM MERAH")
                .lore("&7Kapasitas: &f" + size.playersPerTeam() + " pemain",
                        "&7Spawn: sisi Merah", "", "&eKlik untuk bergabung")
                .action("team:" + mode.id() + ":" + size.id() + ":red").hideFlags().build());
        inventory.setItem(15, new ItemBuilder(Material.BLUE_BANNER).name("&#5B8CFF&lTEAM BIRU")
                .lore("&7Kapasitas: &f" + size.playersPerTeam() + " pemain",
                        "&7Spawn: sisi Biru", "", "&eKlik untuk bergabung")
                .action("team:" + mode.id() + ":" + size.id() + ":blue").hideFlags().build());
        for (int i = 0; i < size.playersPerTeam(); i++) {
            inventory.setItem(27 + i, new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                    .name("&cSlot Merah " + (i + 1)).lore("&7Menunggu pemain").action("noop").build());
            inventory.setItem(35 - i, new ItemBuilder(Material.BLUE_STAINED_GLASS_PANE)
                    .name("&9Slot Biru " + (i + 1)).lore("&7Menunggu pemain").action("noop").build());
        }
        inventory.setItem(40, new ItemBuilder(Material.ARROW).name("&fKembali")
                .action("mode:" + mode.id()).build());
        player.openInventory(inventory);
    }
}
