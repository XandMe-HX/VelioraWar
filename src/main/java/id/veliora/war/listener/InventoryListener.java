package id.veliora.war.listener;

import id.veliora.war.arena.Arena;
import id.veliora.war.arena.ArenaFlag;
import id.veliora.war.arena.ArenaManager;
import id.veliora.war.cooldown.CooldownManager;
import id.veliora.war.gui.FlagGui;
import id.veliora.war.gui.GuideGui;
import id.veliora.war.gui.GuiHolder;
import id.veliora.war.gui.MainMenuGui;
import id.veliora.war.gui.ModeSelectGui;
import id.veliora.war.gui.RefillGui;
import id.veliora.war.gui.TeamSelectGui;
import id.veliora.war.gui.WarItemsGui;
import id.veliora.war.inventory.LoadoutManager;
import id.veliora.war.match.MatchManager;
import id.veliora.war.match.MatchMode;
import id.veliora.war.match.MatchSize;
import id.veliora.war.match.MatchTeam;
import id.veliora.war.storage.ConfigManager;
import id.veliora.war.storage.MessageManager;
import id.veliora.war.util.ItemBuilder;
import id.veliora.war.util.TextUtil;
import id.veliora.war.util.TimeUtil;
import id.veliora.war.util.VaultUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.Map;

public final class InventoryListener implements Listener {
    private final ConfigManager configs;
    private final ArenaManager arenas;
    private final MessageManager messages;
    private final MatchManager matches;
    private final LoadoutManager loadouts;
    private final CooldownManager cooldowns;
    private final MainMenuGui main;
    private final ModeSelectGui modes;
    private final TeamSelectGui teams;
    private final FlagGui flags;
    private final RefillGui refill;
    private final GuideGui guide;
    private final WarItemsGui warItems;
    private final id.veliora.war.storage.PlayerDataStorage playerData;

