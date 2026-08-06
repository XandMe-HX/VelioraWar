package id.veliora.war.gui;

import id.veliora.war.storage.ConfigManager;
import id.veliora.war.util.ItemBuilder;
import id.veliora.war.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class GuideGui {
    private final ConfigManager configs;

    public GuideGui(ConfigManager configs) {
        this.configs = configs;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(new GuiHolder(), configs.file("gui.yml").getInt("guide.size", 27),
                TextUtil.component(configs.file("gui.yml").getString("guide.title", "&8Panduan VelioraWar")));
        MainMenuGui.fill(inventory, Material.matchMaterial(configs.file("gui.yml").getString("guide.filler", "LIGHT_BLUE_STAINED_GLASS_PANE")));
        inventory.setItem(10, new ItemBuilder(Material.IRON_SWORD).name("&bDuel dan Team")
                .lore("&7Pilih mode, ukuran, lalu team.", "&7Match mulai saat kedua team penuh.").hideFlags().build());
        inventory.setItem(12, new ItemBuilder(Material.CLOCK).name("&eTimer")
                .lore("&7Jika waktu habis, total damage", "&7terbesar menjadi pemenang.").build());
        inventory.setItem(14, new ItemBuilder(Material.END_CRYSTAL).name("&cCPVP Aman")
                .lore("&7Ledakan tetap memberi damage,", "&7namun block map tidak hancur.").build());
        inventory.setItem(16, new ItemBuilder(Material.TOTEM_OF_UNDYING).name("&6Fair Play")
                .lore("&7Auto totem dan item dari luar", "&7arena diperiksa Cheat Guard.").build());
        inventory.setItem(22, new ItemBuilder(Material.ARROW).name("&fKembali").action("main").build());
        player.openInventory(inventory);
    }
}
