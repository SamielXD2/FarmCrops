package player.farmcrops;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;

public class FarmCrops extends JavaPlugin {

    private static FarmCrops instance;
    private Economy economy;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("========================================");
        getLogger().info("  FarmCrops is starting up...");
        getLogger().info("========================================");

        // Load config (creates config.yml on first run)
        saveDefaultConfig();
        getLogger().info("✓ Configuration loaded successfully");

        // Hook into Vault's economy
        if (!setupEconomy()) {
            getLogger().severe("========================================");
            getLogger().severe("  ✗ VAULT NOT FOUND!");
            getLogger().severe("  Please install Vault to use FarmCrops");
            getLogger().severe("  Download: https://www.spigotmc.org/resources/vault.34315/");
            getLogger().severe("========================================");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getLogger().info("✓ Vault economy hooked successfully");

        // Register the harvest listener
        getServer().getPluginManager().registerEvents(new CropListener(this), this);
        getLogger().info("✓ Crop harvest listener registered");

        // Register the /sellcrops command
        getCommand("sellcrops").setExecutor(new SellCommand(this));
        getLogger().info("✓ /sellcrops command registered");

        getLogger().info("========================================");
        getLogger().info("  ✓ FarmCrops enabled successfully!");
        getLogger().info("  Tracked crops: Wheat, Carrot, Potato, Beetroot, Melon");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("========================================");
        getLogger().info("  FarmCrops has been disabled");
        getLogger().info("========================================");
    }

    // --- Vault Setup ---
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    // --- Getters ---
    public static FarmCrops getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }
}
