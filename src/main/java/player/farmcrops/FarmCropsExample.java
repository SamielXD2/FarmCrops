package player.farmcrops;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Example of how to use PluginConfig in your main plugin class
 */
public class FarmCropsExample extends JavaPlugin {
    
    @Override
    public void onEnable() {
        // Log edition information
        getLogger().info("Loading FarmCrops " + PluginConfig.EDITION + " v" + PluginConfig.VERSION);
        
        // Register features based on edition
        registerCoreFeatures();
        
        // Only register premium features if enabled
        if (PluginConfig.ACHIEVEMENTS_ENABLED) {
            registerAchievements();
            getLogger().info("✓ Achievements enabled");
        } else {
            getLogger().info("✗ Achievements locked (Premium only)");
        }
        
        if (PluginConfig.DAILY_TASKS_ENABLED) {
            registerDailyTasks();
            getLogger().info("✓ Daily Tasks enabled");
        } else {
            getLogger().info("✗ Daily Tasks locked (Premium only)");
        }
        
        if (PluginConfig.COLLECTIONS_ENABLED) {
            registerCollections();
            getLogger().info("✓ Collections enabled");
        } else {
            getLogger().info("✗ Collections locked (Premium only)");
        }
        
        if (PluginConfig.AUTOSAVE_ENABLED) {
            startAutoSave();
            getLogger().info("✓ Auto-save enabled");
        } else {
            getLogger().info("✗ Auto-save locked (Premium only)");
        }
        
        getLogger().info("FarmCrops " + PluginConfig.EDITION + " loaded successfully!");
    }
    
    private void registerCoreFeatures() {
        // Core features available in both editions
        // - Basic farming
        // - Crop selling
        // - Statistics
        // - Leaderboards
    }
    
    private void registerAchievements() {
        // Register achievement system
        // This code only runs in Premium edition
    }
    
    private void registerDailyTasks() {
        // Register daily task system
        // This code only runs in Premium edition
    }
    
    private void registerCollections() {
        // Register collections system
        // This code only runs in Premium edition
    }
    
    private void startAutoSave() {
        // Start automatic saving
        // This code only runs in Premium edition
    }
}
