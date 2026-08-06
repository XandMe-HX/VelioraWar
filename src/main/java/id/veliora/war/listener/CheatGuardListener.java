package id.veliora.war.listener;

import id.veliora.war.VelioraWarPlugin;
import id.veliora.war.arena.Arena;
import id.veliora.war.arena.ArenaFlag;
import id.veliora.war.inventory.LoadoutManager;
import id.veliora.war.match.MatchManager;
import id.veliora.war.match.MatchMode;
import id.veliora.war.storage.ConfigManager;
import id.veliora.war.storage.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CheatGuardListener implements Listener {
    private final VelioraWarPlugin plugin;
    private final ConfigManager configs;
    private final MessageManager messages;
    private final MatchManager matches;
    private final LoadoutManager loadouts;
    private final Map<UUID, Long> poppedAt = new HashMap<>();
    private final Map<UUID, Long> manualMoveAt = new HashMap<>();
    private final Map<UUID, Integer> strikes = new HashMap<>();
    private BukkitTask scanner;
    private int tick;

    public CheatGuardListener(VelioraWarPlugin plugin, ConfigManager configs, MessageManager messages,
                              MatchManager matches, LoadoutManager loadouts) {
        this.plugin = plugin;
        this.configs = configs;
        this.messages = messages;
        this.matches = matches;
        this.loadouts = loadouts;
    }

    public void start() {
        if (scanner != null) scanner.cancel();
        scanner = Bukkit.getScheduler().runTaskTimer(plugin, this::scan, 1L, 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTotemPop(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Arena arena = matches.arena(player.getUniqueId()).orElse(null);
        if (arena == null) return;
        if (!arena.flag(ArenaFlag.ALLOW_TOTEM)) {
            event.setCancelled(true);
            return;
        }
        if (!event.isCancelled() && configs.config().getBoolean("anti-cheat.enabled", true)
                && configs.config().getBoolean("anti-cheat.auto-totem.enabled", true)) {
            poppedAt.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onManualInventory(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && matches.isPlaying(player.getUniqueId())) {
            manualMoveAt.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (matches.isPlaying(event.getPlayer().getUniqueId())) {
            manualMoveAt.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        }
    }

    private void scan() {
        if (!configs.config().getBoolean("anti-cheat.enabled", true)) return;
        scanAutoTotem();
        tick++;
        int interval = Math.max(5, configs.config().getInt("settings.illegal-item-scan-ticks", 20));
        if (tick % interval == 0 && configs.config().getBoolean("anti-cheat.illegal-items.enabled", true)) scanItems();
    }

    private void scanAutoTotem() {
        long now = System.currentTimeMillis();
        long threshold = configs.config().getLong("anti-cheat.auto-totem.min-refill-milliseconds", 150);
        for (Map.Entry<UUID, Long> entry : new HashMap<>(poppedAt).entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !matches.isPlaying(entry.getKey()) || now - entry.getValue() > 2000) {
                poppedAt.remove(entry.getKey());
                continue;
            }
            if (player.getInventory().getItemInOffHand().getType() != Material.TOTEM_OF_UNDYING) continue;
            long manual = manualMoveAt.getOrDefault(entry.getKey(), 0L);
            if (now - entry.getValue() <= threshold && manual < entry.getValue()) addStrike(player);
            poppedAt.remove(entry.getKey());
        }
    }

    private void addStrike(Player player) {
        int current = strikes.merge(player.getUniqueId(), 1, Integer::sum);
        int maximum = configs.config().getInt("anti-cheat.auto-totem.max-strikes", 3);
        messages.send(player, "auto-totem-warning", Map.of("strikes", Integer.toString(current), "max", Integer.toString(maximum)));
        plugin.getLogger().warning("Cheat Guard mendeteksi auto-totem: " + player.getName() + " (" + current + '/' + maximum + ")");
        if (current >= maximum) {
            strikes.remove(player.getUniqueId());
            matches.kickForCheat(player);
        }
    }

    private void scanItems() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            MatchMode mode = matches.mode(player.getUniqueId()).orElse(null);
            Arena arena = matches.arena(player.getUniqueId()).orElse(null);
            if (mode == null || arena == null || !arena.flag(ArenaFlag.ANTI_ILLEGAL_ITEM)) continue;
            for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (stack == null || stack.getType() == Material.AIR || stack.getType() == Material.GLASS_BOTTLE) continue;
                if (!loadouts.isAllowed(mode, stack.getType())) {
                    if (configs.config().getBoolean("anti-cheat.illegal-items.remove", true)) player.getInventory().setItem(slot, null);
                    messages.send(player, "illegal-item", Map.of("item", stack.getType().name()));
                }
            }
        }
    }
}
