package id.veliora.war.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/** Uses Vault only when it is installed; no hard dependency is required to start the plugin. */
public final class VaultUtil {
    private VaultUtil() { }
    public static boolean withdraw(Player player, double amount) {
        try {
            Class<?> economy = Class.forName("net.milkbowl.vault.economy.Economy");
            Object registration = Bukkit.getServicesManager().getRegistration(economy);
            if (registration == null) return false;
            Object provider = registration.getClass().getMethod("getProvider").invoke(registration);
            Object response = economy.getMethod("withdrawPlayer", org.bukkit.OfflinePlayer.class, double.class).invoke(provider, player, amount);
            Method success = response.getClass().getMethod("transactionSuccess");
            return Boolean.TRUE.equals(success.invoke(response));
        } catch (ReflectiveOperationException ignored) { return false; }
    }
    public static boolean available() {
        try { return Bukkit.getPluginManager().isPluginEnabled("Vault") && Class.forName("net.milkbowl.vault.economy.Economy") != null; }
        catch (ClassNotFoundException ignored) { return false; }
    }
}
