package id.veliora.war.placeholder;

import id.veliora.war.VelioraWarPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.util.Locale;

/** Numeric PlaceholderAPI values designed for ajLeaderboards. */
public final class VelioraWarPlaceholderExpansion extends PlaceholderExpansion {
    private final VelioraWarPlugin plugin;

    public VelioraWarPlaceholderExpansion(VelioraWarPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getIdentifier() { return "veliorawar"; }
    @Override public String getAuthor() { return "XandMe"; }
    @Override public String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override public String onPlaceholderRequest(Player player, String params) {
        if (player == null || params == null) return "0";
        int wins = plugin.playerData().intValue(player.getUniqueId(), "stats.wins", 0);
        int losses = plugin.playerData().intValue(player.getUniqueId(), "stats.losses", 0);
        int draws = plugin.playerData().intValue(player.getUniqueId(), "stats.draws", 0);
        int kills = plugin.playerData().intValue(player.getUniqueId(), "stats.kills", 0);
        int deaths = plugin.playerData().intValue(player.getUniqueId(), "stats.deaths", 0);
        return switch (params.toLowerCase(Locale.ROOT)) {
            case "wins", "leaderboard_wins" -> String.valueOf(wins);
            case "losses", "leaderboard_losses" -> String.valueOf(losses);
            case "draws", "leaderboard_draws" -> String.valueOf(draws);
            case "matches", "played", "leaderboard_matches" -> String.valueOf(wins + losses + draws);
            case "winrate", "win_rate" -> decimal(wins, Math.max(1, wins + losses + draws));
            case "kills", "leaderboard_kills" -> String.valueOf(kills);
            case "deaths", "leaderboard_deaths" -> String.valueOf(deaths);
            case "kd", "kdr", "kill_death_ratio" -> decimal(kills, Math.max(1, deaths));
            default -> null;
        };
    }

    private String decimal(int numerator, int denominator) {
        return String.format(Locale.ROOT, "%.2f", numerator / (double) denominator);
    }
}
