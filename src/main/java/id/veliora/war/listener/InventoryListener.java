package id.veliora.war.listener;

import id.veliora.war.arena.Arena;
import id.veliora.war.arena.ArenaFlag;
import id.veliora.war.arena.ArenaManager;
import id.veliora.war.cooldown.CooldownManager;
import id.veliora.war.gui.FlagGui;
import id.veliora.war.gui.GuideGui;
import id.veliora.war.gui.MainMenuGui;
import id.veliora.war.gui.ModeSelectGui;
import id.veliora.war.gui.RefillGui;
import id.veliora.war.gui.TeamSelectGui;
import id.veliora.war.inventory.LoadoutManager;
import id.veliora.war.match.MatchManager;
import id.veliora.war.match.MatchMode;
import id.veliora.war.match.MatchSize;
import id.veliora.war.match.MatchTeam;
import id.veliora.war.storage.ConfigManager;
import id.veliora.war.storage.MessageManager;
import id.veliora.war.util.ItemBuilder;
import id.veliora.war.util.TimeUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

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

    public InventoryListener(ConfigManager configs, ArenaManager arenas, MessageManager messages,
                             MatchManager matches, LoadoutManager loadouts, CooldownManager cooldowns,
                             MainMenuGui main, ModeSelectGui modes, TeamSelectGui teams, FlagGui flags,
                             RefillGui refill, GuideGui guide) {
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
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        String action = ItemBuilder.action(event.getCurrentItem());
        if (action == null) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        long guiCooldown = configs.config().getLong("cooldowns.gui-milliseconds", 250);
        if (!cooldowns.tryUse(player.getUniqueId(), "gui", guiCooldown)) return;

        String[] parts = action.split(":");
        switch (parts[0]) {
            case "mode" -> MatchMode.from(parts[1]).ifPresent(mode -> modes.open(player, mode));
            case "size" -> {
                MatchMode mode = MatchMode.from(parts[1]).orElse(null);
                MatchSize size = MatchSize.from(parts[2]).orElse(null);
                if (mode != null && size != null) teams.open(player, mode, size);
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
            case "guide" -> guide.open(player);
            case "leave" -> {
                player.closeInventory();
                matches.leave(player, true);
            }
            case "flag" -> toggleFlag(player, parts);
            case "refill" -> claimRefill(player, parts[1]);
            default -> { }
        }
    }

    private void toggleFlag(Player player, String[] parts) {
        if (!player.hasPermission("veliorawar.admin") || parts.length < 3) return;
        Arena arena = arenas.get(parts[1]).orElse(null);
        ArenaFlag flag = ArenaFlag.from(parts[2]).orElse(null);
        if (arena == null || flag == null) return;
        arena.flag(flag, !arena.flag(flag));
        arenas.save();
        flags.open(player, arena);
    }

    private void claimRefill(Player player, String arenaId) {
        Arena arena = arenas.get(arenaId).orElse(null);
        if (arena == null || !matches.isInArena(player.getUniqueId(), arena) || arena.mode() != MatchMode.ALL_MODE) {
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
        loadouts.apply(player, MatchMode.ALL_MODE);
        messages.send(player, "refill-ready");
        refill.open(player, arena);
    }
}
