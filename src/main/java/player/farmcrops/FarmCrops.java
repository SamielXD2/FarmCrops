package player.farmcrops;

import net.milkman.vault.economy.Economy;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;

public class FarmCrops extends JavaPlugin {

    private static FarmCrops instance;
    private Economy economy;

    @Override
    public void onEnable() {
        instance = this;

        // Load config (creates config.yml on first run)
        saveDefaultConfig();

        // Hook into Vault's economy
        if (!setupEconomy()) {
            getLogger().severe("Vault not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getLogger().info("Vault economy hooked successfully.");

        // Register the harvest listener
        getServer().getPluginManager().registerEvents(new CropListener(this), this);

        // Register the /sellcrops command
        getCommand("sellcrops").setExecutor(new SellCommand(this));

        getLogger().info("FarmCrops enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("FarmCrops disabled.");
    }

    // --- Vault Setup ---
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getProvider(Economy.class);
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
