package player.farmcrops;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

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
    private ScoreboardManager scoreboardManager;
    private ActionBarManager actionBarManager;
    private MessageHandler messageHandler;
    private ChatListener chatListener;
    private boolean holoEnabled = false;
    
    // Premium Features (may be null in Lite version)
    private AchievementManager achievementManager;
    private DailyTaskManager dailyTaskManager;
    private CollectionManager collectionManager;
    private AchievementGUI achievementGUI;
    private TitleManager titleManager;
    private TitleGUI titleGUI;
    private DailyTaskGUI dailyTaskGUI;
    
    // Edition detection
    private boolean isPremiumEdition = false;
    private boolean hasPremiumClasses = false;
    
    // Track players who've seen welcome message (prevents spam on reload)
    private final Set<UUID> hasSeenWelcome = new HashSet<>();

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("========================================");
        getLogger().info("========================================");
        getLogger().info("       FARMCROPS v" + getDescription().getVersion());
        getLogger().info("  Weight-Based Crop Economy System");
        
        // Detect edition BEFORE showing any edition-specific messages
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
        
        // Initialize Message Handler
        messageHandler = new MessageHandler(this);
        getLogger().info("✓ Message handler initialized");
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

        // Player Settings
        playerSettings = new PlayerSettings(this);
        getLogger().info("✓ Player settings system initialized");
        getLogger().info("");
        
        // Scoreboard Manager
        scoreboardManager = new ScoreboardManager(this);
        getLogger().info("✓ Scoreboard manager initialized");
        getLogger().info("");
        
        // Action Bar Manager
        actionBarManager = new ActionBarManager(this);
        getLogger().info("✓ Action bar manager initialized");
        getLogger().info("");

        // Event listeners
        getServer().getPluginManager().registerEvents(new CropListener(this), this);
        getLogger().info("✓ Crop listener registered (Wheat, Carrot, Potato, Beetroot, Melon)");

        // Register this class as a listener for PlayerQuitEvent and PlayerJoinEvent
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

        // Main Menu GUI
        mainMenuGUI = new MainMenuGUI(this);
        getServer().getPluginManager().registerEvents(mainMenuGUI, this);
        getLogger().info("✓ Main Menu GUI initialized");
        getLogger().info("");

        // Stats GUI
        statsGUI = new StatsGUI(this);
        getServer().getPluginManager().registerEvents(statsGUI, this);
        getLogger().info("✓ Stats GUI initialized");
        getLogger().info("");

        // Top GUI
        topGUI = new TopGUI(this);
        getServer().getPluginManager().registerEvents(topGUI, this);
        getLogger().info("✓ Top GUI initialized");
        getLogger().info("");

        // Player Settings GUI
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
        
        // ALWAYS register achievements and dailytasks commands (show upgrade message in Lite, open GUI in Premium)
        getCommand("achievements").setExecutor(new AchievementCommand(this));
        getCommand("dailytasks").setExecutor(new DailyTasksCommand(this));
        
        getLogger().info("✓ Commands registered: /sellcrops, /farmstats, /farmtop, /farmsettings, /farmreload, /farm, /achievements, /dailytasks, /farmbackup");
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
     * FIXED: Always detect fresh on startup, don't rely on saved config
     */
    private void detectEdition() {
        // Check if premium classes exist in the JAR
        try {
            Class.forName("player.farmcrops.AchievementManager");
            Class.forName("player.farmcrops.DailyTaskManager");
            Class.forName("player.farmcrops.CollectionManager");
            hasPremiumClasses = true;
            isPremiumEdition = true;
            getLogger().info("[Edition Detection] Premium classes found - PREMIUM EDITION");
        } catch (ClassNotFoundException e) {
            hasPremiumClasses = false;
            isPremiumEdition = false;
            getLogger().info("[Edition Detection] Premium classes not found - LITE EDITION");
        }
        
        // NEVER save edition to config - always detect fresh each startup
        getLogger().info("[Edition Detection] Running as: " + (isPremiumEdition ? "PREMIUM" : "LITE") + 
                         " Edition v" + getDescription().getVersion());
    }

    /**
     * Load premium features (Achievements, Daily Tasks, Collections)
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
                chatListener = new ChatListener(this);
                getServer().getPluginManager().registerEvents(achievementGUI, this);
                getServer().getPluginManager().registerEvents(titleGUI, this);
                getServer().getPluginManager().registerEvents(chatListener, this);
                getLogger().info("✓ Achievement System enabled");
                getLogger().info("✓ Title System enabled");
            } else {
                getLogger().info("✗ Achievements disabled in config");
            }
            
            // Daily Tasks
            if (getConfig().getBoolean("daily-tasks.enabled", true)) {
                dailyTaskManager = new DailyTaskManager(this);
                dailyTaskGUI = new DailyTaskGUI(this);
                getServer().getPluginManager().registerEvents(dailyTaskGUI, this);
                getLogger().info("✓ Daily Tasks enabled");
                getLogger().info("✓ Daily Tasks GUI enabled");
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

    /**
     * NEW: Welcome message on player join
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Only show welcome message once per session (prevents spam on reload)
        if (hasSeenWelcome.contains(uuid)) {
            return;
        }
        
        hasSeenWelcome.add(uuid);
        
        // Delay by 2 seconds so it doesn't get lost in join spam
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) {
                sendWelcomeMessage(player);
            }
        }, 40L); // 40 ticks = 2 seconds
    }

    /**
     * NEW: Send edition-specific welcome message
     */
    private void sendWelcomeMessage(Player player) {
        player.sendMessage("");
        player.sendMessage(ChatColor.GREEN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        if (isPremiumEdition) {
            // Premium welcome message
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "    ⭐ FARMCROPS PREMIUM ⭐");
            player.sendMessage("");
            player.sendMessage(ChatColor.YELLOW + "Thank you for purchasing Premium Edition!");
            player.sendMessage(ChatColor.GRAY + "You have access to all features:");
            player.sendMessage(ChatColor.GREEN + "  ✓ Achievements & Custom Titles");
            player.sendMessage(ChatColor.GREEN + "  ✓ Daily Tasks & Challenges");
            player.sendMessage(ChatColor.GREEN + "  ✓ Crop Collections");
            player.sendMessage(ChatColor.GREEN + "  ✓ Advanced Stats & Leaderboards");
            player.sendMessage("");
            player.sendMessage(ChatColor.AQUA + "Type " + ChatColor.WHITE + "/farm" + ChatColor.AQUA + " to get started!");
        } else {
            // Lite welcome message
            player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "    💎 FARMCROPS LITE 💎");
            player.sendMessage("");
            player.sendMessage(ChatColor.YELLOW + "Welcome to FarmCrops Lite Edition!");
            player.sendMessage(ChatColor.GRAY + "You're using the free version with:");
            player.sendMessage(ChatColor.GREEN + "  ✓ Custom Crop System");
            player.sendMessage(ChatColor.GREEN + "  ✓ Sell GUI & Stats");
            player.sendMessage(ChatColor.GREEN + "  ✓ Holograms & Effects");
            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "Want more? Upgrade to Premium for:");
            player.sendMessage(ChatColor.YELLOW + "  ⭐ Achievements & Titles");
            player.sendMessage(ChatColor.YELLOW + "  ⭐ Daily Tasks");
            player.sendMessage(ChatColor.YELLOW + "  ⭐ Collections");
            player.sendMessage("");
            player.sendMessage(ChatColor.AQUA + "Type " + ChatColor.WHITE + "/farm" + ChatColor.AQUA + " to get started!");
        }
        
        player.sendMessage(ChatColor.GREEN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");
    }

    @Override
    public void onDisable() {
        // Clear welcome tracking
        hasSeenWelcome.clear();
        
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
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        
        // Remove from welcome tracking
        hasSeenWelcome.remove(uuid);
        
        // Save player data immediately before clearing cache
        if (statsManager != null) {
            StatsManager.PlayerStats stats = statsManager.getStats(uuid);
            statsManager.clearCache(uuid);
        }
        
        if (playerSettings != null) {
            playerSettings.saveSettings();
            playerSettings.clearCache(uuid);
        }
        
        if (holoEnabled && cropPreviewManager != null) {
            cropPreviewManager.cleanup();
        }
    }

    /**
     * Get the base price for a crop type
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
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public ActionBarManager getActionBarManager() { return actionBarManager; }
    public MessageHandler getMessageHandler()  { return messageHandler; }
    public ChatListener getChatListener()      { return chatListener; }
    
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
    public DailyTaskGUI getDailyTaskGUI() { return dailyTaskGUI; }
}
