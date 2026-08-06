package id.veliora.war.command;

import id.veliora.war.gui.MainMenuGui;
import org.bukkit.entity.Player;

public final class MemberSubCommand {
    private final MainMenuGui menu;

    public MemberSubCommand(MainMenuGui menu) {
        this.menu = menu;
    }

    public void execute(Player player) {
        menu.open(player);
    }
}
