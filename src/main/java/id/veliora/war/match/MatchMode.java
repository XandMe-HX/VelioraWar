package id.veliora.war.match;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum MatchMode {
    SWORD_DUEL("sword_duel"), MACE_PVP("mace_pvp"), CPVP("cpvp"), ALL_MODE("all_mode");
    private static final List<MatchMode> PLAYABLE = List.of(SWORD_DUEL, MACE_PVP, CPVP, ALL_MODE);
    private final String id;
    MatchMode(String id) { this.id = id; }
    public String id() { return id; }
    public boolean isPlayable() { return true; }
    public static List<MatchMode> playable() { return PLAYABLE; }
    public String shortName() {
        return switch (this) {
            case SWORD_DUEL -> "sword"; case MACE_PVP -> "mace";
            case CPVP -> "cpvp"; case ALL_MODE -> "all";
        };
    }
    public static Optional<MatchMode> from(String value) {
        if (value == null) return Optional.empty();
        String normalized = value.toLowerCase().replace('-', '_').replace(' ', '_');
        if (normalized.equals("sword") || normalized.equals("pvp") || normalized.equals("sword_pvp")) return Optional.of(SWORD_DUEL);
        if (normalized.equals("mace")) return Optional.of(MACE_PVP);
        if (normalized.equals("crystal") || normalized.equals("crystal_pvp")) return Optional.of(CPVP);
        if (normalized.equals("all") || normalized.equals("semua")) return Optional.of(ALL_MODE);
        return Arrays.stream(values()).filter(mode -> mode.id.equals(normalized)).findFirst();
    }
}
