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
        int size = configs.file("gui.yml").getInt("size.size", 27);
        String title = configs.file("gui.yml").getString("size.title", "&8Pilih Ukuran")
                .replace("{mode}", loadouts.displayName(mode));
        Inventory inventory = Bukkit.createInventory(new GuiHolder(), size, TextUtil.component(title));
        MainMenuGui.fill(inventory, Material.matchMaterial(configs.file("gui.yml").getString("size.filler", "GRAY_STAINED_GLASS_PANE")));
        for (MatchSize matchSize : new MatchSize[]{MatchSize.ONE_VS_ONE, MatchSize.TWO_VS_TWO,
                MatchSize.THREE_VS_THREE, MatchSize.FOUR_VS_FOUR}) {
            Material icon = switch (matchSize) {
                case ONE_VS_ONE -> Material.IRON_SWORD;
                case TWO_VS_TWO -> Material.GOLDEN_SWORD;
                case THREE_VS_THREE -> Material.DIAMOND_SWORD;
                case FOUR_VS_FOUR -> Material.NETHERITE_SWORD;
                default -> Material.BARRIER;
            };
            inventory.setItem(configs.file("gui.yml").getInt("size.slots." + matchSize.id(), 10),
                    new ItemBuilder(icon).name("&b&l" + matchSize.id().toUpperCase())
                            .lore("&7" + matchSize.playersPerTeam() + " pemain per team", "&eKlik untuk memilih team")
                            .action("size:" + mode.id() + ":" + matchSize.id()).hideFlags().build());
        }
        player.openInventory(inventory);
    }

    public void openParty(Player player) {
        int size = configs.file("gui.yml").getInt("party.size", 27);
        Inventory inventory = Bukkit.createInventory(new GuiHolder(), size, TextUtil.component("&8Buat Party • Pilih Mode"));
        MainMenuGui.fill(inventory, Material.BLUE_STAINED_GLASS_PANE);
        int[] slots = {10, 12, 14, 16};
        int index = 0;
        for (MatchMode mode : MatchMode.values()) {
            Material icon = loadouts.icon(mode);
            if (icon == null) icon = Material.BARRIER;
            inventory.setItem(slots[index++], new ItemBuilder(icon).name(loadouts.displayName(mode))
                    .lore(loadouts.description(mode)).action("mode:" + mode.id()).hideFlags().build());
        }
        inventory.setItem(22, new ItemBuilder(Material.ARROW).name("&fKembali").action("main").build());
        player.openInventory(inventory);
    }

    private void openAllModeConfirm(Player player) {
        int size = configs.file("gui.yml").getInt("all-mode-confirm.size", 27);
        Inventory inventory = Bukkit.createInventory(new GuiHolder(), size,
                TextUtil.component(configs.file("gui.yml").getString("all-mode-confirm.title", "&8Masuk All Mode?")));
        MainMenuGui.fill(inventory, Material.matchMaterial(configs.file("gui.yml").getString("all-mode-confirm.filler", "BLACK_STAINED_GLASS_PANE")));
        inventory.setItem(configs.file("gui.yml").getInt("all-mode-confirm.cancel-slot", 11),
                new ItemBuilder(Material.RED_CONCRETE).name("&c&lBATAL").action("all-cancel").build());
        inventory.setItem(configs.file("gui.yml").getInt("all-mode-confirm.join-slot", 15),
                new ItemBuilder(Material.LIME_CONCRETE).name("&a&lIKUT MAIN")
                        .lore("&7Klik untuk loading dan teleport", "&7ke arena All Mode")
                        .action("all-join").build());
        player.openInventory(inventory);
    }
}
