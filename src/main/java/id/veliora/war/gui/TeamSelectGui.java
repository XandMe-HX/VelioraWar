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
        int inventorySize = configs.file("gui.yml").getInt("team.size", 27);
        String title = configs.file("gui.yml").getString("team.title", "&8Pilih Team")
                .replace("{mode}", mode.id()).replace("{size}", size.id());
        Inventory inventory = Bukkit.createInventory(new GuiHolder(), inventorySize, TextUtil.component(title));
        MainMenuGui.fill(inventory, Material.matchMaterial(configs.file("gui.yml").getString("team.filler", "BLACK_STAINED_GLASS_PANE")));
        inventory.setItem(configs.file("gui.yml").getInt("team.red-slot", 11),
                new ItemBuilder(Material.RED_WOOL).name("&c&lTEAM MERAH")
                        .lore("&7Maksimal " + size.playersPerTeam() + " pemain", "&eKlik untuk bergabung")
                        .action("team:" + mode.id() + ":" + size.id() + ":red").build());
        inventory.setItem(configs.file("gui.yml").getInt("team.green-slot", 15),
                new ItemBuilder(Material.LIME_WOOL).name("&a&lTEAM HIJAU")
                        .lore("&7Maksimal " + size.playersPerTeam() + " pemain", "&eKlik untuk bergabung")
                        .action("team:" + mode.id() + ":" + size.id() + ":green").build());
        player.openInventory(inventory);
    }
}
