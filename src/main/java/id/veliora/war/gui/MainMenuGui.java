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
        int size = Math.max(45, configs.file("gui.yml").getInt("main.size", 45));
        Inventory inventory = Bukkit.createInventory(new GuiHolder(), size,
                TextUtil.component(configs.file("gui.yml").getString("main.title", "&#48D9FF&lVelioraWar &8• &fPilih Mode")));
        fill(inventory, Material.matchMaterial(configs.file("gui.yml").getString("main.filler", "BLACK_STAINED_GLASS_PANE")));
        border(inventory);

        putMode(inventory, 10, MatchMode.SWORD_DUEL, Material.NETHERITE_SWORD,
                "&#62E6FF&lSWORD DUEL", "&7Pertarungan klasik dengan pedang.", "&b1vs1 &8• &b2vs2 &8• &b3vs3 &8• &b4vs4");
        putMode(inventory, 12, MatchMode.MACE_PVP, Material.MACE,
                "&#C978FF&lMACE PVP", "&7Kuasai lompatan dan serangan berat.", "&d1vs1 &8• &d2vs2 &8• &d3vs3 &8• &d4vs4");
        putMode(inventory, 14, MatchMode.CPVP, Material.END_CRYSTAL,
                "&#FF5577&lCRYSTAL PVP", "&7Crystal, anchor, obsidian, dan refleks.", "&c1vs1 &8• &c2vs2 &8• &c3vs3 &8• &c4vs4");
        putMode(inventory, 16, MatchMode.ALL_MODE, Material.NETHER_STAR,
                "&#FFD45A&lALL MODE", "&7Semua perlengkapan dan NPC refill.", "&6Mode bebas dengan loadout lengkap");

        inventory.setItem(28, new ItemBuilder(Material.ENDER_CHEST).name("&#FFB347&lWAR ITEMS")
                .lore("&7Lihat koleksi dan upgrade PvP.", "", "&eKlik untuk membuka")
                .action("items").hideFlags().build());
        inventory.setItem(31, new ItemBuilder(Material.CLOCK).name("&#7BE7FF&lSTATUS")
                .lore("&7Hijau = siap dimainkan", "&7Merah = spawn belum lengkap", "&7Kuning = maintenance")
                .action("noop").build());
        inventory.setItem(34, new ItemBuilder(Material.WRITABLE_BOOK).name("&#8FFFA3&lPANDUAN")
                .lore("&7Cara bermain, aturan, dan keamanan.", "", "&eKlik untuk membaca")
                .action("guide").build());
        if (matches.isPlaying(player.getUniqueId())) {
            inventory.setItem(44, new ItemBuilder(Material.RED_BED).name("&c&lKELUAR DARI WAR")
                    .lore("&7Inventory asli akan dikembalikan.", "&eKlik untuk keluar")
                    .action("leave").build());
        }
        player.openInventory(inventory);
    }

    private void putMode(Inventory inventory, int slot, MatchMode mode, Material icon,
                         String name, String description, String sizes) {
        boolean available = matches.modeAvailable(mode);
        List<String> lore = List.of(description, sizes, "",
                matches.modeStatus(mode), matches.modePopulation(mode),
                available ? "&eKlik untuk memilih ukuran" : "&7Admin perlu mengatur spawn dan enable");
        inventory.setItem(slot, new ItemBuilder(available ? icon : Material.BARRIER)
                .name(name).lore(lore).action(available ? "mode:" + mode.id() : "noop").hideFlags().build());
    }

    private void border(Inventory inventory) {
        ItemStack cyan = new ItemBuilder(Material.CYAN_STAINED_GLASS_PANE).name(" ").action("noop").build();
        ItemStack blue = new ItemBuilder(Material.BLUE_STAINED_GLASS_PANE).name(" ").action("noop").build();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            int row = slot / 9;
            int column = slot % 9;
            if (row == 0 || row == inventory.getSize() / 9 - 1 || column == 0 || column == 8) {
                inventory.setItem(slot, (slot % 2 == 0 ? cyan : blue));
            }
        }
    }

    static void fill(Inventory inventory, Material material) {
        if (material == null) material = Material.BLACK_STAINED_GLASS_PANE;
        ItemStack filler = new ItemBuilder(material).name(" ").action("noop").build();
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler);
    }
}
