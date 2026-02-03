package player.farmcrops;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Achievement/Milestone System
 * Players must manually claim achievements to receive rewards
 */
public class AchievementManager {
    
    private final FarmCrops plugin;
    private final Map<UUID, Set<String>> playerAchievements;  // Claimed achievements
    private final Map<UUID, Set<String>> unclaimedAchievements; // Unlocked but not claimed
    private final Map<String, AchievementData> achievementData;
    
    public AchievementManager(FarmCrops plugin) {
        this.plugin = plugin;
        this.playerAchievements = new HashMap<>();
        this.unclaimedAchievements = new HashMap<>();
        this.achievementData = new HashMap<>();
        loadAchievements();
        initializeAchievements();
    }
    
    private void loadAchievements() {
        // Load player achievements from data file
        // TODO: Implement persistence
    }
    
    private void initializeAchievements() {
        // Harvest milestones
        registerAchievement("first_harvest", "First Harvest", "Harvest your first crop!", 10, 1, AchievementType.HARVEST);
        registerAchievement("hundred_harvest", "Century Farmer", "Harvest 100 crops!", 100, 100, AchievementType.HARVEST);
        registerAchievement("thousand_harvest", "Master Farmer", "Harvest 1,000 crops!", 500, 1000, AchievementType.HARVEST);
        registerAchievement("ten_thousand_harvest", "Legendary Farmer", "Harvest 10,000 crops!", 2000, 10000, AchievementType.HARVEST);
        registerAchievement("fifty_thousand_harvest", "Elite Farmer", "Harvest 50,000 crops!", 5000, 50000, AchievementType.HARVEST);
        
        // Earnings milestones
        registerAchievement("first_thousand", "Money Maker", "Earn $1,000 from crops!", 50, 1000, AchievementType.EARNINGS);
        registerAchievement("ten_thousand", "Rich Farmer", "Earn $10,000 from crops!", 250, 10000, AchievementType.EARNINGS);
        registerAchievement("hundred_thousand", "Crop Tycoon", "Earn $100,000 from crops!", 1000, 100000, AchievementType.EARNINGS);
        registerAchievement("million_earned", "Millionaire", "Earn $1,000,000 from crops!", 10000, 1000000, AchievementType.EARNINGS);
        
        // Tier milestones
        registerAchievement("first_epic", "Epic Harvester", "Harvest an Epic tier crop!", 200, 1, AchievementType.TIER_EPIC);
        registerAchievement("first_legendary", "Legendary Harvester", "Harvest a Legendary tier crop!", 500, 1, AchievementType.TIER_LEGENDARY);
        
        // Collection milestones
        registerAchievement("collection_wheat_1000", "Wheat Master", "Harvest 1,000 wheat!", 300, 1000, AchievementType.COLLECTION_WHEAT);
        registerAchievement("collection_carrot_1000", "Carrot King", "Harvest 1,000 carrots!", 300, 1000, AchievementType.COLLECTION_CARROT);
        registerAchievement("collection_potato_1000", "Potato Lord", "Harvest 1,000 potatoes!", 300, 1000, AchievementType.COLLECTION_POTATO);
    }
    
    private void registerAchievement(String id, String name, String description, double reward, int requirement, AchievementType type) {
        achievementData.put(id, new AchievementData(id, name, description, reward, requirement, type));
    }
    
    public void checkAchievements(Player player) {
        UUID uuid = player.getUniqueId();
        StatsManager statsManager = plugin.getStatsManager();
        
        int totalHarvests = statsManager.getTotalHarvests(uuid);
        double totalEarnings = statsManager.getTotalEarnings(uuid);
        
        // Check all achievements
        for (AchievementData ach : achievementData.values()) {
            if (hasAchievement(uuid, ach.id)) continue; // Already claimed
            if (isUnclaimedAchievement(uuid, ach.id)) continue; // Already unlocked, just not claimed
            
            boolean unlocked = false;
            
            switch (ach.type) {
                case HARVEST:
                    unlocked = totalHarvests >= ach.requirement;
                    break;
                case EARNINGS:
                    unlocked = totalEarnings >= ach.requirement;
                    break;
                case TIER_EPIC:
                    unlocked = statsManager.getEpicHarvests(uuid) >= ach.requirement;
                    break;
                case TIER_LEGENDARY:
                    unlocked = statsManager.getLegendaryHarvests(uuid) >= ach.requirement;
                    break;
                case COLLECTION_WHEAT:
                    unlocked = statsManager.getCropHarvests(uuid, "WHEAT") >= ach.requirement;
                    break;
                case COLLECTION_CARROT:
                    unlocked = statsManager.getCropHarvests(uuid, "CARROTS") >= ach.requirement;
                    break;
                case COLLECTION_POTATO:
                    unlocked = statsManager.getCropHarvests(uuid, "POTATOES") >= ach.requirement;
                    break;
            }
            
            if (unlocked) {
                unlockAchievement(player, ach.id);
            }
        }
    }
    
