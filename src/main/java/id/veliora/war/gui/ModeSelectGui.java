package id.veliora.war.gui;

import id.veliora.war.inventory.LoadoutManager;
import id.veliora.war.match.MatchMode;
import id.veliora.war.match.MatchSize;
import id.veliora.war.storage.ConfigManager;
import id.veliora.war.util.ItemBuilder;
import id.veliora.war.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class ModeSelectGui {
    private final ConfigManager configs;
    private final LoadoutManager loadouts;

    public ModeSelectGui(ConfigManager configs, LoadoutManager loadouts) {
        this.configs = configs;
        this.loadouts = loadouts;
    }

    public void open(Player player, MatchMode mode) {
        if (mode == MatchMode.ALL_MODE) {
            openAllModeConfirm(player);
            return;
        }
        int size = Math.max(27, configs.file("gui.yml").getInt("size.size", 27));
        String title = configs.file("gui.yml").getString("size.title", "&#48D9FF&lPilih Ukuran &8• {mode}")
                .replace("{mode}", loadouts.displayName(mode));
        Inventory inventory = Bukkit.createInventory(new GuiHolder(), size, TextUtil.component(title));
        MainMenuGui.fill(inventory, Material.GRAY_STAINED_GLASS_PANE);
        for (MatchSize matchSize : new MatchSize[]{MatchSize.ONE_VS_ONE, MatchSize.TWO_VS_TWO,
                MatchSize.THREE_VS_THREE, MatchSize.FOUR_VS_FOUR}) {
            Material icon = switch (matchSize) {
                case ONE_VS_ONE -> Material.IRON_SWORD;
                case TWO_VS_TWO -> Material.GOLDEN_SWORD;
                case THREE_VS_THREE -> Material.DIAMOND_SWORD;
                case FOUR_VS_FOUR -> Material.NETHERITE_SWORD;
                default -> Material.BARRIER;
            };
            int slot = configs.file("gui.yml").getInt("size.slots." + matchSize.id(), 10);
            inventory.setItem(slot, new ItemBuilder(icon)
                    .name("&#66E0FF&l" + matchSize.id().toUpperCase())
                    .lore("&7Butuh &f" + matchSize.playersPerTeam() + " pemain &7per team.",
                            "&7Match dimulai ketika kedua team penuh.", "", "&eKlik untuk memilih team")
                    .action("size:" + mode.id() + ":" + matchSize.id()).hideFlags().build());
        }
        inventory.setItem(22, new ItemBuilder(Material.ARROW).name("&fKembali ke mode")
                .action("main").build());
        player.openInventory(inventory);
    }

    public void openParty(Player player) {
        int size = Math.max(27, configs.file("gui.yml").getInt("party.size", 27));
        Inventory inventory = Bukkit.createInventory(new GuiHolder(), size,
                TextUtil.component("&#48D9FF&lVelioraWar &8• &fPilih Mode"));
        MainMenuGui.fill(inventory, Material.BLUE_STAINED_GLASS_PANE);
        int[] slots = {10, 12, 14, 16};
        int index = 0;
        for (MatchMode mode : MatchMode.values()) {
            Material icon = loadouts.icon(mode);
            if (icon == null) icon = Material.BARRIER;
            inventory.setItem(slots[index++], new ItemBuilder(icon).name(loadouts.displayName(mode))
                    .lore(loadouts.description(mode)).action("mode:" + mode.id()).hideFlags().build());
        }
        inventory.setItem(22, new ItemBuilder(Material.ARROW).name("&fKembali")
                .action("main").build());
        player.openInventory(inventory);
    }

    private void openAllModeConfirm(Player player) {
        int size = Math.max(27, configs.file("gui.yml").getInt("all-mode-confirm.size", 27));
        Inventory inventory = Bukkit.createInventory(new GuiHolder(), size,
                TextUtil.component(configs.file("gui.yml").getString("all-mode-confirm.title", "&#FFD45A&lMasuk All Mode?")));
        MainMenuGui.fill(inventory, Material.BLACK_STAINED_GLASS_PANE);
        inventory.setItem(11, new ItemBuilder(Material.RED_CONCRETE).name("&c&lBATAL")
                .lore("&7Kembali ke pilihan mode").action("all-cancel").build());
        inventory.setItem(13, new ItemBuilder(Material.NETHER_STAR).name("&#FFD45A&lALL MODE")
                .lore("&7Loadout lengkap", "&7NPC refill tersedia", "&7Inventory asli tetap diamankan")
                .action("noop").hideFlags().build());
        inventory.setItem(15, new ItemBuilder(Material.LIME_CONCRETE).name("&a&lIKUT MAIN")
                .lore("&7Teleport ke arena All Mode.", "", "&eKlik untuk masuk")
                .action("all-join").build());
        player.openInventory(inventory);
    }
}
