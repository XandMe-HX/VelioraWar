package id.veliora.war.command;

import id.veliora.war.util.TextUtil;
import org.bukkit.command.CommandSender;

public final class AdminSubCommand {
    public void sendHelp(CommandSender sender) {
        sender.sendMessage(TextUtil.component("&8&m---------------- &b&lVelioraWar Admin &8&m----------------"));
        sender.sendMessage(TextUtil.component("&f/vgwar pos1 &7- titik pertama region"));
        sender.sendMessage(TextUtil.component("&f/vgwar pos2 &7- titik kedua region"));
        sender.sendMessage(TextUtil.component("&f/vgwar claim <arena> &7- simpan region"));
        sender.sendMessage(TextUtil.component("&f/vgwar set <arena> <mode> <size> &7- mode dan ukuran"));
        sender.sendMessage(TextUtil.component("&f/vgwar set <arena> <1|2> &7- spawn merah/hijau"));
        sender.sendMessage(TextUtil.component("&f/vgwar reset <arena> &7- reset kedua spawn"));
        sender.sendMessage(TextUtil.component("&f/vgwar flag <arena> &7- GUI aturan arena"));
        sender.sendMessage(TextUtil.component("&f/vgwar enable|disable <arena>"));
        sender.sendMessage(TextUtil.component("&f/vgwar setnpc <arena> &7- NPC refill"));
        sender.sendMessage(TextUtil.component("&f/vgwar setwarp &7- warp utama"));
        sender.sendMessage(TextUtil.component("&f/vgwar info|delete <arena> &7- informasi/hapus"));
        sender.sendMessage(TextUtil.component("&f/vgwar list &7- daftar arena"));
        sender.sendMessage(TextUtil.component("&f/vgwar reload &7- muat ulang YAML"));
    }
}
