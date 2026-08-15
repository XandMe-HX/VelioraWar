package id.veliora.war.command;

import id.veliora.war.util.TextUtil;
import org.bukkit.command.CommandSender;

public final class AdminSubCommand {
    public void sendHelp(CommandSender sender) {
        sender.sendMessage(TextUtil.component("&8&m---------------- &b&lVelioraWar Admin &8&m----------------"));
        sender.sendMessage(TextUtil.component("&f/vgwar setwarp &7- set lokasi kembali setelah war"));
        sender.sendMessage(TextUtil.component("&f/vgwar pos1 &7- titik pertama region"));
        sender.sendMessage(TextUtil.component("&f/vgwar pos2 &7- titik kedua region"));
        sender.sendMessage(TextUtil.component("&f/vgwar claim &7- tampilkan ID: 1 Sword, 2 Mace, 3 Crystal, 4 All"));
        sender.sendMessage(TextUtil.component("&f/vgwar claim <1-4> &7- simpan region sesuai arena"));
        sender.sendMessage(TextUtil.component("&f/vgwar spawn <merah|hijau> &7- berdiri di dalam claim arena"));
        sender.sendMessage(TextUtil.component("&f/vgwar flag &7- berdiri di dalam claim lalu buka aturan arena"));
        sender.sendMessage(TextUtil.component("&f/vgwar enable|disable <arena>"));
        sender.sendMessage(TextUtil.component("&f/vgwar setnpc <1|2> &7- set NPC refill (maks. 2)"));
        sender.sendMessage(TextUtil.component("&f/vgwar info|delete <arena> &7- informasi/hapus"));
        sender.sendMessage(TextUtil.component("&f/vgwar list &7- daftar arena"));
        sender.sendMessage(TextUtil.component("&f/vgwar reload &7- muat ulang YAML"));
    }
}
