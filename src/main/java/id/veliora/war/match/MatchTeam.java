package id.veliora.war.match;

import java.util.Optional;

public enum MatchTeam {
    RED("red", "&cMerah"),
    GREEN("blue", "&9Biru");

    private final String id;
    private final String displayName;

    MatchTeam(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static Optional<MatchTeam> from(String value) {
        if (value == null) return Optional.empty();
        return switch (value.toLowerCase()) {
            case "red", "merah", "1" -> Optional.of(RED);
            case "blue", "biru", "green", "hijau", "2" -> Optional.of(GREEN);
            default -> Optional.empty();
        };
    }
}
