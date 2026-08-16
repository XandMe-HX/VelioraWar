package id.veliora.war.command;
import id.veliora.war.util.TextUtil;
import org.bukkit.command.CommandSender;
public final class AdminSubCommand {
 public void sendHelp(CommandSender s){
  s.sendMessage(TextUtil.component("&8&m------------- &#45D6FF&lVelioraWar Admin &8&m-------------"));
  s.sendMessage(TextUtil.component("&f/vgwar pos1 / pos2 &7- ubah dua pojok sebelum claim"));
  s.sendMessage(TextUtil.component("&f/vgwar claim &7- buat satu land global penuh dari bawah sampai atas"));
  s.sendMessage(TextUtil.component("&f/vgwar redefine confirm &7- ganti batas tanpa menghapus data pemain"));
  s.sendMessage(TextUtil.component("&f/vgwar spawn <sword|mace|cpvp> <merah|biru>"));
  s.sendMessage(TextUtil.component("&f/vgwar set stay &7- lokasi kembali dan penyelamat void"));
  s.sendMessage(TextUtil.component("&f/vgwar enable/disable &7- aktifkan atau maintenance"));
  s.sendMessage(TextUtil.component("&f/vgwar flag &7- atur perlindungan land global"));
  s.sendMessage(TextUtil.component("&f/vgwar info &7- periksa batas, spawn, dan status"));
  s.sendMessage(TextUtil.component("&f/vgwar delete spawn <mode> <merah|biru|semua>"));
  s.sendMessage(TextUtil.component("&f/vgwar delete land confirm &7- hapus land dengan aman"));
  s.sendMessage(TextUtil.component("&f/vgwar reload &7- muat ulang YAML"));
 }
}
