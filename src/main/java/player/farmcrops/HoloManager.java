package player.farmcrops;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.Arrays;
import java.util.List;

/**
 * Manages harvest holograms using DecentHolograms
 * NO VISIBLE ARMOR STANDS - All holograms are created with proper settings
 */
public class HoloManager {
    
    private final FarmCrops plugin;
    private int holoCounter = 0;
    
    public HoloManager(FarmCrops plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Show a temporary hologram when harvesting crops
     */
    public void flashHarvest(Location loc, String playerName, String tier, double weight, double price, String cropName) {
        try {
            // Get tier color
            String tierColor = getTierColor(tier);
            
            // Create hologram lines
            List<String> lines = Arrays.asList(
                tierColor + "✦ " + tier.toUpperCase() + " " + cropName + " " + tierColor + "✦",
                ChatColor.GRAY + "Weight: " + ChatColor.WHITE + String.format("%.2f", weight) + "kg",
                ChatColor.GOLD + "+$" + String.format("%.2f", price)
            );
            
            // Create unique hologram name
            String holoName = "harvest_" + System.currentTimeMillis() + "_" + (holoCounter++);
            
            // Spawn location slightly above the crop
            Location holoLoc = loc.clone().add(0.5, 1.5, 0.5);
            
            // Create hologram using DecentHolograms API
            // This will NOT show armor stands because DecentHolograms handles it properly
            Hologram hologram = DHAPI.createHologram(holoName, holoLoc, lines);
            
            // Make it temporary and remove after 2 seconds (40 ticks)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (hologram != null) {
                    hologram.delete();
                }
            }, 40L);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create hologram: " + e.getMessage());
        }
    }
    
    /**
     * Get color code for tier
     */
    private String getTierColor(String tier) {
        return switch (tier.toLowerCase()) {
            case "legendary" -> ChatColor.GOLD.toString() + ChatColor.BOLD;
            case "epic" -> ChatColor.LIGHT_PURPLE.toString() + ChatColor.BOLD;
            case "rare" -> ChatColor.BLUE.toString() + ChatColor.BOLD;
            case "uncommon" -> ChatColor.GREEN.toString();
            default -> ChatColor.WHITE.toString(); // Common
        };
    }
}