    private void unlockAchievement(Player player, String id) {
        UUID uuid = player.getUniqueId();
        Set<String> unclaimed = unclaimedAchievements.computeIfAbsent(uuid, k -> new HashSet<>());
        unclaimed.add(id);
        
        AchievementData ach = achievementData.get(id);
        
        // Notify player
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage(ChatColor.GOLD + "★ " + ChatColor.YELLOW + "NEW ACHIEVEMENT UNLOCKED!");
        player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + ach.name);
        player.sendMessage(ChatColor.GRAY + ach.description);
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "Reward: " + ChatColor.GOLD + "$" + ach.reward);
        player.sendMessage(ChatColor.AQUA + "Use /achievements to claim your reward!");
        player.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }
    
    public boolean claimAchievement(Player player, String id) {
        UUID uuid = player.getUniqueId();
        
        if (!isUnclaimedAchievement(uuid, id)) {
            return false;
        }
        
        AchievementData ach = achievementData.get(id);
        
        // Move from unclaimed to claimed
        unclaimedAchievements.get(uuid).remove(id);
        Set<String> claimed = playerAchievements.computeIfAbsent(uuid, k -> new HashSet<>());
        claimed.add(id);
        
        // Give reward
        if (plugin.getEconomy() != null) {
            plugin.getEconomy().depositPlayer(player, ach.reward);
        }
        
        // Broadcast
        if (plugin.getConfig().getBoolean("achievements.broadcast", true)) {
            String message = ChatColor.GOLD + "★ " + ChatColor.YELLOW + player.getName() + 
                            ChatColor.GOLD + " completed: " + ChatColor.GREEN + ach.name;
            Bukkit.broadcastMessage(message);
        }
        
        player.sendMessage(ChatColor.GREEN + "✓ Achievement claimed! +" + ChatColor.GOLD + "$" + ach.reward);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        
        return true;
    }
    
    public Set<String> getPlayerAchievements(UUID uuid) {
        return playerAchievements.getOrDefault(uuid, new HashSet<>());
    }
    
    public Set<String> getUnclaimedAchievements(UUID uuid) {
        return unclaimedAchievements.getOrDefault(uuid, new HashSet<>());
    }
    
    public boolean hasAchievement(UUID uuid, String id) {
        return playerAchievements.containsKey(uuid) && playerAchievements.get(uuid).contains(id);
    }
    
    public boolean isUnclaimedAchievement(UUID uuid, String id) {
        return unclaimedAchievements.containsKey(uuid) && unclaimedAchievements.get(uuid).contains(id);
    }
    
    public int getAchievementCount(UUID uuid) {
        return getPlayerAchievements(uuid).size();
    }
    
    public int getTotalAchievements() {
        return achievementData.size();
    }
    
    public AchievementData getAchievementData(String id) {
        return achievementData.get(id);
    }
    
    public Collection<AchievementData> getAllAchievements() {
        return achievementData.values();
    }
    
    // Achievement data class
    public static class AchievementData {
        public final String id;
        public final String name;
        public final String description;
        public final double reward;
        public final int requirement;
        public final AchievementType type;
        
        public AchievementData(String id, String name, String description, double reward, int requirement, AchievementType type) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.reward = reward;
            this.requirement = requirement;
            this.type = type;
        }
    }
    
    public enum AchievementType {
        HARVEST, EARNINGS, TIER_EPIC, TIER_LEGENDARY,
        COLLECTION_WHEAT, COLLECTION_CARROT, COLLECTION_POTATO
    }
}