    public InventoryListener(ConfigManager configs, ArenaManager arenas, MessageManager messages,
                             MatchManager matches, LoadoutManager loadouts, CooldownManager cooldowns,
                             MainMenuGui main, ModeSelectGui modes, TeamSelectGui teams, FlagGui flags,
                             RefillGui refill, GuideGui guide, WarItemsGui warItems,
                             id.veliora.war.storage.PlayerDataStorage playerData) {
        this.configs = configs;
        this.arenas = arenas;
        this.messages = messages;
        this.matches = matches;
        this.loadouts = loadouts;
        this.cooldowns = cooldowns;
        this.main = main;
        this.modes = modes;
        this.teams = teams;
        this.flags = flags;
        this.refill = refill;
        this.guide = guide;
        this.warItems = warItems;
        this.playerData = playerData;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof GuiHolder)) return;

        // Always lock our top inventory. This also closes shift-click, number-key,
        // double-click, collect-to-cursor, hotbar-swap and empty-slot exploits.
        event.setCancelled(true);
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        String action = ItemBuilder.action(event.getCurrentItem());
        if (action == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        long guiCooldown = configs.config().getLong("cooldowns.gui-milliseconds", 250);
        if (!cooldowns.tryUse(player.getUniqueId(), "gui", guiCooldown)) return;

        String[] parts = action.split(":");
        switch (parts[0]) {
            case "party" -> modes.openParty(player);
            case "mode" -> MatchMode.from(parts[1]).ifPresent(mode -> modes.open(player, mode));
            case "size" -> {
                MatchMode mode = MatchMode.from(parts[1]).orElse(null);
                MatchSize size = MatchSize.from(parts[2]).orElse(null);
                if (mode != null && size != null) { player.closeInventory(); matches.joinAutomatic(player, mode, size); }
            }
            case "team" -> {
                player.closeInventory();
                MatchMode mode = MatchMode.from(parts[1]).orElse(null);
                MatchSize size = MatchSize.from(parts[2]).orElse(null);
                MatchTeam team = MatchTeam.from(parts[3]).orElse(null);
                if (mode != null && size != null && team != null) matches.joinTeam(player, mode, size, team);
            }
            case "all-join" -> {
                player.closeInventory();
                matches.joinAllMode(player);
            }
            case "all-cancel", "main" -> main.open(player);
            case "guide" -> sendGuide(player);
            case "items" -> warItems.open(player);
            case "upgrades" -> warItems.upgrades(player);
            case "shop" -> warItems.shop(player);
            case "collection" -> warItems.collection(player);
            case "buy" -> buy(player, parts.length > 1 ? parts[1] : "");
            case "leave" -> {
                player.closeInventory();
                if (matches.isAllMode(player.getUniqueId()) && matches.combatRemaining(player.getUniqueId()) > 0L) {
                    long seconds = (matches.combatRemaining(player.getUniqueId()) + 999L) / 1000L;
                    player.sendMessage(TextUtil.component("&8[&bVelioraWar&8] &cTunggu " + seconds + " detik setelah combat sebelum keluar."));
                } else {
                    matches.leave(player, true);
                }
            }
            case "flag" -> toggleFlag(player, parts);
            case "refill" -> org.bukkit.Bukkit.getScheduler().runTask(
                    org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()),
                    () -> { if (player.isOnline() && !player.isDead()) claimRefill(player, parts[1]); });
            default -> { }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof GuiHolder)) return;
        // A VelioraWar GUI never allows moving anything, including the player's bottom inventory.
        event.setCancelled(true);
    }

    private void toggleFlag(Player player, String[] parts) {
        if (!player.hasPermission("veliorawar.admin") || parts.length < 3) return;
        Arena arena = arenas.globalArena().orElse(null);
        ArenaFlag flag = ArenaFlag.from(parts[2]).orElse(null);
        if (arena == null || flag == null) return;
        arenas.setGlobalFlag(flag, !arenas.globalFlag(flag));
        flags.open(player, arenas.globalArena().orElse(arena));
    }

    private void claimRefill(Player player, String arenaId) {
        Arena arena = arenas.get(arenaId).orElse(null);
        if (arena == null || !matches.isInArena(player.getUniqueId(), arena) || arena.mode() != MatchMode.ALL_MODE) {
            player.closeInventory();
            return;
        }
        if (matches.isSuddenDeath(player.getUniqueId())
                && configs.config().getBoolean("match.sudden-death.disable-refill", true)) {
            player.sendMessage(TextUtil.component("&cRefill dinonaktifkan selama Sudden Death."));
            player.closeInventory();
            return;
        }
        String key = "refill:" + arena.id();
        long duration = configs.config().getLong("cooldowns.refill-seconds", 60) * 1000L;
        if (!cooldowns.tryUse(player.getUniqueId(), key, duration)) {
            messages.send(player, "refill-cooldown", Map.of("time", TimeUtil.formatMillis(cooldowns.remaining(player.getUniqueId(), key))));
            refill.open(player, arena);
            return;
        }
        player.closeInventory();
        loadouts.apply(player, MatchMode.ALL_MODE);
        messages.send(player, "refill-ready");
        org.bukkit.Bukkit.getScheduler().runTask(
                org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()),
                () -> { if (player.isOnline()) player.updateInventory(); });
    }

    private void sendGuide(Player player) {
        player.closeInventory();
        player.sendMessage(TextUtil.component("&8&m---------------- &b&lPanduan VelioraWar &8&m----------------"));
        player.sendMessage(TextUtil.component("&f1. &7Pilih &bBuat Party&7, lalu pilih mode dan ukuran team."));
        player.sendMessage(TextUtil.component("&f2. &7Team dibagi otomatis. Match mulai saat kedua team penuh."));
        player.sendMessage(TextUtil.component("&f3. &7Tunggu hitungan mundur; bergerak setelah tulisan GO."));
        player.sendMessage(TextUtil.component("&f4. &7Keluar: &f/vgwar leave&7. Duel aktif dihitung menyerah; All Mode menunggu combat selesai."));
    }

    private void buy(Player player, String id) {
        java.util.Map<String, Integer> prices = java.util.Map.ofEntries(
                java.util.Map.entry("sharpness_1", 1000), java.util.Map.entry("sharpness_2", 5000), java.util.Map.entry("density_1", 3500),
                java.util.Map.entry("mace", 5000), java.util.Map.entry("trident", 5000), java.util.Map.entry("elytra", 5000),
                java.util.Map.entry("golden_apple", 3000), java.util.Map.entry("rocket", 1000), java.util.Map.entry("blocks", 5000),
                java.util.Map.entry("potion", 4000), java.util.Map.entry("sword", 5000));
        Integer price = prices.get(id);
        if (price == null) return;
        if (playerData.intValue(player.getUniqueId(), "war-items." + id, 0) > 0) { player.sendMessage(TextUtil.component("&eItem ini sudah kamu miliki.")); return; }
        if (!VaultUtil.available()) { player.sendMessage(TextUtil.component("&cVault/Economy belum aktif di server.")); return; }
        if (!VaultUtil.withdraw(player, price)) { player.sendMessage(TextUtil.component("&cSaldo kamu tidak cukup. Harga: &e" + price)); return; }
        playerData.set(player.getUniqueId(), "war-items." + id, 1); playerData.save();
        player.sendMessage(TextUtil.component("&aBerhasil membeli &f" + id + " &aseharga &e" + price));
        warItems.open(player);
    }
}
