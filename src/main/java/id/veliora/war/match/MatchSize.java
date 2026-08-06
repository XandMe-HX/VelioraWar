package id.veliora.war.match;

import java.util.Arrays;
import java.util.Optional;

public enum MatchSize {
    ONE_VS_ONE("1vs1", 1),
    TWO_VS_TWO("2vs2", 2),
    THREE_VS_THREE("3vs3", 3),
    FOUR_VS_FOUR("4vs4", 4),
    UNLIMITED("unlimited", Integer.MAX_VALUE);

    private final String id;
    private final int playersPerTeam;

    MatchSize(String id, int playersPerTeam) {
        this.id = id;
        this.playersPerTeam = playersPerTeam;
    }

    public String id() {
        return id;
    }

    public int playersPerTeam() {
        return playersPerTeam;
    }

    public static Optional<MatchSize> from(String value) {
        if (value == null) return Optional.empty();
        String normalized = value.toLowerCase().replace("v", "vs");
        return Arrays.stream(values())
                .filter(size -> size.id.equalsIgnoreCase(value) || size.id.equals(normalized))
                .findFirst();
    }
}
