package id.veliora.war.match;

import java.util.Arrays;
import java.util.Optional;

public enum MatchMode {
    SWORD_DUEL("sword_duel"),
    MACE_PVP("mace_pvp"),
    CPVP("cpvp"),
    ALL_MODE("all_mode");

    private final String id;

    MatchMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<MatchMode> from(String value) {
        if (value == null) return Optional.empty();
        String normalized = value.toLowerCase().replace('-', '_');
        return Arrays.stream(values()).filter(mode -> mode.id.equals(normalized)).findFirst();
    }
}
