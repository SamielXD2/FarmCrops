package player.farmcrops;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class FarmCrops extends JavaPlugin implements Listener {

    private static FarmCrops instance;
    private Economy economy;
    private SellGUI sellGUI;
    private HoloManager holoManager;
    private StatsManager statsManager;
    private SettingsGUI settingsGUI;
    private boolean holoEnabled = false;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("========================================");
        getLogger().info("========================================");
        getLogger().info("       FARMCROPS v0.6.0");
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

        // Commands
        getCommand("sellcrops").setExecutor(new SellCommand(this));
        getCommand("farmstats").setExecutor(new StatsCommand(this));
        getCommand("farmtop").setExecutor(new TopCommand(this));
        getCommand("farmsettings").setExecutor(new SettingsCommand(this));
        getCommand("farmreload").setExecutor(new ReloadCommand(this));
        getLogger().info("✓ Commands registered: /sellcrops, /farmstats, /farmtop, /farmsettings, /farmreload");
        getLogger().info("");

        // PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderProvider(this).register();
            getLogger().info("✓ PlaceholderAPI integration active");
        } else {
            getLogger().info("  PlaceholderAPI not found — placeholder support disabled");
        }
        getLogger().info("");

        // DecentHolograms
        if (Bukkit.getPluginManager().getPlugin("DecentHolograms") != null) {
            holoManager = new HoloManager(this);
            holoEnabled = true;
            getLogger().info("✓ DecentHolograms integration active");
            getLogger().info("  - Harvest Flash: " + getConfig().getBoolean("holograms.harvest-flash"));
            getLogger().info("  - Growing Cursor: " + getConfig().getBoolean("holograms.growing-cursor"));
            getLogger().info("  - Particles: " + getConfig().getBoolean("holograms.particles"));
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
        getLogger().info("  - farmcrops.admin    — grants all above");
        getLogger().info("");

        getLogger().info("========================================");
        getLogger().info("  ✓✓✓ FARMCROPS v0.6.0 ENABLED ✓✓✓");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        if (statsManager != null) {
            getLogger().info("Saving player statistics...");
            statsManager.saveAll();
        }

        getLogger().info("========================================");
        getLogger().info("  FarmCrops v0.6.0 shutting down...");
        getLogger().info("========================================");
    }

    /**
     * Clear stats cache when a player leaves to prevent memory leaks.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (statsManager != null) {
            statsManager.clearCache(event.getPlayer().getUniqueId());
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
    public HoloManager getHoloManager()        { return holoManager; }
    public StatsManager getStatsManager()      { return statsManager; }
    public SettingsGUI getSettingsGUI()         { return settingsGUI; }
}
