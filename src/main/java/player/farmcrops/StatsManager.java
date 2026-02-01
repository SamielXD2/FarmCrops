package player.farmcrops;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages player statistics for crop harvesting
 * Tracks total harvests, earnings, and best drops per player
 */
public class StatsManager {
    
    private final FarmCrops plugin;
    private final File dataFolder;
    private final Map<UUID, PlayerStats> cachedStats = new HashMap<>();
    
    public StatsManager(FarmCrops plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }
    
    /**
     * Record a harvest for a player
     */
    public void recordHarvest(Player player, String tier, double weight, double value) {
        PlayerStats stats = getStats(player.getUniqueId());
        
        stats.totalHarvests++;
        stats.totalEarnings += value;
        
        // Track tier-specific counts
        switch (tier.toLowerCase()) {
            case "common":
                stats.commonHarvests++;
                break;
            case "rare":
                stats.rareHarvests++;
                break;
            case "epic":
                stats.epicHarvests++;
                break;
            case "legendary":
                stats.legendaryHarvests++;
                break;
        }
        
        // Check if this is their best drop
        if (value > stats.bestDropValue) {
            stats.bestDropValue = value;
            stats.bestDropTier = tier;
            stats.bestDropWeight = weight;
        }
        
        // Check if this is their heaviest crop
        if (weight > stats.heaviestWeight) {
            stats.heaviestWeight = weight;
            stats.heaviestTier = tier;
        }
        
        // Save to disk
        saveStats(player.getUniqueId(), stats);
    }
    
    /**
     * Get stats for a player (loads from disk if not cached)
     */
    public PlayerStats getStats(UUID playerId) {
        if (cachedStats.containsKey(playerId)) {
            return cachedStats.get(playerId);
        }
        
        PlayerStats stats = loadStats(playerId);
        cachedStats.put(playerId, stats);
        return stats;
    }
    
    /**
     * Load stats from disk
     */
    private PlayerStats loadStats(UUID playerId) {
        File file = new File(dataFolder, playerId.toString() + ".yml");
        
        if (!file.exists()) {
            return new PlayerStats();
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        PlayerStats stats = new PlayerStats();
        stats.totalHarvests = config.getInt("total-harvests", 0);
        stats.totalEarnings = config.getDouble("total-earnings", 0.0);
        stats.commonHarvests = config.getInt("common-harvests", 0);
        stats.rareHarvests = config.getInt("rare-harvests", 0);
        stats.epicHarvests = config.getInt("epic-harvests", 0);
        stats.legendaryHarvests = config.getInt("legendary-harvests", 0);
        stats.bestDropValue = config.getDouble("best-drop-value", 0.0);
        stats.bestDropTier = config.getString("best-drop-tier", "none");
        stats.bestDropWeight = config.getDouble("best-drop-weight", 0.0);
        stats.heaviestWeight = config.getDouble("heaviest-weight", 0.0);
        stats.heaviestTier = config.getString("heaviest-tier", "none");
        
        return stats;
    }
    
    /**
     * Save stats to disk
     */
    private void saveStats(UUID playerId, PlayerStats stats) {
        File file = new File(dataFolder, playerId.toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();
        
        config.set("total-harvests", stats.totalHarvests);
        config.set("total-earnings", stats.totalEarnings);
        config.set("common-harvests", stats.commonHarvests);
        config.set("rare-harvests", stats.rareHarvests);
        config.set("epic-harvests", stats.epicHarvests);
        config.set("legendary-harvests", stats.legendaryHarvests);
        config.set("best-drop-value", stats.bestDropValue);
        config.set("best-drop-tier", stats.bestDropTier);
        config.set("best-drop-weight", stats.bestDropWeight);
        config.set("heaviest-weight", stats.heaviestWeight);
        config.set("heaviest-tier", stats.heaviestTier);
        
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save stats for player " + playerId + ": " + e.getMessage());
        }
    }
    
    /**
     * Clear cached stats for a player (called on logout)
     */
    public void clearCache(UUID playerId) {
        cachedStats.remove(playerId);
    }
    
    /**
     * Save all cached stats to disk
     */
    public void saveAll() {
        for (Map.Entry<UUID, PlayerStats> entry : cachedStats.entrySet()) {
            saveStats(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Player statistics data class
     */
    public static class PlayerStats {
        public int totalHarvests = 0;
        public double totalEarnings = 0.0;
        public int commonHarvests = 0;
        public int rareHarvests = 0;
        public int epicHarvests = 0;
        public int legendaryHarvests = 0;
        public double bestDropValue = 0.0;
        public String bestDropTier = "none";
        public double bestDropWeight = 0.0;
        public double heaviestWeight = 0.0;
        public String heaviestTier = "none";
    }
}
