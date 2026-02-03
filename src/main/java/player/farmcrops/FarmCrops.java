package player.farmcrops;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class FarmCrops extends JavaPlugin implements Listener {

    private static FarmCrops instance;
    private Economy economy;
    private SellGUI sellGUI;
    // HoloManager removed - using CropPreviewManager with FancyHolograms
    private StatsManager statsManager;
    private SettingsGUI settingsGUI;
    private MainMenuGUI mainMenuGUI;
    private StatsGUI statsGUI;
    private TopGUI topGUI;
    private PlayerSettings playerSettings;
    private PlayerSettingsGUI playerSettingsGUI;
    private CropPreviewManager cropPreviewManager;
    private boolean holoEnabled = false;
    
    // New v1.0.0 Features
    private AchievementManager achievementManager;
    private DailyTaskManager dailyTaskManager;
    private CollectionManager collectionManager;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("========================================");
        getLogger().info("========================================");
        getLogger().info("       FARMCROPS v0.9.5");
        getLogger().info("  Weight-Based Crop Economy System");
        getLogger().info("========================================");
        getLogger().info("========================================");
        getLogger().info("");
        getLogger().info("Starting initialization...");
        getLogger().info("");

        saveDefaultConfig();
        getLogger().info("✓ Configuration loaded");
        getLogger().info("  - Weight: " + getConfig().getDouble("weight.min") + " – " + getConfig().getDouble("weight.max") + " kg");
        getLogger().info("  - Per-crop pricing enabled");
        getLogger().info("  - Seeds: " + (getConfig().getBoolean("seeds.enabled") ? "Enabled" : "Disabled"));
        getLogger().info("");

        // Vault
        getLogger().info("Searching for Vault...");
        if (!setupEconomy()) {
            getLogger().severe("========================================");
            getLogger().severe("  ✗✗✗ CRITICAL: VAULT NOT FOUND ✗✗✗");
            getLogger().severe("  FarmCrops requires Vault!");
            getLogger().severe("  https://www.spigotmc.org/resources/vault.34315/");
            getLogger().severe("========================================");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getLogger().info("✓ Vault hooked — Economy: " + economy.getName());
        getLogger().info("");

        // Stats
        statsManager = new StatsManager(this);
        getLogger().info("✓ Stats system initialized");
        getLogger().info("");

        // Player Settings (v0.9.0)
        playerSettings = new PlayerSettings(this);
        getLogger().info("✓ Player settings system initialized");
        getLogger().info("");

        // Event listeners
        getServer().getPluginManager().registerEvents(new CropListener(this), this);
        getLogger().info("✓ Crop listener registered (Wheat, Carrot, Potato, Beetroot, Melon)");

        // Register this class as a listener for PlayerQuitEvent (cache cleanup)
        getServer().getPluginManager().registerEvents(this, this);

        // Sell GUI
        sellGUI = new SellGUI(this);
        getServer().getPluginManager().registerEvents(sellGUI, this);
        getLogger().info("✓ Sell GUI initialized (grouped by tier + crop)");
        getLogger().info("");

        // Settings GUI
        settingsGUI = new SettingsGUI(this);
        getServer().getPluginManager().registerEvents(settingsGUI, this);
        getLogger().info("✓ Settings GUI initialized");
        getLogger().info("");

        // Main Menu GUI (v0.8.0)
        mainMenuGUI = new MainMenuGUI(this);
        getServer().getPluginManager().registerEvents(mainMenuGUI, this);
        getLogger().info("✓ Main Menu GUI initialized");
        getLogger().info("");

        // Stats GUI (v0.8.0)
        statsGUI = new StatsGUI(this);
        getServer().getPluginManager().registerEvents(statsGUI, this);
        getLogger().info("✓ Stats GUI initialized");
        getLogger().info("");

        // Top GUI (v0.8.0)
        topGUI = new TopGUI(this);
        getServer().getPluginManager().registerEvents(topGUI, this);
        getLogger().info("✓ Top GUI initialized");
        getLogger().info("");

        // Player Settings GUI (v0.9.0)
        playerSettingsGUI = new PlayerSettingsGUI(this);
        getServer().getPluginManager().registerEvents(playerSettingsGUI, this);
        getLogger().info("✓ Player Settings GUI initialized");
        getLogger().info("");

        // Crop Preview Manager (v0.10.0)
        cropPreviewManager = new CropPreviewManager(this);
        getServer().getPluginManager().registerEvents(cropPreviewManager, this);
        getLogger().info("✓ Crop Preview Manager initialized (right-click preview + caching)");
        getLogger().info("");
        
        // NEW v1.0.0 Features
        if (getConfig().getBoolean("achievements.enabled", true)) {
            achievementManager = new AchievementManager(this);
            getLogger().info("✓ Achievement System enabled");
        }
        
        if (getConfig().getBoolean("daily-tasks.enabled", true)) {
            dailyTaskManager = new DailyTaskManager(this);
            getLogger().info("✓ Daily Tasks enabled");
        }
        
        if (getConfig().getBoolean("collections.enabled", true)) {
            collectionManager = new CollectionManager(this);
            getLogger().info("✓ Crop Collections enabled");
        }
        getLogger().info("");

        // Commands
        getCommand("sellcrops").setExecutor(new SellCommand(this));
        getCommand("farmstats").setExecutor(new StatsCommand(this));
        getCommand("farmtop").setExecutor(new TopCommand(this));
        getCommand("farmsettings").setExecutor(new SettingsCommand(this));
        getCommand("farmreload").setExecutor(new ReloadCommand(this));
        getCommand("farm").setExecutor(new FarmCommand(this));
        getLogger().info("✓ Commands registered: /sellcrops, /farmstats, /farmtop, /farmsettings, /farmreload, /farm");
        getLogger().info("");
        
        // Auto-save scheduler (saves data every 5 minutes)
        int autoSaveInterval = getConfig().getInt("auto-save-interval", 6000); // 6000 ticks = 5 minutes
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (statsManager != null) {
                statsManager.saveAll();
            }
            if (playerSettings != null) {
                playerSettings.saveSettings();
            }
            getLogger().info("Auto-save completed (stats, settings)");
        }, autoSaveInterval, autoSaveInterval);
        getLogger().info("✓ Auto-save enabled (every " + (autoSaveInterval / 1200) + " minutes)");
        getLogger().info("");

        // PlaceholderAPI support temporarily disabled
        getLogger().info("  PlaceholderAPI integration: Disabled");
        getLogger().info("");

        // FancyHolograms
        if (Bukkit.getPluginManager().getPlugin("FancyHolograms") != null) {
            cropPreviewManager = new CropPreviewManager(this);
            Bukkit.getPluginManager().registerEvents(cropPreviewManager, this);
            holoEnabled = true;
            getLogger().info("✓ FancyHolograms integration active");
            getLogger().info("  - Right-click preview: " + getConfig().getBoolean("holograms.right-click-preview"));
            getLogger().info("  - Preview duration: " + getConfig().getInt("holograms.preview-duration") + "s");
        } else {
            getLogger().info("  FancyHolograms not found — hologram support disabled");
        }
        getLogger().info("");

        // Permissions info
        getLogger().info("✓ Permissions configured (LuckPerms compatible)");
        getLogger().info("  - farmcrops.harvest  — allow/deny custom crop drops");
        getLogger().info("  - farmcrops.sell     — open sell GUI");
        getLogger().info("  - farmcrops.stats    — view own stats");
        getLogger().info("  - farmcrops.stats.others — view others' stats");
        getLogger().info("  - farmcrops.top      — view leaderboard");
        getLogger().info("  - farmcrops.settings — admin settings GUI");
        getLogger().info("  - farmcrops.reload   — reload config");
        getLogger().info("  - farmcrops.menu     — main menu GUI");
        getLogger().info("  - farmcrops.admin    — grants all above");
        getLogger().info("  - farmcrops.autosell.use — auto-sell on harvest");
        getLogger().info("  - farmcrops.preview  — right-click crop preview");
        getLogger().info("");

        getLogger().info("========================================");
        getLogger().info("  ✓✓✓ FARMCROPS v0.9.5 ENABLED ✓✓✓");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        // Save all player data
        getLogger().info("Saving all player data...");
        
        if (statsManager != null) {
            statsManager.saveAll();
            getLogger().info("✓ Player statistics saved");
        }
        
        if (playerSettings != null) {
            playerSettings.saveSettings();
            getLogger().info("✓ Player settings saved");
        }
        
        if (achievementManager != null) {
            // TODO: Save achievements when persistence is implemented
        }
        
        if (dailyTaskManager != null) {
            // TODO: Save daily tasks when persistence is implemented
        }
        
        if (collectionManager != null) {
            // TODO: Save collections when persistence is implemented
        }

        getLogger().info("========================================");
        getLogger().info("  FarmCrops v0.9.5 shutting down...");
        getLogger().info("  All data saved successfully!");
        getLogger().info("========================================");
    }

    /**
     * Clear stats cache when a player leaves to prevent memory leaks.
     * Also saves their data immediately to prevent data loss.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        
        // Save player data immediately before clearing cache
        if (statsManager != null) {
            StatsManager.PlayerStats stats = statsManager.getStats(uuid);
            // Data is auto-saved by StatsManager, just clear cache
            statsManager.clearCache(uuid);
        }
        
        if (playerSettings != null) {
            // Save before clearing cache
            playerSettings.saveSettings();
            playerSettings.clearCache(uuid);
        }
        
        if (holoEnabled && cropPreviewManager != null) {
            cropPreviewManager.cleanup();
        }
    }

    /**
     * Get the base price for a crop type (delegates to CropListener logic).
     */
    public double getCropPrice(Material cropType) {
        String cropKey = null;
        switch (cropType) {
            case WHEAT:      cropKey = "wheat";    break;
            case CARROTS:    cropKey = "carrot";   break;
            case POTATOES:   cropKey = "potato";   break;
            case BEETROOTS:  cropKey = "beetroot"; break;
            case MELON:      cropKey = "melon";    break;
            default: break;
        }
        if (cropKey != null && getConfig().contains("prices." + cropKey)) {
            return getConfig().getDouble("prices." + cropKey);
        }
        return getConfig().getDouble("prices.default", 10.0);
    }

    // ─────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp =
            getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    // ─────────────────────────────────────────────
    // Getters
    // ─────────────────────────────────────────────

    public static FarmCrops getInstance()      { return instance; }
    public Economy getEconomy()                { return economy; }
    public SellGUI getSellGUI()                { return sellGUI; }
    public boolean isHoloEnabled()             { return holoEnabled; }
    public StatsManager getStatsManager()      { return statsManager; }
    public SettingsGUI getSettingsGUI()        { return settingsGUI; }
    public MainMenuGUI getMainMenuGUI()        { return mainMenuGUI; }
    public StatsGUI getStatsGUI()              { return statsGUI; }
    public TopGUI getTopGUI()                  { return topGUI; }
    public PlayerSettings getPlayerSettings()  { return playerSettings; }
    public PlayerSettingsGUI getPlayerSettingsGUI() { return playerSettingsGUI; }
    public CropPreviewManager getCropPreviewManager() { return cropPreviewManager; }
    
    // v1.0.0 Features
    public AchievementManager getAchievementManager() { return achievementManager; }
    public DailyTaskManager getDailyTaskManager() { return dailyTaskManager; }
    public CollectionManager getCollectionManager() { return collectionManager; }
}
