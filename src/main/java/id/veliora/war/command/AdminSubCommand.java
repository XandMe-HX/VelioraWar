package id.veliora.war.command;

import id.veliora.war.util.TextUtil;
import org.bukkit.command.CommandSender;

public final class AdminSubCommand {
    public void sendHelp(CommandSender sender) {
        sender.sendMessage(TextUtil.component("&8&m------------- &#45D6FF&lVelioraWar Admin &8&m-------------"));
        sender.sendMessage(TextUtil.component("&f/vgwar pos1 &7- titik pertama satu land global"));
        sender.sendMessage(TextUtil.component("&f/vgwar pos2 &7- titik kedua satu land global"));
        sender.sendMessage(TextUtil.component("&f/vgwar claim &7- simpan seluruh wilayah VelioraWar"));
        sender.sendMessage(TextUtil.component("&f/vgwar spawn <sword|mace|cpvp|all> <merah|biru>"));
        sender.sendMessage(TextUtil.component("&f/vgwar set stay &7- lokasi kembali setelah war"));
        sender.sendMessage(TextUtil.component("&f/vgwar enable &7- aktifkan war dan perlindungan"));
        sender.sendMessage(TextUtil.component("&f/vgwar disable &7- maintenance; admin boleh membangun"));
        sender.sendMessage(TextUtil.component("&f/vgwar flag <mode> &7- atur aturan mode"));
        sender.sendMessage(TextUtil.component("&f/vgwar npc <set|delete> <1|2>"));
        sender.sendMessage(TextUtil.component("&f/vgwar info [mode] &7- periksa land dan spawn"));
        sender.sendMessage(TextUtil.component("&f/vgwar delete spawn <mode> <merah|biru|semua>"));
        sender.sendMessage(TextUtil.component("&f/vgwar delete land confirm &7- hapus land dengan aman"));
        sender.sendMessage(TextUtil.component("&f/vgwar reload &7- muat ulang YAML"));
    }
}
