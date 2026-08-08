package id.veliora.war.gui;

import id.veliora.war.inventory.LoadoutManager;
import id.veliora.war.match.MatchManager;
import id.veliora.war.match.MatchMode;
import id.veliora.war.storage.ConfigManager;
import id.veliora.war.util.ItemBuilder;
import id.veliora.war.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class MainMenuGui {
    private final ConfigManager configs;
    private final LoadoutManager loadouts;
    private final MatchManager matches;

    public MainMenuGui(ConfigManager configs, LoadoutManager loadouts, MatchManager matches) {
        this.configs = configs;
        this.loadouts = loadouts;
        this.matches = matches;
    }

    public void open(Player player) {
        int size = configs.file("gui.yml").getInt("main.size", 27);
        Inventory inventory = Bukkit.createInventory(new GuiHolder(), size,
                TextUtil.component(configs.file("gui.yml").getString("main.title", "&8VelioraWar")));
        fill(inventory, Material.matchMaterial(configs.file("gui.yml").getString("main.filler", "BLACK_STAINED_GLASS_PANE")));
        inventory.setItem(configs.file("gui.yml").getInt("main.party-slot", 11),
                new ItemBuilder(Material.NETHER_STAR).name("&b&lBUAT PARTY")
                        .lore("&7Pilih mode, ukuran 1vs1 sampai 4vs4,", "&7lalu pilih Team Merah atau Biru.", "", "&eKlik untuk mulai memilih")
                        .action("party").hideFlags().build());
        inventory.setItem(configs.file("gui.yml").getInt("main.items-slot", 13),
                new ItemBuilder(Material.CHEST).name("&6&lWAR ITEMS")
                        .lore("&7Upgrade enchant, beli item PvP,", "&7dan lihat loadout milikmu.", "", "&eKlik untuk membuka")
                        .action("items").hideFlags().build());
        inventory.setItem(configs.file("gui.yml").getInt("main.guide-slot", 15),
                new ItemBuilder(Material.BOOK).name("&b&lPANDUAN").lore("&7Cara bermain dan aturan", "&eKlik untuk membuka")
                        .action("guide").build());
        if (matches.isPlaying(player.getUniqueId())) {
            inventory.setItem(configs.file("gui.yml").getInt("main.leave-slot", 26),
                    new ItemBuilder(Material.RED_BED).name("&c&lKELUAR").lore("&7Inventory asli akan dikembalikan")
                            .action("leave").build());
        }
        player.openInventory(inventory);
    }

    static void fill(Inventory inventory, Material material) {
        if (material == null) material = Material.BLACK_STAINED_GLASS_PANE;
        ItemStack filler = new ItemBuilder(material).name(" ").action("noop").build();
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler);
    }
}
