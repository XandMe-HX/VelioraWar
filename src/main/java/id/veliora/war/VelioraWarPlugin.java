package id.veliora.war;

import id.veliora.war.arena.ArenaManager;
import id.veliora.war.command.VgWarCommand;
import id.veliora.war.cooldown.CooldownManager;
import id.veliora.war.gui.FlagGui;
import id.veliora.war.gui.GuideGui;
import id.veliora.war.gui.MainMenuGui;
import id.veliora.war.gui.ModeSelectGui;
import id.veliora.war.gui.RefillGui;
import id.veliora.war.gui.TeamSelectGui;
import id.veliora.war.gui.WarItemsGui;
import id.veliora.war.inventory.InventoryManager;
import id.veliora.war.inventory.LoadoutManager;
import id.veliora.war.listener.BlockListener;
import id.veliora.war.listener.CheatGuardListener;
import id.veliora.war.listener.DamageListener;
import id.veliora.war.listener.ExplosionListener;
import id.veliora.war.listener.InventoryListener;
import id.veliora.war.listener.PlayerListener;
import id.veliora.war.match.MatchManager;
import id.veliora.war.npc.RefillNpcManager;
import id.veliora.war.protection.ExplosionProtection;
import id.veliora.war.protection.RegionProtection;
import id.veliora.war.protection.TemporaryBlockManager;
import id.veliora.war.storage.ArenaStorage;
import id.veliora.war.storage.ConfigManager;
import id.veliora.war.storage.MessageManager;
import id.veliora.war.storage.PlayerDataStorage;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class VelioraWarPlugin extends JavaPlugin {
    private static VelioraWarPlugin instance;
    private ConfigManager configs;
    private MessageManager messages;
    private PlayerDataStorage playerData;
    private ArenaManager arenaManager;
    private LoadoutManager loadouts;
    private InventoryManager inventories;
    private TemporaryBlockManager temporaryBlocks;
    private CooldownManager cooldowns;
    private MatchManager matches;
    private RefillNpcManager refillNpcs;

    @Override
    public void onEnable() {
        instance = this;
        configs = new ConfigManager(this);
        configs.load();
        messages = new MessageManager(configs);
        playerData = new PlayerDataStorage(configs);
        arenaManager = new ArenaManager(new ArenaStorage(configs));
        loadouts = new LoadoutManager(configs, playerData);
        inventories = new InventoryManager(playerData);
        temporaryBlocks = new TemporaryBlockManager();
        cooldowns = new CooldownManager();
        matches = new MatchManager(this, configs, arenaManager, messages, inventories, loadouts,
                temporaryBlocks, cooldowns, playerData);

        RegionProtection regions = new RegionProtection(arenaManager, matches);
        ExplosionProtection explosions = new ExplosionProtection();
        MainMenuGui mainMenu = new MainMenuGui(configs, loadouts, matches);
        ModeSelectGui modeMenu = new ModeSelectGui(configs, loadouts);
        TeamSelectGui teamMenu = new TeamSelectGui(configs);
        FlagGui flagMenu = new FlagGui(configs);
        RefillGui refillMenu = new RefillGui(configs, cooldowns);
        GuideGui guideMenu = new GuideGui(configs);
        WarItemsGui warItemsMenu = new WarItemsGui(playerData);
        refillNpcs = new RefillNpcManager(this, configs, arenaManager);

        registerCommand(new VgWarCommand(this, arenaManager, configs, messages,
                mainMenu, flagMenu, refillNpcs, matches, refillMenu));

        PluginManager plugins = getServer().getPluginManager();
        plugins.registerEvents(new PlayerListener(this, configs, messages, matches, inventories,
                cooldowns, refillNpcs, refillMenu), this);
        plugins.registerEvents(new BlockListener(configs, regions, matches, loadouts, temporaryBlocks), this);
        plugins.registerEvents(new DamageListener(arenaManager, matches), this);
        plugins.registerEvents(new ExplosionListener(arenaManager, explosions, temporaryBlocks), this);
        plugins.registerEvents(new InventoryListener(configs, arenaManager, messages, matches, loadouts,
                cooldowns, mainMenu, modeMenu, teamMenu, flagMenu, refillMenu, guideMenu, warItemsMenu, playerData), this);
        CheatGuardListener cheatGuard = new CheatGuardListener(this, configs, messages, matches, loadouts);
        plugins.registerEvents(cheatGuard, this);
        cheatGuard.start();

        // ZNPCS menangani NPC refill. NPC bawaan hanya aktif jika memang diminta di config.
        if (configs.config().getBoolean("features.native-refill-npc-enabled", false)) {
            getServer().getScheduler().runTask(this, refillNpcs::spawnAll);
        }
        getServer().getScheduler().runTaskTimer(this, cooldowns::cleanup, 1200L, 1200L);
        getLogger().info("VelioraWar aktif. " + arenaManager.all().size() + " arena berhasil dimuat.");
    }

    @Override
    public void onDisable() {
        if (matches != null) matches.shutdown();
        if (refillNpcs != null) refillNpcs.shutdown();
        if (temporaryBlocks != null && arenaManager != null) temporaryBlocks.restoreAll(arenaManager.all());
        if (arenaManager != null) arenaManager.save();
        if (playerData != null) playerData.save();
        getLogger().info("VelioraWar nonaktif. Inventory dan temporary block telah diamankan.");
        instance = null;
    }

    public void reloadAll() {
        matches.shutdown();
        configs.reload();
        arenaManager.reload();
        loadouts.reload();
        matches.restartQueue();
        if (configs.config().getBoolean("features.native-refill-npc-enabled", false)) refillNpcs.spawnAll();
    }

    private void registerCommand(VgWarCommand executor) {
        PluginCommand command = getCommand("vgwar");
        if (command == null) throw new IllegalStateException("Command vgwar tidak ditemukan di plugin.yml");
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    public static VelioraWarPlugin getInstance() {
        if (instance == null) throw new IllegalStateException("VelioraWar belum aktif");
        return instance;
    }

    public ArenaManager arenaManager() {
        return arenaManager;
    }
}
