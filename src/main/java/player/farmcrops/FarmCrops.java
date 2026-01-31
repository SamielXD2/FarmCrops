package player.farmcrops;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class FarmCrops extends JavaPlugin {

    private static FarmCrops instance;
    private Economy economy;
    private SellGUI sellGUI;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("========================================");
        getLogger().info("========================================");
        getLogger().info("       FARMCROPS v0.4.0");
        getLogger().info("  Weight-Based Crop Economy System");
        getLogger().info("========================================");
        getLogger().info("========================================");
        getLogger().info("");
        getLogger().info("Starting initialization...");
        getLogger().info("");

        saveDefaultConfig();
        getLogger().info("✓ Configuration file loaded");
        getLogger().info("  - Weight Range: " + getConfig().getDouble("weight.min") + "kg - " + getConfig().getDouble("weight.max") + "kg");
        getLogger().info("  - Base Price: $" + getConfig().getDouble("prices.default") + " per kg");
        getLogger().info("  - Tier System: 4 tiers configured");
        getLogger().info("");

        getLogger().info("Searching for Vault...");
        if (!setupEconomy()) {
            getLogger().severe("========================================");
            getLogger().severe("  ✗✗✗ CRITICAL ERROR ✗✗✗");
            getLogger().severe("  VAULT NOT FOUND!");
            getLogger().severe("========================================");
            getLogger().severe("  FarmCrops requires Vault to function!");
            getLogger().severe("  Please install Vault from:");
            getLogger().severe("  https://www.spigotmc.org/resources/vault.34315/");
            getLogger().severe("========================================");
            getLogger().severe("  DISABLING FARMCROPS...");
            getLogger().severe("========================================");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getLogger().info("✓ Vault found and hooked!");
        getLogger().info("  - Economy Provider: " + economy.getName());
        getLogger().info("");

        getLogger().info("Registering event listeners...");
        getServer().getPluginManager().registerEvents(new CropListener(this), this);
        getLogger().info("✓ Crop harvest listener registered");
        getLogger().info("  - Tracking: Wheat, Carrot, Potato, Beetroot, Melon");
        getLogger().info("");

        // Initialize and register SellGUI
        sellGUI = new SellGUI(this);
        getServer().getPluginManager().registerEvents(sellGUI, this);
        getLogger().info("✓ Sell GUI system initialized");
        getLogger().info("");

        getLogger().info("Registering commands...");
        getCommand("sellcrops").setExecutor(new SellCommand(this));
        getLogger().info("✓ /sellcrops command registered");
        getLogger().info("");

        // Check for PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("PlaceholderAPI found! Registering placeholders...");
            new PlaceholderProvider(this).register();
            getLogger().info("✓ PlaceholderAPI integration active");
            getLogger().info("  - %farmcrops_price%");
            getLogger().info("  - %farmcrops_tier%");
            getLogger().info("  - %farmcrops_weight%");
            getLogger().info("  - %farmcrops_crop%");
            getLogger().info("");
        } else {
            getLogger().info("PlaceholderAPI not found - placeholder support disabled");
            getLogger().info("");
        }

        getLogger().info("========================================");
        getLogger().info("  ✓✓✓ FARMCROPS ENABLED ✓✓✓");
        getLogger().info("========================================");
        getLogger().info("  Version: 0.4.0");
        getLogger().info("  Author: Player");
        getLogger().info("  All systems operational!");
        getLogger().info("========================================");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("========================================");
        getLogger().info("  FarmCrops v0.4.0 shutting down...");
        getLogger().info("  Thanks for using FarmCrops!");
        getLogger().info("========================================");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp =
            getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public static FarmCrops getInstance() { 
        return instance; 
    }
    
    public Economy getEconomy() { 
        return economy; 
    }
    
    public SellGUI getSellGUI() {
        return sellGUI;
    }
}
