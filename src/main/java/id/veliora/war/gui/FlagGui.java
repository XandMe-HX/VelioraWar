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
                .replace("{arena}", "Land Global");
        Inventory inventory = Bukkit.createInventory(new GuiHolder(), size, TextUtil.component(title));
        MainMenuGui.fill(inventory, Material.matchMaterial(configs.file("gui.yml").getString("flag.filler", "GRAY_STAINED_GLASS_PANE")));
        int slot = 10;
        for (ArenaFlag flag : ArenaFlag.values()) {
            if (slot % 9 == 8) slot += 2;
            boolean enabled = arena.flag(flag);
            inventory.setItem(slot++, new ItemBuilder(material(flag, enabled))
                    .name((enabled ? "&a&l" : "&c&l") + display(flag))
                    .lore("&7Status: " + (enabled ? "&aON" : "&cOFF"), "&eKlik untuk mengubah")
                    .action("flag:global:" + flag.key()).build());
        }
        player.openInventory(inventory);
    }

    private Material material(ArenaFlag flag, boolean enabled) {
        if (!enabled) return Material.BARRIER;
        return switch (flag) {
            case PVP -> Material.DIAMOND_SWORD;
            case BLOCK_PLACE -> Material.BRICKS;
            case BLOCK_BREAK -> Material.NETHERITE_PICKAXE;
            case TEMPORARY_BLOCK -> Material.OBSIDIAN;
            case EXPLOSION_DAMAGE, EXPLOSION_BLOCK_DAMAGE -> Material.END_CRYSTAL;
            case VOID_TELEPORT -> Material.ENDER_PEARL;
            case FALL_DAMAGE -> Material.FEATHER;
            case ITEM_DROP -> Material.DROPPER;
            case KEEP_INVENTORY -> Material.CHEST;
            case ALLOW_TOTEM -> Material.TOTEM_OF_UNDYING;
            case ALLOW_ELYTRA -> Material.ELYTRA;
            case ALLOW_COMMAND -> Material.COMMAND_BLOCK;
            case ANTI_ILLEGAL_ITEM -> Material.SHIELD;
        };
    }

    private String display(ArenaFlag flag) {
        return switch (flag) {
            case PVP -> "PvP";
            case BLOCK_PLACE -> "Pasang Block";
            case BLOCK_BREAK -> "Hancurkan Block";
            case TEMPORARY_BLOCK -> "Block Sementara";
            case EXPLOSION_DAMAGE -> "Damage Ledakan";
            case EXPLOSION_BLOCK_DAMAGE -> "Ledakan Hancurkan Map";
            case VOID_TELEPORT -> "Void Teleport";
            case FALL_DAMAGE -> "Fall Damage";
            case ITEM_DROP -> "Buang Item";
            case KEEP_INVENTORY -> "Simpan Inventory";
            case ALLOW_TOTEM -> "Totem";
            case ALLOW_ELYTRA -> "Elytra";
            case ALLOW_COMMAND -> "Command Saat War";
            case ANTI_ILLEGAL_ITEM -> "Anti Item Ilegal";
        };
    }
}
