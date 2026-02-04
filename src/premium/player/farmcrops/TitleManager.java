package player.farmcrops;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Title System - Players can unlock and equip titles from achievements
 */
public class TitleManager {
    
    private final FarmCrops plugin;
    private final Map<UUID, String> equippedTitles;
    private final Map<String, String> achievementTitles; // achievement id -> title
    
    public TitleManager(FarmCrops plugin) {
        this.plugin = plugin;
        this.equippedTitles = new HashMap<>();
        this.achievementTitles = new HashMap<>();
        initializeTitles();
    }
    
    private void initializeTitles() {
        // Map achievement IDs to titles
        achievementTitles.put("first_harvest", "&7Novice");
        achievementTitles.put("hundred_harvest", "&aCentury Farmer");
        achievementTitles.put("thousand_harvest", "&9Master Farmer");
        achievementTitles.put("ten_thousand_harvest", "&6Legendary Farmer");
        achievementTitles.put("first_thousand", "&aMoney Maker");
        achievementTitles.put("ten_thousand", "&9Rich Farmer");
        achievementTitles.put("hundred_thousand", "&6Crop Tycoon");
        achievementTitles.put("first_epic", "&dEpic Harvester");
        achievementTitles.put("first_legendary", "&6Legendary Harvester");
        achievementTitles.put("collection_wheat_1000", "&eWheat Master");
        achievementTitles.put("collection_carrot_1000", "&6Carrot King");
        achievementTitles.put("collection_potato_1000", "&ePotato Lord");
        achievementTitles.put("fifty_thousand_harvest", "&5Elite Farmer");
        achievementTitles.put("million_earned", "&4Millionaire");
    }
    
    public void equipTitle(Player player, String achievementId) {
        // Check if player has unlocked this achievement
        if (!plugin.getAchievementManager().hasAchievement(player.getUniqueId(), achievementId)) {
            player.sendMessage(ChatColor.RED + "You haven't unlocked this achievement yet!");
            return;
        }
        
        String title = achievementTitles.get(achievementId);
        if (title == null) {
            player.sendMessage(ChatColor.RED + "This achievement doesn't have a title!");
            return;
        }
        
        equippedTitles.put(player.getUniqueId(), title);
        player.sendMessage(ChatColor.GREEN + "✓ Title equipped: " + colorize(title));
    }
    
    public void unequipTitle(Player player) {
        equippedTitles.remove(player.getUniqueId());
        player.sendMessage(ChatColor.YELLOW + "Title unequipped.");
    }
    
    public String getEquippedTitle(UUID uuid) {
        return equippedTitles.get(uuid);
    }
    
    public boolean hasTitle(UUID uuid) {
        return equippedTitles.containsKey(uuid);
    }
    
    public String getTitleForAchievement(String achievementId) {
        return achievementTitles.get(achievementId);
    }
    
    public List<String> getUnlockedTitles(UUID uuid) {
        List<String> titles = new ArrayList<>();
        Set<String> achievements = plugin.getAchievementManager().getPlayerAchievements(uuid);
        
        for (String achId : achievements) {
            String title = achievementTitles.get(achId);
            if (title != null) {
                titles.add(title);
            }
        }
        
        return titles;
    }
    
    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
