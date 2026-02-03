package player.farmcrops;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Crop Collections System
 * Track how many of each crop type players have harvested
 */
public class CollectionManager {
    
    private final FarmCrops plugin;
    private final Map<UUID, Map<Material, Integer>> playerCollections; // UUID -> Crop -> Amount
    
    // Collection milestones for each crop
    private static final int[] MILESTONES = {50, 100, 250, 500, 1000, 2500, 5000, 10000};
    
    public CollectionManager(FarmCrops plugin) {
        this.plugin = plugin;
        this.playerCollections = new HashMap<>();
    }
    
    public void addCropToCollection(Player player, Material cropType) {
        UUID uuid = player.getUniqueId();
        Map<Material, Integer> collections = playerCollections.computeIfAbsent(uuid, k -> new HashMap<>());
        
        int oldAmount = collections.getOrDefault(cropType, 0);
        int newAmount = oldAmount + 1;
        collections.put(cropType, newAmount);
        
        // Check if hit a milestone
        for (int milestone : MILESTONES) {
            if (newAmount == milestone) {
                reachedMilestone(player, cropType, milestone);
                break;
            }
        }
    }
    
    private void reachedMilestone(Player player, Material cropType, int amount) {
        String cropName = formatCropName(cropType);
        
        // Calculate reward (increases with milestone)
        double reward = amount * 0.5; // $0.50 per crop in milestone
        
        if (plugin.getEconomy() != null) {
            plugin.getEconomy().depositPlayer(player, reward);
        }
        
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "★ COLLECTION MILESTONE! ★");
        player.sendMessage(ChatColor.YELLOW + cropName + " Collection: " + 
                         ChatColor.GREEN + amount + " harvested!");
        player.sendMessage(ChatColor.GRAY + "Reward: " + ChatColor.GOLD + "$" + reward);
        player.sendMessage("");
    }
    
    public int getCollectionAmount(UUID uuid, Material cropType) {
        return playerCollections.getOrDefault(uuid, new HashMap<>()).getOrDefault(cropType, 0);
    }
    
    public Map<Material, Integer> getPlayerCollections(UUID uuid) {
        return new HashMap<>(playerCollections.getOrDefault(uuid, new HashMap<>()));
    }
    
    public int getTotalCollected(UUID uuid) {
        return playerCollections.getOrDefault(uuid, new HashMap<>())
                .values().stream().mapToInt(Integer::intValue).sum();
    }
    
    public int getNextMilestone(int current) {
        for (int milestone : MILESTONES) {
            if (milestone > current) {
                return milestone;
            }
        }
        return -1; // Max reached
    }
    
    private String formatCropName(Material crop) {
        String name = crop.name().replace("_", " ").toLowerCase();
        String[] words = name.split(" ");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        
        return result.toString().trim();
    }
}
