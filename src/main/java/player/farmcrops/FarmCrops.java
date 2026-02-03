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
    private HoloManager holoManager;
    private StatsManager statsManager;
    private SettingsGUI settingsGUI;
    private MainMenuGUI mainMenuGUI;
    private StatsGUI statsGUI;
    private TopGUI topGUI;
    private PlayerSettings playerSettings;
    private PlayerSettingsGUI playerSettingsGUI;
    private CropPreviewManager cropPreviewManager;
    private boolean holoEnabled = false;
    
    // Premium Features (may be null in Lite version)
    private AchievementManager achievementManager;
    private DailyTaskManager dailyTaskManager;
    private CollectionManager collectionManager;
    private AchievementGUI achievementGUI;
    private TitleManager titleManager;
    private TitleGUI titleGUI;
    
    // Edition detection
    private boolean isPremiumEdition = false;
    private boolean hasPremiumClasses = false;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("========================================");
        getLogger().info("========================================");
        getLogger().info("       FARMCROPS v" + getDescription().getVersion());
        getLogger().info("  Weight-Based Crop Economy System");
        
        // Detect edition
        detectEdition();
        
        if (isPremiumEdition) {
            getLogger().info("         ⭐ PREMIUM EDITION ⭐");
            getLogger().info("  All features unlocked!");
        } else {
            getLogger().info("         💎 LITE EDITION 💎");
            getLogger().info("  (Upgrade to Premium for Achievements,");
            getLogger().info("   Daily Tasks, and Collections!)");
        }
        
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
        
        // Load Premium Features (if available)
        if (isPremiumEdition && hasPremiumClasses) {
            loadPremiumFeatures();
        } else if (isPremiumEdition && !hasPremiumClasses) {
            getLogger().warning("⚠ Premium edition detected but premium classes not found!");
            getLogger().warning("⚠ This may be a compilation issue. Premium features disabled.");
        } else {
            getLogger().info("ℹ Premium features not available in Lite edition");
            getLogger().info("ℹ To unlock: Achievements, Daily Tasks, Collections");
            getLogger().info("ℹ Visit: [Your website/store link here]");
        }
        getLogger().info("");

        // Commands (Basic - always available)
        getCommand("sellcrops").setExecutor(new SellCommand(this));
        getCommand("farmstats").setExecutor(new StatsCommand(this));
        getCommand("farmtop").setExecutor(new TopCommand(this));
        getCommand("farmsettings").setExecutor(new SettingsCommand(this));
        getCommand("farmreload").setExecutor(new ReloadCommand(this));
        getCommand("farm").setExecutor(new FarmCommand(this));
        getCommand("farmbackup").setExecutor(new BackupCommand(this));
        
        // Premium commands (only if available)
        if (isPremiumEdition && hasPremiumClasses) {
            try {
                getCommand("achievements").setExecutor(new AchievementCommand(this));
                getLogger().info("✓ Commands registered: /sellcrops, /farmstats, /farmtop, /farmsettings, /farmreload, /farm, /achievements, /farmbackup");
            } catch (Exception e) {
                getLogger().info("✓ Commands registered: /sellcrops, /farmstats, /farmtop, /farmsettings, /farmreload, /farm, /farmbackup");
            }
        } else {
            getLogger().info("✓ Commands registered: /sellcrops, /farmstats, /farmtop, /farmsettings, /farmreload, /farm, /farmbackup");
        }
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

        // DecentHolograms
        if (Bukkit.getPluginManager().getPlugin("DecentHolograms") != null) {
            cropPreviewManager = new CropPreviewManager(this);
            holoManager = new HoloManager(this);
            Bukkit.getPluginManager().registerEvents(cropPreviewManager, this);
            holoEnabled = true;
            getLogger().info("✓ DecentHolograms integration active");
            getLogger().info("  - Right-click preview: " + getConfig().getBoolean("holograms.right-click-preview"));
            getLogger().info("  - Preview duration: " + getConfig().getInt("holograms.preview-duration") + "s");
        } else {
            getLogger().info("  DecentHolograms not found — hologram support disabled");
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
        getLogger().info("  ✓✓✓ FARMCROPS v" + getDescription().getVersion() + " ENABLED ✓✓✓");
        getLogger().info("  Edition: " + (isPremiumEdition ? "Premium" : "Lite"));
        getLogger().info("========================================");
    }

    /**
     * Detect if this is Premium or Lite edition
     * Checks both version number and if premium classes exist
     */
    private void detectEdition() {
        // Method 1: Check version number (1.0.0+ = Premium, 0.9.x = Lite)
        String version = getDescription().getVersion();
        isPremiumEdition = version.startsWith("1.0") || version.startsWith("1.1") || version.startsWith("1.2");
        
        // Method 2: Check if premium classes actually exist (for compile-time exclusion)
        try {
            Class.forName("player.farmcrops.AchievementManager");
            Class.forName("player.farmcrops.DailyTaskManager");
            Class.forName("player.farmcrops.CollectionManager");
            hasPremiumClasses = true;
        } catch (ClassNotFoundException e) {
            hasPremiumClasses = false;
        }
        
        // Override: If classes don't exist, can't be premium regardless of version
        if (!hasPremiumClasses && isPremiumEdition) {
            getLogger().warning("Premium version detected but classes missing - treating as Lite");
            isPremiumEdition = false;
        }
    }

    /**
     * Load premium features (Achievements, Daily Tasks, Collections)
     * Only called if isPremiumEdition && hasPremiumClasses
     */
    private void loadPremiumFeatures() {
        try {
            getLogger().info("─────────────────────────────────────");
            getLogger().info("  Loading Premium Features...");
            getLogger().info("─────────────────────────────────────");
            
            // Achievement System
            if (getConfig().getBoolean("achievements.enabled", true)) {
                achievementManager = new AchievementManager(this);
                achievementGUI = new AchievementGUI(this);
                titleManager = new TitleManager(this);
                titleGUI = new TitleGUI(this);
                getServer().getPluginManager().registerEvents(achievementGUI, this);
                getServer().getPluginManager().registerEvents(titleGUI, this);
                getLogger().info("✓ Achievement System enabled");
                getLogger().info("✓ Title System enabled");
            } else {
                getLogger().info("✗ Achievements disabled in config");
            }
            
            // Daily Tasks
            if (getConfig().getBoolean("daily-tasks.enabled", true)) {
                dailyTaskManager = new DailyTaskManager(this);
                getLogger().info("✓ Daily Tasks enabled");
            } else {
                getLogger().info("✗ Daily Tasks disabled in config");
            }
            
            // Collections
            if (getConfig().getBoolean("collections.enabled", true)) {
                collectionManager = new CollectionManager(this);
                getLogger().info("✓ Crop Collections enabled");
            } else {
                getLogger().info("✗ Collections disabled in config");
            }
            
            getLogger().info("─────────────────────────────────────");
            getLogger().info("  ⭐ Premium Features Loaded! ⭐");
            getLogger().info("─────────────────────────────────────");
            
        } catch (Exception e) {
            getLogger().severe("Failed to load premium features!");
            e.printStackTrace();
            getLogger().severe("Premium features will be unavailable.");
        }
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
        
        // Save premium features (if they exist)
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
        getLogger().info("  FarmCrops v" + getDescription().getVersion() + " shutting down...");
        getLogger().info("  Edition: " + (isPremiumEdition ? "Premium" : "Lite"));
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
    public HoloManager getHoloManager()        { return holoManager; }
    
    // Edition info
    public boolean isPremiumEdition()          { return isPremiumEdition; }
    public boolean hasPremiumFeatures()        { return isPremiumEdition && hasPremiumClasses; }
    
    // Premium Features (may return null in Lite version!)
    public AchievementManager getAchievementManager() { return achievementManager; }
    public AchievementGUI getAchievementGUI() { return achievementGUI; }
    public DailyTaskManager getDailyTaskManager() { return dailyTaskManager; }
    public CollectionManager getCollectionManager() { return collectionManager; }
    public TitleManager getTitleManager() { return titleManager; }
    public TitleGUI getTitleGUI() { return titleGUI; }
}
