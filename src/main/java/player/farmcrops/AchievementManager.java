package player.farmcrops;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Achievement/Milestone System
 * Rewards players for reaching harvest milestones
 */
public class AchievementManager {
    
    private final FarmCrops plugin;
    private final Map<UUID, Set<String>> playerAchievements;
    
    public AchievementManager(FarmCrops plugin) {
        this.plugin = plugin;
        this.playerAchievements = new HashMap<>();
        loadAchievements();
    }
    
    private void loadAchievements() {
        // Load player achievements from data file
        // TODO: Implement persistence
    }
    
    public void checkAchievements(Player player) {
        UUID uuid = player.getUniqueId();
        StatsManager statsManager = plugin.getStatsManager();
        
        int totalHarvests = statsManager.getTotalHarvests(uuid);
        double totalEarnings = statsManager.getTotalEarnings(uuid);
        
        // Check harvest milestones
        checkMilestone(player, "first_harvest", totalHarvests >= 1, 
            "First Harvest", "Harvest your first crop!", 10);
        checkMilestone(player, "hundred_harvest", totalHarvests >= 100, 
            "Century Farmer", "Harvest 100 crops!", 100);
        checkMilestone(player, "thousand_harvest", totalHarvests >= 1000, 
            "Master Farmer", "Harvest 1,000 crops!", 500);
        checkMilestone(player, "ten_thousand_harvest", totalHarvests >= 10000, 
            "Legendary Farmer", "Harvest 10,000 crops!", 2000);
            
        // Check earnings milestones
        checkMilestone(player, "first_thousand", totalEarnings >= 1000, 
            "Money Maker", "Earn $1,000 from crops!", 50);
        checkMilestone(player, "ten_thousand", totalEarnings >= 10000, 
            "Rich Farmer", "Earn $10,000 from crops!", 250);
        checkMilestone(player, "hundred_thousand", totalEarnings >= 100000, 
            "Crop Tycoon", "Earn $100,000 from crops!", 1000);
    }
    
    private void checkMilestone(Player player, String id, boolean condition, 
                                String name, String description, double reward) {
        if (!condition) return;
        
        UUID uuid = player.getUniqueId();
        Set<String> achievements = playerAchievements.computeIfAbsent(uuid, k -> new HashSet<>());
        
        if (achievements.contains(id)) return; // Already unlocked
        
        // Unlock achievement
        achievements.add(id);
        
        // Give reward
        if (plugin.getEconomy() != null) {
            plugin.getEconomy().depositPlayer(player, reward);
        }
        
        // Broadcast
        String message = ChatColor.GOLD + "★ " + ChatColor.YELLOW + player.getName() + 
                        ChatColor.GOLD + " unlocked: " + ChatColor.GREEN + name + 
                        ChatColor.GRAY + " (+" + ChatColor.GOLD + "$" + reward + ChatColor.GRAY + ")";
        Bukkit.broadcastMessage(message);
        
        player.sendMessage(ChatColor.GREEN + "Achievement Unlocked!");
        player.sendMessage(ChatColor.YELLOW + name);
        player.sendMessage(ChatColor.GRAY + description);
    }
    
    public Set<String> getPlayerAchievements(UUID uuid) {
        return playerAchievements.getOrDefault(uuid, new HashSet<>());
    }
    
    public int getAchievementCount(UUID uuid) {
        return getPlayerAchievements(uuid).size();
    }
}
