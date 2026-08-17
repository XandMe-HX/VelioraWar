package id.veliora.war.command;

import id.veliora.war.VelioraWarPlugin;
import id.veliora.war.arena.Arena;
import id.veliora.war.arena.ArenaFlag;
import id.veliora.war.gui.FlagGui;
import id.veliora.war.gui.MainMenuGui;
import id.veliora.war.match.MatchMode;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class VgWarCommand implements CommandExecutor, TabCompleter {
    private final VelioraWarPlugin plugin;
    private final id.veliora.war.arena.ArenaManager arenas;
    private final id.veliora.war.storage.ConfigManager configs;
    private final MessageManager messages;
    private final MainMenuGui menu;
    private final FlagGui flagGui;
    private final RefillNpcManager npcs;
    private final MemberSubCommand member;
    private final AdminSubCommand admin = new AdminSubCommand();

    public VgWarCommand(VelioraWarPlugin plugin, id.veliora.war.arena.ArenaManager arenas,
                        id.veliora.war.storage.ConfigManager configs, MessageManager messages,
                        MainMenuGui menu, FlagGui flagGui, RefillNpcManager npcs) {
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
        if (sender instanceof Player player && handleMemberCommand(player, args)) return true;
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
        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only");
            return true;
        }
        try {
            switch (sub) {
                case "pos1", "pos2" -> setPosition(player, sub.equals("pos1") ? 1 : 2);
                case "claim" -> claim(player);
                case "redefine" -> redefine(player, args);
                case "spawn" -> setSpawn(player, args);
                case "set" -> set(player, args);
                case "enable" -> enable(player);
                case "disable" -> disable(player);
                case "info", "list" -> info(player, args);
                case "flag" -> flag(player, args);
                case "npc" -> npc(player, args);
                case "delete" -> delete(player, args);
                case "setnpc" -> legacyNpc(player, args);
                default -> admin.sendHelp(player);
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            player.sendMessage(TextUtil.component("&8[&bVelioraWar&8] &c" + exception.getMessage()));
        }
        return true;
    }

    private boolean handleMemberCommand(Player player, String[] args) {
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("leave")) {
            if (matches.isAllMode(player.getUniqueId())) {
                long remaining = matches.combatRemaining(player.getUniqueId());
                if (remaining > 0L) player.sendMessage(TextUtil.component("&8[&bVelioraWar&8] &cTunggu " + ((remaining + 999L) / 1000L) + " detik setelah combat sebelum keluar."));
                else matches.leaveAllMode(player);
            } else player.sendMessage(TextUtil.component("&8[&bVelioraWar&8] &c/ vgwar leave hanya untuk All Mode."));
            return true;
        }
        if (sub.equals("duel")) {
            if (args.length < 3) { player.sendMessage(TextUtil.component("&cPenggunaan: /vgwar duel <nama> <sword|mace|cpvp>")); return true; }
            Player target = plugin.getServer().getPlayerExact(args[1]);
            MatchMode mode = MatchMode.from(args[2]).orElse(null);
            if (target == null || mode == null || mode == MatchMode.ALL_MODE || !matches.requestDuel(player, target, mode)) player.sendMessage(TextUtil.component("&cDuel tidak dapat dibuat."));
            else { player.sendMessage(TextUtil.component("&aTantangan dikirim ke &f" + target.getName())); target.sendMessage(TextUtil.component("&8[&bVelioraWar&8] &e" + player.getName() + " &fmenantangmu &b" + display(mode) + "&f. &a/vgwar accept &7atau &c/vgwar deny")); }
            return true;
        }
        if (sub.equals("accept")) { if (matches.acceptDuel(player).isEmpty()) player.sendMessage(TextUtil.component("&cTidak ada tantangan duel yang aktif.")); return true; }
        if (sub.equals("deny")) { matches.denyDuel(player).ifPresentOrElse(uuid -> { Player sender = plugin.getServer().getPlayer(uuid); if (sender != null) sender.sendMessage(TextUtil.component("&cTantangan duel ditolak.")); }, () -> player.sendMessage(TextUtil.component("&cTidak ada tantangan duel yang aktif."))); return true; }
        if (sub.equals("cancel")) { matches.cancelDuel(player).ifPresentOrElse(uuid -> { Player target = plugin.getServer().getPlayer(uuid); if (target != null) target.sendMessage(TextUtil.component("&eTantangan duel dibatalkan.")); player.sendMessage(TextUtil.component("&eTantangan dibatalkan.")); }, () -> player.sendMessage(TextUtil.component("&cTidak ada tantangan duel yang aktif."))); return true; }
        return false;
    }

    private void setPosition(Player player, int number) {
        arenas.setPosition(player, number);
        Location location = player.getLocation();
        messages.send(player, "position-set", Map.of("position", "pos" + number,
                "x", Integer.toString(location.getBlockX()), "y", Integer.toString(location.getBlockY()),
                "z", Integer.toString(location.getBlockZ())));
    }

    private void claim(Player player) {
        arenas.claimGlobal(player);
        configs.config().set("settings.enabled", false);
        plugin.saveConfig();
        player.sendMessage(TextUtil.component("&8[&bVelioraWar&8] &aSatu land global berhasil disimpan, penuh dari bawah sampai atas."));
        player.sendMessage(TextUtil.component("&7Claim selesai. Sekarang cukup atur spawn Sword, Mace, dan CPVP."));
    }

    private void redefine(Player player, String[] args) {
        requireArgs(args, 2, "/vgwar redefine confirm");
        if (!args[1].equalsIgnoreCase("confirm")) throw new IllegalArgumentException("Tambahkan kata confirm");
        arenas.redefineGlobal(player);
        configs.config().set("settings.enabled", false);
        plugin.saveConfig();
        player.sendMessage(TextUtil.component("&8[&bVelioraWar&8] &aBatas land berhasil diganti. Spawn dan data pemain tetap aman."));
    }

    private void setSpawn(Player player, String[] args) {
        requireArgs(args, 3, "/vgwar spawn <sword|mace|cpvp> <merah|biru>");
        MatchMode mode = parseMode(args[1]);
        MatchTeam team = MatchTeam.from(args[2]).orElseThrow(() ->
                new IllegalArgumentException("Gunakan team merah atau biru"));
        Arena arena = arenas.forMode(mode).orElseThrow(() ->
                new IllegalStateException("Land belum dibuat. Gunakan pos1, pos2, lalu /vgwar claim"));
        if (!arena.region().contains(player.getLocation())) {
            throw new IllegalStateException("Lokasi spawn harus berada di dalam land VelioraWar");
        }
        if (team == MatchTeam.RED) arena.redSpawn(player.getLocation());
        else arena.greenSpawn(player.getLocation());
        arena.enabled(configs.config().getBoolean("settings.enabled", false) && arena.isComplete());
        arenas.save();
        messages.send(player, "spawn-set", Map.of("arena", display(mode), "team", team.displayName()));
    }

    private void set(Player player, String[] args) {
        requireArgs(args, 2, "/vgwar set stay");
        if (!args[1].equalsIgnoreCase("stay")) throw new IllegalArgumentException("Gunakan /vgwar set stay");
        configs.stay(player.getLocation());
        messages.send(player, "stay-set");
    }

    private void enable(Player player) {
        if (!arenas.hasLand()) throw new IllegalStateException("Land belum dibuat");
        int enabled = arenas.enableCompleteProfiles();
        if (enabled == 0) throw new IllegalStateException("Belum ada mode yang memiliki spawn lengkap");
        configs.config().set("settings.enabled", true);
        plugin.saveConfig();
        player.sendMessage(TextUtil.component("&8[&bVelioraWar&8] &aVelioraWar aktif. &f" + enabled + " &amode siap dimainkan."));
    }

    private void disable(Player player) {
        arenas.disableAll();
        configs.config().set("settings.enabled", false);
        plugin.saveConfig();
        player.sendMessage(TextUtil.component("&8[&bVelioraWar&8] &eMaintenance aktif. Admin sekarang dapat memperbaiki land."));
    }

    private void info(Player player, String[] args) {
        if (!arenas.hasLand()) throw new IllegalStateException("Land VelioraWar belum dibuat");
        player.sendMessage(TextUtil.component("&8&m------------- &b&lVelioraWar &8&m-------------"));
        player.sendMessage(TextUtil.component("&7Status: " + (arenas.maintenance() ? "&eMAINTENANCE" : "&aAKTIF")));
        arenas.globalRegion().ifPresent(region -> {
            player.sendMessage(TextUtil.component("&7World: &f" + region.world()));
            player.sendMessage(TextUtil.component("&7Batas X/Z: &f" + region.minX() + ", " + region.minZ()
                    + " &8sampai &f" + region.maxX() + ", " + region.maxZ()));
            player.sendMessage(TextUtil.component("&7Vertikal: &fFULL HEIGHT &8(" + region.minY() + " sampai " + region.maxY() + ")"));
        });
        for (MatchMode mode : MatchMode.playable()) {
            Arena arena = arenas.forMode(mode).orElse(null);
            if (arena == null) continue;
            player.sendMessage(TextUtil.component("&f" + display(mode)
                    + " &8• &7Merah: " + yes(arena.redSpawn() != null)
                    + " &7Biru: " + yes(arena.greenSpawn() != null)
                    + " &7Enabled: " + yes(arena.enabled())));
        }
    }

    private void flag(Player player, String[] args) {
        flagGui.open(player, arenas.globalArena().orElseThrow(() -> new IllegalStateException("Land belum dibuat")));
    }

    private void npc(Player player, String[] args) {
        requireArgs(args, 2, "/vgwar npc <set|delete|cleanup> ...");
        if (args[1].equalsIgnoreCase("cleanup")) {
            int removed = npcs.cleanupOrphans();
            npcs.spawnAll();
            player.sendMessage(TextUtil.component("&8[&bVelioraWar&8] &aPembersihan selesai. &f"
                    + removed + " &aNPC/hologram lama dihapus."));
            return;
        }
        requireArgs(args, 3, "/vgwar npc <set|delete> <1|2>");
        int slot = parseNpcSlot(args[2]);
        if (args[1].equalsIgnoreCase("set")) {
            npcs.set(slot, player.getLocation());
            player.sendMessage(TextUtil.component("&8[&bVelioraWar&8] &aNPC refill " + slot + " berhasil dibuat."));
        } else if (args[1].equalsIgnoreCase("delete")) {
            npcs.removeSlot(slot);
            player.sendMessage(TextUtil.component("&8[&bVelioraWar&8] &eNPC refill " + slot + " berhasil dihapus."));
        } else throw new IllegalArgumentException("Gunakan /vgwar npc <set|delete|cleanup>");
    }

    private void legacyNpc(Player player, String[] args) {
        requireArgs(args, 2, "/vgwar npc set <1|2>");
        npcs.set(parseNpcSlot(args[1]), player.getLocation());
        player.sendMessage(TextUtil.component("&8[&bVelioraWar&8] &aNPC refill berhasil dibuat."));
    }

    private void delete(Player player, String[] args) {
        requireArgs(args, 2, "/vgwar delete <spawn|land> ...");
        if (args[1].equalsIgnoreCase("land")) {
            requireArgs(args, 3, "/vgwar delete land confirm");
            if (!args[2].equalsIgnoreCase("confirm")) throw new IllegalArgumentException("Tambahkan kata confirm");
            arenas.deleteLand();
            configs.config().set("settings.enabled", false);
            plugin.saveConfig();
            player.sendMessage(TextUtil.component("&8[&bVelioraWar&8] &eLand global telah dihapus. Data pemain tetap aman."));
            return;
        }
        if (args[1].equalsIgnoreCase("spawn")) {
            requireArgs(args, 4, "/vgwar delete spawn <mode> <merah|biru|semua>");
            MatchMode mode = parseMode(args[2]);
            Arena arena = arenas.forMode(mode).orElseThrow(() -> new IllegalStateException("Mode tidak ditemukan"));
            String target = args[3].toLowerCase(Locale.ROOT);
            if (target.equals("merah") || target.equals("red")) arena.redSpawn(null);
            else if (target.equals("biru") || target.equals("blue")) arena.greenSpawn(null);
            else if (target.equals("semua") || target.equals("all")) {
                arena.redSpawn(null);
                arena.greenSpawn(null);
            } else throw new IllegalArgumentException("Gunakan merah, biru, atau semua");
            arena.enabled(false);
            arenas.save();
            player.sendMessage(TextUtil.component("&8[&bVelioraWar&8] &eSpawn " + display(mode) + " berhasil diperbarui."));
            return;
        }
        throw new IllegalArgumentException("Gunakan /vgwar delete spawn ... atau /vgwar delete land confirm");
    }

    private int parseNpcSlot(String input) {
        try {
            int slot = Integer.parseInt(input);
            if (slot < 1 || slot > 2) throw new NumberFormatException();
            return slot;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Nomor NPC hanya 1 atau 2");
        }
    }

    private MatchMode parseMode(String value) {
        MatchMode mode = MatchMode.from(value).orElseThrow(() ->
                new IllegalArgumentException("Mode harus sword, mace, atau cpvp"));
        if (!mode.isPlayable()) throw new IllegalArgumentException("All Mode sedang dinonaktifkan");
        return mode;
    }

    private String display(MatchMode mode) {
        return switch (mode) {
            case SWORD_DUEL -> "Sword";
            case MACE_PVP -> "Mace";
            case CPVP -> "CPVP";
            case ALL_MODE -> "All Mode";
        };
    }

    private String yes(boolean value) {
        return value ? "&aYA" : "&cTIDAK";
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
            if (sender.hasPermission("veliorawar.admin")) suggestions.addAll(List.of(
                    "help", "pos1", "pos2", "claim", "redefine", "spawn", "set", "enable", "disable",
                    "info", "flag", "npc", "delete", "reload"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            suggestions.addAll(List.of("sword", "mace", "cpvp"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("spawn")) {
            suggestions.addAll(List.of("merah", "biru"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            suggestions.add("stay");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("npc")) {
            suggestions.addAll(List.of("set", "delete", "cleanup"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("npc")) {
            suggestions.addAll(List.of("1", "2"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            suggestions.addAll(List.of("spawn", "land"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("delete") && args[1].equalsIgnoreCase("spawn")) {
            suggestions.addAll(List.of("sword", "mace", "cpvp"));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("delete") && args[1].equalsIgnoreCase("spawn")) {
            suggestions.addAll(List.of("merah", "biru", "semua"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("redefine")) {
            suggestions.add("confirm");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("delete") && args[1].equalsIgnoreCase("land")) {
            suggestions.add("confirm");
        }
        String input = args[args.length - 1].toLowerCase(Locale.ROOT);
        return suggestions.stream().filter(value -> value.startsWith(input)).distinct().toList();
    }
}
