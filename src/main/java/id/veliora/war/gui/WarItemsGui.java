package id.veliora.war.gui;

import id.veliora.war.storage.PlayerDataStorage;
import id.veliora.war.util.ItemBuilder;
import id.veliora.war.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/** Persistent PvP purchases and upgrades. Values are intentionally data driven in gui.yml. */
public final class WarItemsGui {
    private final PlayerDataStorage data;
    public WarItemsGui(PlayerDataStorage data) { this.data = data; }

    public void open(Player player) {
        Inventory inv = menu("&8War Items");
        inv.setItem(11, item(Material.ENCHANTED_BOOK, "&d&lUPGRADE ENCHANT", "&7Tambah upgrade PvP milikmu", "&eKlik untuk melihat", "upgrades"));
        inv.setItem(13, item(Material.EMERALD, "&a&lITEM SHOP", "&7Beli item PvP memakai Vault", "&7Harga 1.000 sampai 20.000", "&eKlik untuk membuka", "shop"));
        inv.setItem(15, item(Material.CHEST, "&6&lITEM MILIKKU", "&7Lihat item dan armor yang kamu miliki", "&eKlik untuk melihat", "collection"));
        inv.setItem(22, item(Material.ARROW, "&fKembali", "", "main"));
        player.openInventory(inv);
    }

    public void upgrades(Player player) {
        Inventory inv = menu("&8War Items • Upgrade");
        inv.setItem(11, item(Material.DIAMOND_SWORD, "&bSharpness I", "&7Harga: &e1.000", "&7Status: " + owned(player, "sharpness_1"), "&eKlik untuk membeli", "buy:sharpness_1"));
        inv.setItem(13, item(Material.NETHERITE_SWORD, "&dSharpness II", "&7Harga: &e5.000", "&7Status: " + owned(player, "sharpness_2"), "&eKlik untuk membeli", "buy:sharpness_2"));
        inv.setItem(15, item(Material.MACE, "&6Density I", "&7Harga: &e10.000", "&7Status: " + owned(player, "density_1"), "&eKlik untuk membeli", "buy:density_1"));
        inv.setItem(22, item(Material.ARROW, "&fKembali", "", "items"));
        player.openInventory(inv);
    }

    public void shop(Player player) {
        Inventory inv = menu("&8War Items • Shop");
        String[][] products = {{"mace","MACE","Mace","10000"}, {"trident","TRIDENT","Trident","15000"}, {"elytra","ELYTRA","Elytra","20000"}, {"golden_apple","GOLDEN_APPLE","Golden Apple x8","3000"}, {"rocket","FIREWORK_ROCKET","Rocket x32","1000"}, {"blocks","OBSIDIAN","Obsidian x32","5000"}, {"potion","SPLASH_POTION","Potion Heal x4","4000"}, {"sword","NETHERITE_SWORD","Netherite Sword","20000"}};
        int[] slots = {10,11,12,13,14,15,16,19};
        for (int i=0;i<products.length;i++) {
            String[] p=products[i]; Material material=Material.matchMaterial(p[1]);
            inv.setItem(slots[i], item(material == null ? Material.BARRIER : material, "&a"+p[2], "&7Harga: &e"+p[3], "&7Status: "+owned(player,p[0]), "&eKlik untuk membeli", "buy:"+p[0]));
        }
        inv.setItem(22, item(Material.ARROW, "&fKembali", "", "items")); player.openInventory(inv);
    }

    public void collection(Player player) {
        Inventory inv = menu("&8War Items • Milikku");
        Material[] armour = {Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS};
        for (int i=0;i<armour.length;i++) inv.setItem(8 + i*9, item(armour[i], "&bArmor " + (i+1), "&7Armor war bawaan", "noop"));
        inv.setItem(16, item(Material.ELYTRA, "&bElytra", "&7"+owned(player,"elytra"), "noop"));
        String[] ids={"mace","trident","sword","golden_apple","rocket","blocks","potion"}; Material[] mats={Material.MACE,Material.TRIDENT,Material.NETHERITE_SWORD,Material.GOLDEN_APPLE,Material.FIREWORK_ROCKET,Material.OBSIDIAN,Material.SPLASH_POTION};
        for(int i=0;i<ids.length;i++) inv.setItem(27+i, item(mats[i], "&f"+ids[i], "&7"+owned(player,ids[i]), "noop"));
        inv.setItem(49, item(Material.ARROW, "&fKembali", "", "items")); player.openInventory(inv);
    }
    private Inventory menu(String title) { Inventory inv=Bukkit.createInventory(new GuiHolder(),54,TextUtil.component(title)); MainMenuGui.fill(inv,Material.GRAY_STAINED_GLASS_PANE); return inv; }
    private org.bukkit.inventory.ItemStack item(Material material,String name,String lore,String action){ return new ItemBuilder(material).name(name).lore(lore).action(action).hideFlags().build(); }
    private org.bukkit.inventory.ItemStack item(Material material,String name,String lore1,String lore2,String action){ return new ItemBuilder(material).name(name).lore(lore1,lore2).action(action).hideFlags().build(); }
    private org.bukkit.inventory.ItemStack item(Material material,String name,String lore1,String lore2,String lore3,String action){ return new ItemBuilder(material).name(name).lore(lore1,lore2,lore3).action(action).hideFlags().build(); }
    private String owned(Player p,String id){return data.intValue(p.getUniqueId(),"war-items."+id,0)>0?"&aDimiliki":"&cBelum dimiliki";}
}
