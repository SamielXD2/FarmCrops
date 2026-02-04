package player.farmcrops;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * FIXED Title System - Uses Scoreboard Teams (ACTUALLY WORKS!)
 * 
 * How it works:
 * 1. Creates a scoreboard team for each player
 * 2. Sets the team prefix to their selected title
 * 3. Title appears above player's head AND in tab list
 * 4. No PlaceholderAPI needed!
 * 
 * This is how ALL title plugins work (NametagEdit, PrefiX, etc.)
 */
public class TitleSystem {
    
    private final FarmCrops plugin;
    private final Map<UUID, String> playerTitles = new HashMap<>();
    
    public TitleSystem(FarmCrops plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Set a player's title (appears above head and in tab)
     */
    public void setTitle(Player player, String title) {
        UUID uuid = player.getUniqueId();
        
        // Save title
        playerTitles.put(uuid, title);
        
        // Update their nametag using scoreboard teams
        updateNametag(player, title);
    }
    
    /**
     * Remove a player's title
     */
    public void removeTitle(Player player) {
        UUID uuid = player.getUniqueId();
        playerTitles.remove(uuid);
        
        // Remove from team
        updateNametag(player, "");
    }
    
    /**
     * Get a player's current title
     */
    public String getTitle(UUID uuid) {
        return playerTitles.getOrDefault(uuid, "");
    }
    
    /**
     * Update player's nametag using Scoreboard Teams
     * This makes the title appear above their head and in tab list
     */
    private void updateNametag(Player player, String title) {
        // Get or create main scoreboard
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        
        // Create team name (unique per player, max 16 chars)
        String teamName = "fc_" + player.getName().substring(0, Math.min(player.getName().length(), 12));
        
        // Get or create team
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        
        // Add player to team if not already in it
        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }
        
        // Set prefix (title) - appears BEFORE player name
        // Format: [Title] PlayerName
        if (title != null && !title.isEmpty()) {
            String coloredTitle = ChatColor.translateAlternateColorCodes('&', title);
            
            // Prefix can only be 16 characters max in older versions
            // For modern versions (1.13+) it can be longer
            if (coloredTitle.length() > 16) {
                coloredTitle = coloredTitle.substring(0, 16);
            }
            
            team.setPrefix(coloredTitle + " ");
        } else {
            team.setPrefix("");
        }
        
        // Optional: Set suffix (appears AFTER player name)
        team.setSuffix("");
        
        // Update for all online players so they see the change
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.setScoreboard(scoreboard);
        }
    }
    
    /**
     * Apply title to player when they join
     */
    public void onPlayerJoin(Player player) {
        UUID uuid = player.getUniqueId();
        
        // If they have a saved title, apply it
        if (playerTitles.containsKey(uuid)) {
            updateNametag(player, playerTitles.get(uuid));
        }
        
        // Make sure they see everyone else's titles
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
    
    /**
     * Clean up when player leaves
     */
    public void onPlayerQuit(Player player) {
        // Don't remove from map - keep their title saved
        // Just remove from team to free memory
        String teamName = "fc_" + player.getName().substring(0, Math.min(player.getName().length(), 12));
        Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(teamName);
        
        if (team != null) {
            team.removeEntry(player.getName());
            // Don't unregister team - reuse it when they rejoin
        }
    }
    
    /**
     * Get all available titles (from TitleManager or config)
     */
    public Map<String, String> getAvailableTitles() {
        Map<String, String> titles = new HashMap<>();
        
        // Example titles - you can load these from config or TitleManager
        titles.put("novice", "&7[Novice Farmer]");
        titles.put("farmer", "&a[Farmer]");
        titles.put("expert", "&2[Expert Farmer]");
        titles.put("master", "&6[Master Farmer]");
        titles.put("legend", "&c[Legendary Farmer]");
        titles.put("golden", "&e⭐ &6[Golden Farmer] &e⭐");
        
        return titles;
    }
    
    /**
     * Check if player has unlocked a title (via achievements)
     */
    public boolean hasUnlocked(UUID uuid, String titleId) {
        // Check with achievement manager
        if (plugin.getAchievementManager() != null) {
            // Example: Check if they've earned the achievement
            // You'd implement this based on your achievement system
            return true; // For now, all unlocked
        }
        return true;
    }
    
    /**
     * Save all titles to file (call this periodically)
     */
    public void saveAll() {
        // TODO: Save playerTitles map to a file
        // You can use YAML or JSON here
    }
    
    /**
     * Load all titles from file
     */
    public void loadAll() {
        // TODO: Load playerTitles map from file
    }
}
