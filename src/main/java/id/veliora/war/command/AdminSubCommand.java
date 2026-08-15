package id.veliora.war.command;

import id.veliora.war.util.TextUtil;
import org.bukkit.command.CommandSender;

public final class AdminSubCommand {
    public void sendHelp(CommandSender sender) {
        sender.sendMessage(TextUtil.component("&8&m---------------- &b&lVelioraWar Admin &8&m----------------"));
        sender.sendMessage(TextUtil.component("&f/vgwar set stay &7- satu lokasi kembali setelah war"));
        sender.sendMessage(TextUtil.component("&f/vgwar pos1 &7- titik pertama region"));
        sender.sendMessage(TextUtil.component("&f/vgwar pos2 &7- titik kedua region"));
        sender.sendMessage(TextUtil.component("&f/vgwar claim &7- ID 1-4 Sword, 5-8 Mace, 9-12 Crystal, 13 All"));
        sender.sendMessage(TextUtil.component("&f/vgwar claim <1-13> &7- simpan region sesuai arena"));
        sender.sendMessage(TextUtil.component("&f/vgwar set <merah|biru> &7- berdiri di dalam claim arena"));
        sender.sendMessage(TextUtil.component("&f/vgwar flag &7- berdiri di dalam claim lalu buka aturan arena"));
        sender.sendMessage(TextUtil.component("&f/vgwar enable|disable [arena] &7- ID opsional bila berdiri di arena"));
        sender.sendMessage(TextUtil.component("&f/vgwar setnpc <1|2> &7- set NPC refill (maks. 2)"));
        sender.sendMessage(TextUtil.component("&f/vgwar info <arena> &7- informasi arena"));
        sender.sendMessage(TextUtil.component("&f/vgwar delete land <arena> confirm &7- hapus claim dengan konfirmasi"));
        sender.sendMessage(TextUtil.component("&f/vgwar delete spawn <arena> <merah|biru|semua>"));
        sender.sendMessage(TextUtil.component("&f/vgwar list &7- daftar arena"));
        sender.sendMessage(TextUtil.component("&f/vgwar reload &7- muat ulang YAML"));
    }
}
