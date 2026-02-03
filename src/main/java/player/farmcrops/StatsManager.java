package player.farmcrops;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages player statistics for crop harvesting.
 * v0.6.0: Added per-crop tracking and leaderboard support.
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
     * Record a harvest for a player.
     * v0.6.0: Now also tracks per-crop-type stats.
     */
    public void recordHarvest(Player player, Material cropType, String tier, double weight, double value) {
        PlayerStats stats = getStats(player.getUniqueId());

        stats.totalHarvests++;
        stats.totalEarnings += value;
        stats.displayName = player.getName();

        // Tier counts
        switch (tier.toLowerCase()) {
            case "common":    stats.commonHarvests++;    break;
            case "rare":      stats.rareHarvests++;      break;
            case "epic":      stats.epicHarvests++;      break;
            case "legendary": stats.legendaryHarvests++; break;
            case "mythic":    stats.mythicHarvests++;    break;
        }

        // Per-crop tracking
        String cropKey = cropType.name();
        stats.cropHarvests.merge(cropKey, 1, Integer::sum);
        stats.cropEarnings.merge(cropKey, value, Double::sum);

        // Best drop
        if (value > stats.bestDropValue) {
            stats.bestDropValue = value;
            stats.bestDropTier = tier;
            stats.bestDropWeight = weight;
            stats.bestDropCrop = cropKey;
        }

        // Heaviest crop
        if (weight > stats.heaviestWeight) {
            stats.heaviestWeight = weight;
            stats.heaviestTier = tier;
            stats.heaviestCrop = cropKey;
        }

        saveStats(player.getUniqueId(), stats);
    }

    public PlayerStats getStats(UUID playerId) {
        if (cachedStats.containsKey(playerId)) {
            return cachedStats.get(playerId);
        }
        PlayerStats stats = loadStats(playerId);
        cachedStats.put(playerId, stats);
        return stats;
    }

    /**
     * Get total harvests for a player.
     */
    public int getTotalHarvests(UUID playerId) {
        return getStats(playerId).totalHarvests;
    }

    /**
     * Get total earnings for a player.
     */
    public double getTotalEarnings(UUID playerId) {
        return getStats(playerId).totalEarnings;
    }

    /**
     * Get epic tier harvests for a player.
     */
    public int getEpicHarvests(UUID playerId) {
        return getStats(playerId).epicHarvests;
    }

    /**
     * Get legendary tier harvests for a player.
     */
    public int getLegendaryHarvests(UUID playerId) {
        return getStats(playerId).legendaryHarvests;
    }

    /**
     * Get harvests for a specific crop type.
     */
    public int getCropHarvests(UUID playerId, String cropType) {
        return getStats(playerId).cropHarvests.getOrDefault(cropType, 0);
    }

    /**
     * Get leaderboard sorted by the configured criteria.
     * Scans all saved player data files.
     */
    public List<LeaderboardEntry> getLeaderboard(int limit) {
        String sortBy = plugin.getConfig().getString("leaderboard.sort-by", "earnings");

        // Load all player stats (cached + any on disk not yet cached)
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String uuidStr = file.getName().replace(".yml", "");
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    if (!cachedStats.containsKey(uuid)) {
                        cachedStats.put(uuid, loadStats(uuid));
                    }
                } catch (IllegalArgumentException e) {
                    // Skip malformed filenames
                }
            }
        }

        List<LeaderboardEntry> board = new ArrayList<>();
        for (Map.Entry<UUID, PlayerStats> entry : cachedStats.entrySet()) {
            PlayerStats s = entry.getValue();
            if (s.totalHarvests == 0) continue;
            board.add(new LeaderboardEntry(
                entry.getKey(),
                s.displayName != null ? s.displayName : entry.getKey().toString(),
                s.totalEarnings,
                s.totalHarvests
            ));
        }

        // Sort
        if ("harvests".equalsIgnoreCase(sortBy)) {
            board.sort((a, b) -> Integer.compare(b.harvests, a.harvests));
        } else {
            board.sort((a, b) -> Double.compare(b.earnings, a.earnings));
        }

        return board.subList(0, Math.min(limit, board.size()));
    }

    // ─────────────────────────────────────────────
    // Load / Save
    // ─────────────────────────────────────────────

    private PlayerStats loadStats(UUID playerId) {
        File file = new File(dataFolder, playerId.toString() + ".yml");
        if (!file.exists()) return new PlayerStats();

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        PlayerStats stats = new PlayerStats();

        stats.displayName = config.getString("display-name", null);
        stats.totalHarvests = config.getInt("total-harvests", 0);
        stats.totalEarnings = config.getDouble("total-earnings", 0.0);
        stats.commonHarvests = config.getInt("common-harvests", 0);
        stats.rareHarvests = config.getInt("rare-harvests", 0);
        stats.epicHarvests = config.getInt("epic-harvests", 0);
        stats.legendaryHarvests = config.getInt("legendary-harvests", 0);
        stats.mythicHarvests = config.getInt("mythic-harvests", 0);
        stats.bestDropValue = config.getDouble("best-drop-value", 0.0);
        stats.bestDropTier = config.getString("best-drop-tier", "none");
        stats.bestDropWeight = config.getDouble("best-drop-weight", 0.0);
        stats.bestDropCrop = config.getString("best-drop-crop", "none");
        stats.heaviestWeight = config.getDouble("heaviest-weight", 0.0);
        stats.heaviestTier = config.getString("heaviest-tier", "none");
        stats.heaviestCrop = config.getString("heaviest-crop", "none");

        // Load per-crop maps
        if (config.contains("crop-harvests")) {
            for (String key : config.getConfigurationSection("crop-harvests").getKeys(false)) {
                stats.cropHarvests.put(key, config.getInt("crop-harvests." + key, 0));
            }
        }
        if (config.contains("crop-earnings")) {
            for (String key : config.getConfigurationSection("crop-earnings").getKeys(false)) {
                stats.cropEarnings.put(key, config.getDouble("crop-earnings." + key, 0.0));
            }
        }

        return stats;
    }

    private void saveStats(UUID playerId, PlayerStats stats) {
        File file = new File(dataFolder, playerId.toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        config.set("display-name", stats.displayName);
        config.set("total-harvests", stats.totalHarvests);
        config.set("total-earnings", stats.totalEarnings);
        config.set("common-harvests", stats.commonHarvests);
        config.set("rare-harvests", stats.rareHarvests);
        config.set("epic-harvests", stats.epicHarvests);
        config.set("legendary-harvests", stats.legendaryHarvests);
        config.set("mythic-harvests", stats.mythicHarvests);
        config.set("best-drop-value", stats.bestDropValue);
        config.set("best-drop-tier", stats.bestDropTier);
        config.set("best-drop-weight", stats.bestDropWeight);
        config.set("best-drop-crop", stats.bestDropCrop);
        config.set("heaviest-weight", stats.heaviestWeight);
        config.set("heaviest-tier", stats.heaviestTier);
        config.set("heaviest-crop", stats.heaviestCrop);

        // Save per-crop maps
        for (Map.Entry<String, Integer> e : stats.cropHarvests.entrySet()) {
            config.set("crop-harvests." + e.getKey(), e.getValue());
        }
        for (Map.Entry<String, Double> e : stats.cropEarnings.entrySet()) {
            config.set("crop-earnings." + e.getKey(), e.getValue());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save stats for " + playerId + ": " + e.getMessage());
        }
    }

    public void clearCache(UUID playerId) {
        cachedStats.remove(playerId);
    }

    public void saveAll() {
        for (Map.Entry<UUID, PlayerStats> entry : cachedStats.entrySet()) {
            saveStats(entry.getKey(), entry.getValue());
        }
    }

    // ─────────────────────────────────────────────
    // Data classes
    // ─────────────────────────────────────────────

    public static class PlayerStats {
        public String displayName = null;
        public int totalHarvests = 0;
        public double totalEarnings = 0.0;
        public int commonHarvests = 0;
        public int rareHarvests = 0;
        public int epicHarvests = 0;
        public int legendaryHarvests = 0;
        public int mythicHarvests = 0;
        public double bestDropValue = 0.0;
        public String bestDropTier = "none";
        public double bestDropWeight = 0.0;
        public String bestDropCrop = "none";
        public double heaviestWeight = 0.0;
        public String heaviestTier = "none";
        public String heaviestCrop = "none";

        // Per-crop tracking (v0.6.0)
        public Map<String, Integer> cropHarvests = new HashMap<>();
        public Map<String, Double> cropEarnings = new HashMap<>();
    }

    public static class LeaderboardEntry {
        public final UUID uuid;
        public final String name;
        public final double earnings;
        public final int harvests;

        public LeaderboardEntry(UUID uuid, String name, double earnings, int harvests) {
            this.uuid = uuid;
            this.name = name;
            this.earnings = earnings;
            this.harvests = harvests;
        }
    }
}
