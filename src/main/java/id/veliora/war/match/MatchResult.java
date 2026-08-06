package id.veliora.war.match;

public record MatchResult(MatchTeam winner, String reason, boolean draw) {
    public static MatchResult winner(MatchTeam team, String reason) {
        return new MatchResult(team, reason, false);
    }

    public static MatchResult draw(String reason) {
        return new MatchResult(null, reason, true);
    }
}
