package id.veliora.war.command;

import id.veliora.war.VelioraWarPlugin;
import id.veliora.war.arena.Arena;
import id.veliora.war.arena.ArenaManager;
import id.veliora.war.arena.ArenaState;
import id.veliora.war.gui.FlagGui;
import id.veliora.war.gui.MainMenuGui;
import id.veliora.war.match.MatchMode;
import id.veliora.war.match.MatchSize;
import id.veliora.war.match.MatchTeam;
import id.veliora.war.npc.RefillNpcManager;
import id.veliora.war.storage.MessageManager;
import id.veliora.war.util.TextUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class VgWarCommand implements CommandExecutor, TabCompleter {
    private final VelioraWarPlugin plugin;
    private final ArenaManager arenas;
    private final id.veliora.war.storage.ConfigManager configs;
    private final MessageManager messages;
    private final MainMenuGui menu;
    private final FlagGui flagGui;
    private final RefillNpcManager npcs;
    private final MemberSubCommand member;
    private final AdminSubCommand admin = new AdminSubCommand();

    public VgWarCommand(VelioraWarPlugin plugin, ArenaManager arenas, id.veliora.war.storage.ConfigManager configs,
                        MessageManager messages, MainMenuGui menu, FlagGui flagGui, RefillNpcManager npcs) {
        this.plugin = plugin;
        this.arenas = arenas;
        this.configs = configs;
        this.messages = messages;
        this.menu = menu;
        this.flagGui = flagGui;
        this.npcs = npcs;
        this.member = new MemberSubCommand(menu);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("menu")) {
            if (!(sender instanceof Player player)) {
                messages.send(sender, "player-only");
                return true;
            }
            if (!player.hasPermission("veliorawar.use")) messages.send(player, "no-permission");
            else member.execute(player);
            return true;
        }
        if (!sender.hasPermission("veliorawar.admin")) {
            messages.send(sender, "no-permission");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("help")) {
            admin.sendHelp(sender);
            return true;
        }
        if (sub.equals("reload")) {
            plugin.reloadAll();
            messages.send(sender, "reload");
            return true;
        }
        if (sub.equals("list")) {
            sender.sendMessage(TextUtil.component("&bArena: &f" + String.join(", ", arenas.ids())));
            return true;
        }
        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only");
            return true;
        }
        try {
            // Friendly spelling kept for administrators: /vgwar set npc 1
            // The compact /vgwar setnpc 1 form remains supported as well.
            if (sub.equals("set") && args.length >= 2 && args[1].equalsIgnoreCase("npc")) {
                String[] npcArgs = {"setnpc", args.length >= 3 ? args[2] : ""};
                setNpc(player, npcArgs);
                return true;
            }
            switch (sub) {
                case "setwarp" -> {
                    configs.warp(player.getLocation());
                    messages.send(player, "warp-set");
                }
                case "pos1", "pos2" -> setPosition(player, sub.equals("pos1") ? 1 : 2);
                case "claim" -> claim(player, args);
                case "delete" -> delete(player, args);
                case "info" -> info(player, args);
                case "set" -> {
                    set(player, args);
                }
                case "spawn" -> setSpawnHere(player, args);
                case "reset" -> reset(player, args);
                case "flag" -> flag(player);
                case "enable", "disable" -> toggle(player, args, sub.equals("enable"));
                case "setnpc" -> setNpc(player, args);
                default -> admin.sendHelp(player);
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            player.sendMessage(TextUtil.component("&8[&bVelioraWar&8] &c" + exception.getMessage()));
        }
        return true;
    }

    private void setPosition(Player player, int number) {
        arenas.setPosition(player, number);
        Location location = player.getLocation();
        messages.send(player, "position-set", Map.of("position", "pos" + number,
                "x", Integer.toString(location.getBlockX()), "y", Integer.toString(location.getBlockY()),
                "z", Integer.toString(location.getBlockZ())));
    }

    private void claim(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(TextUtil.component("&8&m---------------- &b&lDaftar Arena &8&m----------------"));
            player.sendMessage(TextUtil.component("&f1. &bSword Duel"));
            player.sendMessage(TextUtil.component("&f2. &dMace PvP"));
            player.sendMessage(TextUtil.component("&f3. &cCrystal PvP"));
            player.sendMessage(TextUtil.component("&f4. &eAll Mode"));
            player.sendMessage(TextUtil.component("&7Set pos1 dan pos2 dulu, lalu pakai: &f/vgwar claim <1-4>"));
            return;
        }
        MatchMode mode = fixedMode(args[1]);
        Arena arena = arenas.create(player, args[1]);
        if (arena == null) messages.send(player, "arena-exists");
        else {
            // A single physical arena can host the 1v1–4v4 sizes selected by players.
            arena.mode(mode);
            arena.size(mode == MatchMode.ALL_MODE ? MatchSize.UNLIMITED : MatchSize.FOUR_VS_FOUR);
            arenas.save();
            messages.send(player, "arena-created", Map.of("arena", arenaLabel(arena.id())));
            player.sendMessage(TextUtil.component("&7Berdiri di dalam claim, lalu set spawn: &f/vgwar spawn merah &7dan &f/vgwar spawn biru"));
        }
    }

    private void delete(Player player, String[] args) {
        Arena arena = arena(args);
        if (arena.state() != ArenaState.WAITING && arena.state() != ArenaState.DISABLED)
            throw new IllegalStateException("Arena sedang dipakai dan tidak boleh dihapus");
        npcs.remove(arena.id());
        arenas.delete(arena.id());
        messages.send(player, "arena-deleted", Map.of("arena", arena.id()));
    }

    private void info(Player player, String[] args) {
        Arena arena = arena(args);
        player.sendMessage(TextUtil.component("&8&m---------- &b" + arena.id() + " &8&m----------"));
        player.sendMessage(TextUtil.component("&7Mode: &f" + (arena.mode() == null ? "belum diatur" : arena.mode().id())));
        player.sendMessage(TextUtil.component("&7Size: &f" + (arena.size() == null ? "belum diatur" : arena.size().id())));
        player.sendMessage(TextUtil.component("&7Status: &f" + arena.state() + " &7| Enabled: &f" + arena.enabled()));
        player.sendMessage(TextUtil.component("&7Spawn 1/merah: &f" + (arena.redSpawn() != null)));
        player.sendMessage(TextUtil.component("&7Spawn 2/hijau: &f" + (arena.greenSpawn() != null)));
        player.sendMessage(TextUtil.component("&7NPC refill: &f" + (arena.refillNpcLocation() != null)));
    }

    private void set(Player player, String[] args) {
        requireArgs(args, 3, "/vgwar set <arena> <1|2> atau /vgwar set <arena> <mode> <size>");
        Arena arena = arenas.get(args[1]).orElseThrow(() -> new IllegalArgumentException("Arena tidak ditemukan"));
        if (args.length == 3) {
            MatchTeam team = MatchTeam.from(args[2]).orElseThrow(() -> new IllegalArgumentException("Gunakan 1/merah atau 2/hijau"));
            if (!arena.region().contains(player.getLocation())) throw new IllegalStateException("Spawn harus berada di dalam region arena");
            if (team == MatchTeam.RED) arena.redSpawn(player.getLocation());
            else arena.greenSpawn(player.getLocation());
            arenas.save();
            messages.send(player, "spawn-set", Map.of("arena", arena.id(), "team", team.displayName()));
            return;
        }
        MatchMode mode = MatchMode.from(args[2]).orElseThrow(() -> new IllegalArgumentException("Mode tidak dikenal"));
        MatchSize size = mode == MatchMode.ALL_MODE ? MatchSize.UNLIMITED
                : MatchSize.from(args[3]).orElseThrow(() -> new IllegalArgumentException("Size tidak dikenal"));
        arena.mode(mode);
        arena.size(size);
        arena.enabled(false);
        arenas.save();
        messages.send(player, "arena-settings-set", Map.of("arena", arena.id(), "mode", mode.id(), "size", size.id()));
    }

    private void reset(Player player, String[] args) {
        Arena arena = arena(args);
        arena.redSpawn(null);
        arena.greenSpawn(null);
        arena.enabled(false);
        arenas.save();
        messages.send(player, "spawns-reset", Map.of("arena", arena.id()));
    }

    private void setSpawnHere(Player player, String[] args) {
        requireArgs(args, 2, "/vgwar spawn <merah|hijau>");
        Arena arena = arenaHere(player);
        MatchTeam team = MatchTeam.from(args[1]).orElseThrow(() -> new IllegalArgumentException("Gunakan merah atau hijau"));
        if (team == MatchTeam.RED) arena.redSpawn(player.getLocation()); else arena.greenSpawn(player.getLocation());
        arenas.save();
        messages.send(player, "spawn-set", Map.of("arena", arenaLabel(arena.id()), "team", team.displayName()));
    }

    private void flag(Player player) {
        flagGui.open(player, arenaHere(player));
    }

    private void toggle(Player player, String[] args, boolean enabled) {
        Arena arena = arena(args);
        if (enabled && !arena.isComplete()) {
            messages.send(player, "arena-incomplete");
            return;
        }
        if (!enabled && arena.state() != ArenaState.WAITING && arena.state() != ArenaState.DISABLED)
            throw new IllegalStateException("Arena sedang dipakai. Tunggu match selesai");
        arena.enabled(enabled);
        arenas.save();
        messages.send(player, enabled ? "arena-enabled" : "arena-disabled", Map.of("arena", arena.id()));
    }

    private void setNpc(Player player, String[] args) {
        requireArgs(args, 2, "/vgwar setnpc <1|2>");
        int slot;
        try { slot = Integer.parseInt(args[1]); } catch (NumberFormatException exception) { throw new IllegalArgumentException("Gunakan nomor NPC 1 atau 2"); }
        if (slot < 1 || slot > 2) throw new IllegalArgumentException("NPC refill hanya tersedia nomor 1 atau 2");
        npcs.set(slot, player.getLocation());
        player.sendMessage(TextUtil.component("&aNPC refill nomor &f" + slot + " &aberhasil dibuat di lokasimu."));
    }

    private Arena arenaHere(Player player) {
        return arenas.at(player.getLocation()).orElseThrow(() -> new IllegalStateException("Kamu harus berdiri di dalam claim arena terlebih dahulu"));
    }

    private MatchMode fixedMode(String id) {
        return switch (id) {
            case "1" -> MatchMode.SWORD_DUEL;
            case "2" -> MatchMode.MACE_PVP;
            case "3" -> MatchMode.CPVP;
            case "4" -> MatchMode.ALL_MODE;
            default -> throw new IllegalArgumentException("ID arena hanya 1 Sword, 2 Mace, 3 Crystal, atau 4 All Mode");
        };
    }

    private String arenaLabel(String id) {
        return switch (id) {
            case "1" -> "1 • Sword Duel";
            case "2" -> "2 • Mace PvP";
            case "3" -> "3 • Crystal PvP";
            case "4" -> "4 • All Mode";
            default -> id;
        };
    }

    private Arena arena(String[] args) {
        requireArgs(args, 2, "/vgwar " + args[0] + " <arena>");
        return arenas.get(args[1]).orElseThrow(() -> new IllegalArgumentException("Arena tidak ditemukan: " + args[1]));
    }

    private void requireArgs(String[] args, int minimum, String usage) {
        if (args.length < minimum) throw new IllegalArgumentException("Penggunaan: " + usage);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.add("menu");
            if (sender.hasPermission("veliorawar.admin")) suggestions.addAll(List.of("help", "setwarp", "pos1", "pos2", "claim", "spawn",
                    "delete", "list", "info", "set", "reset", "flag", "enable", "disable", "setnpc", "reload"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            suggestions.add("npc");
            suggestions.addAll(arenas.ids());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("set") && args[1].equalsIgnoreCase("npc")) {
            suggestions.addAll(List.of("1", "2"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("setnpc")) {
            suggestions.addAll(List.of("1", "2"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("claim")) {
            suggestions.addAll(List.of("1", "2", "3", "4"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            suggestions.addAll(List.of("merah", "hijau"));
        } else if (args.length == 2 && List.of("delete", "info", "reset", "enable", "disable").contains(args[0].toLowerCase())) {
            suggestions.addAll(arenas.ids());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            suggestions.addAll(Arrays.stream(MatchMode.values()).map(MatchMode::id).toList());
            suggestions.addAll(List.of("1", "2"));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("set")) {
            suggestions.addAll(List.of("1vs1", "2vs2", "3vs3", "4vs4"));
        }
        String input = args[args.length - 1].toLowerCase(Locale.ROOT);
        return suggestions.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(input)).distinct().toList();
    }
}
