package id.veliora.war.arena;

import java.util.Arrays;
import java.util.Optional;

public enum ArenaFlag {
    PVP("pvp", true),
    BLOCK_PLACE("block-place", true),
    BLOCK_BREAK("block-break", true),
    TEMPORARY_BLOCK("temporary-block", true),
    EXPLOSION_DAMAGE("explosion-damage", true),
    EXPLOSION_BLOCK_DAMAGE("explosion-block-damage", false),
    VOID_TELEPORT("void-teleport", true),
    FALL_DAMAGE("fall-damage", true),
    ITEM_DROP("item-drop", false),
    ITEM_PICKUP("item-pickup", true),
    INTERACT("interact", true),
    FIRE_SPREAD("fire-spread", false),
    LIQUID_FLOW("liquid-flow", true),
    PISTON("piston", false),
    MOB_SPAWN("mob-spawn", false),
    KEEP_INVENTORY("keep-inventory", true),
    ALLOW_TOTEM("allow-totem", true),
    ALLOW_ELYTRA("allow-elytra", true),
    ALLOW_COMMAND("allow-command", false),
    ANTI_ILLEGAL_ITEM("anti-illegal-item", true);

    private final String key;
    private final boolean defaultValue;
    ArenaFlag(String key, boolean defaultValue) { this.key=key; this.defaultValue=defaultValue; }
    public String key(){return key;}
    public boolean defaultValue(){return defaultValue;}
    public static Optional<ArenaFlag> from(String value){
        return Arrays.stream(values()).filter(flag->flag.key.equalsIgnoreCase(value)).findFirst();
    }
}
