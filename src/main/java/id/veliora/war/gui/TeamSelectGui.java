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
import org.bukkit.inventory.meta.SkullMeta;

public final class TeamSelectGui {
    private final ConfigManager configs;

    public TeamSelectGui(ConfigManager configs) {
        this.configs = configs;
    }

    public void open(Player player, MatchMode mode, MatchSize size) {
        int inventorySize = Math.max(54, configs.file("gui.yml").getInt("team.size", 54));
        String title = configs.file("gui.yml").getString("team.title", "&8Pilih Team")
                .replace("{mode}", mode.id()).replace("{size}", size.id());
        Inventory inventory = Bukkit.createInventory(new GuiHolder(), inventorySize, TextUtil.component(title));
        MainMenuGui.fill(inventory, Material.matchMaterial(configs.file("gui.yml").getString("team.filler", "BLACK_STAINED_GLASS_PANE")));
        inventory.setItem(configs.file("gui.yml").getInt("team.red-slot", 11),
                new ItemBuilder(Material.RED_WOOL).name("&c&lTEAM MERAH")
                        .lore("&7Maksimal " + size.playersPerTeam() + " pemain", "&eKlik untuk bergabung")
                        .action("team:" + mode.id() + ":" + size.id() + ":red").build());
        inventory.setItem(configs.file("gui.yml").getInt("team.blue-slot", 15),
                new ItemBuilder(Material.BLUE_WOOL).name("&9&lTEAM BIRU")
                        .lore("&7Maksimal " + size.playersPerTeam() + " pemain", "&eKlik untuk bergabung")
                        .action("team:" + mode.id() + ":" + size.id() + ":blue").build());
        // Visual slots: empty members are barriers. The selected player will join on click.
        for (int i = 0; i < size.playersPerTeam(); i++) {
            inventory.setItem(27 + i, new ItemBuilder(Material.BARRIER).name("&cSlot Merah kosong").lore("&7Belum ada player").action("noop").build());
            inventory.setItem(35 - i, new ItemBuilder(Material.BARRIER).name("&9Slot Biru kosong").lore("&7Belum ada player").action("noop").build());
        }
        player.openInventory(inventory);
    }
}
