package player.farmcrops;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * FarmCrops Scoreboard Manager
 * Displays live farming stats on the side of the screen
 * 
 * Features:
 * - Real-time crop count
 * - Total earnings display
 * - Current session stats
 * - Auto-updates every second
 * - Per-player toggle support
 */
public class ScoreboardManager {
    
    private final FarmCrops plugin;
    private final Map<UUID, Scoreboard> playerBoards = new HashMap<>();
    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0.00");
    private final DecimalFormat numberFormat = new DecimalFormat("#,###");
    
    // Session tracking
    private final Map<UUID, Integer> sessionCrops = new HashMap<>();
    private final Map<UUID, Double> sessionEarnings = new HashMap<>();
    
    public ScoreboardManager(FarmCrops plugin) {
        this.plugin = plugin;
        
        // Start auto-update task (updates every 20 ticks = 1 second)
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllScoreboards, 20L, 20L);
    }
    
    /**
     * Show scoreboard for a player
     */
    public void showScoreboard(Player player) {
        // Check if player wants to see scoreboard
        PlayerSettings.PlayerPreferences prefs = plugin.getPlayerSettings().getPreferences(player.getUniqueId());
        if (!prefs.showScoreboard) {
            return;
        }
        
        try {
            Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective objective = board.registerNewObjective("farmcrops", "dummy", 
                ChatColor.GOLD + "⌁" + ChatColor.BOLD + " FarmCrops");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            
            updateScoreboardContent(player, objective);
            
            player.setScoreboard(board);
            playerBoards.put(player.getUniqueId(), board);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create scoreboard for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Hide scoreboard for a player
     */
    public void hideScoreboard(Player player) {
        try {
            playerBoards.remove(player.getUniqueId());
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to hide scoreboard for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Toggle scoreboard for a player
     */
    public void toggleScoreboard(Player player) {
        PlayerSettings.PlayerPreferences prefs = plugin.getPlayerSettings().getPreferences(player.getUniqueId());
        
        if (prefs.showScoreboard) {
            showScoreboard(player);
        } else {
            hideScoreboard(player);
        }
    }
    
    /**
     * Update scoreboard content for a specific player
     */
    private void updateScoreboardContent(Player player, Objective objective) {
        UUID uuid = player.getUniqueId();
        
        try {
            // Clear existing scores
            for (String entry : objective.getScoreboard().getEntries()) {
                objective.getScoreboard().resetScores(entry);
            }
            
            // Get stats
            StatsManager.PlayerStats stats = plugin.getStatsManager().getStats(uuid);
            int sessionCropCount = sessionCrops.getOrDefault(uuid, 0);
            double sessionMoney = sessionEarnings.getOrDefault(uuid, 0.0);
            
            int line = 15;
            
            // Header
            setScore(objective, ChatColor.GRAY + "━━━━━━━━━━━━━━", line--);
            
            // Session Stats
            setScore(objective, ChatColor.YELLOW + "Session:", line--);
            setScore(objective, ChatColor.WHITE + "  Crops: " + ChatColor.GREEN + numberFormat.format(sessionCropCount), line--);
            setScore(objective, ChatColor.WHITE + "  Earned: " + ChatColor.GOLD + "$" + moneyFormat.format(sessionMoney), line--);
            setScore(objective, "", line--); // Spacer
            
            // Total Stats
            setScore(objective, ChatColor.AQUA + "Total Stats:", line--);
            setScore(objective, ChatColor.WHITE + "  Crops: " + ChatColor.GREEN + numberFormat.format(stats.totalHarvests), line--);
            setScore(objective, ChatColor.WHITE + "  Money: " + ChatColor.GOLD + "$" + moneyFormat.format(stats.totalEarnings), line--);
            setScore(objective, "", line--); // Spacer
            
            // Best Crop
            if (stats.bestDropValue > 0) {
                setScore(objective, ChatColor.LIGHT_PURPLE + "Best Crop:", line--);
                setScore(objective, ChatColor.WHITE + "  " + ChatColor.GOLD + "$" + moneyFormat.format(stats.bestDropValue), line--);
            }
            
            // Footer
            setScore(objective, ChatColor.GRAY + "━━━━━━━━━━━━━━", line--);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error updating scoreboard for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Set a score on the scoreboard
     */
    private void setScore(Objective objective, String text, int score) {
        try {
            Score s = objective.getScore(text);
            s.setScore(score);
        } catch (Exception e) {
            // Ignore - sometimes scores fail due to duplicate entries
        }
    }
    
    /**
     * Update all active scoreboards
     */
    private void updateAllScoreboards() {
        for (Map.Entry<UUID, Scoreboard> entry : playerBoards.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                try {
                    Objective obj = entry.getValue().getObjective(DisplaySlot.SIDEBAR);
                    if (obj != null) {
                        updateScoreboardContent(player, obj);
                    }
                } catch (Exception e) {
                    // Player might have logged off, remove them
                    playerBoards.remove(entry.getKey());
                }
            } else {
                // Player offline, remove scoreboard
                playerBoards.remove(entry.getKey());
            }
        }
    }
    
    /**
     * Record a crop harvest (updates session stats)
     */
    public void recordHarvest(Player player, double earnings) {
        UUID uuid = player.getUniqueId();
        
        // Update session stats
        sessionCrops.put(uuid, sessionCrops.getOrDefault(uuid, 0) + 1);
        sessionEarnings.put(uuid, sessionEarnings.getOrDefault(uuid, 0.0) + earnings);
        
        // If scoreboard is active, force update
        if (playerBoards.containsKey(uuid)) {
            Scoreboard board = playerBoards.get(uuid);
            Objective obj = board.getObjective(DisplaySlot.SIDEBAR);
            if (obj != null) {
                updateScoreboardContent(player, obj);
            }
        }
    }
    
    /**
     * Reset session stats for a player
     */
    public void resetSession(Player player) {
        UUID uuid = player.getUniqueId();
        sessionCrops.remove(uuid);
        sessionEarnings.remove(uuid);
    }
    
    /**
     * Clear player data when they leave
     */
    public void clearPlayer(UUID uuid) {
        playerBoards.remove(uuid);
        sessionCrops.remove(uuid);
        sessionEarnings.remove(uuid);
    }
    
    /**
     * Shutdown cleanup
     */
    public void shutdown() {
        playerBoards.clear();
        sessionCrops.clear();
        sessionEarnings.clear();
    }
    
    /**
     * Reload scoreboards for all online players
     */
    public void reloadAllScoreboards() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerSettings.PlayerPreferences prefs = plugin.getPlayerSettings().getPreferences(player.getUniqueId());
            if (prefs.showScoreboard && playerBoards.containsKey(player.getUniqueId())) {
                // Refresh existing scoreboard
                hideScoreboard(player);
                showScoreboard(player);
            }
        }
    }
}
