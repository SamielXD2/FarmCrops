package player.farmcrops;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Manages temporary harvest flash holograms using FancyHolograms
 * Falls back to SimpleHologram if FancyHolograms is unavailable
 */
public class HarvestHologramManager {
    
    private final FarmCrops plugin;
    private final FancyHologramsPlugin fancyAPI;
    private final boolean useFancyHolograms;
    
    public HarvestHologramManager(FarmCrops plugin) {
        this.plugin = plugin;
        this.fancyAPI = (FancyHologramsPlugin) Bukkit.getPluginManager().getPlugin("FancyHolograms");
        this.useFancyHolograms = (fancyAPI != null);
        
        if (useFancyHolograms) {
            plugin.getLogger().info("✓ Using FancyHolograms for harvest holograms");
        } else {
            plugin.getLogger().info("✓ Using SimpleHologram (armor stands) for harvest holograms");
        }
    }
    
    /**
     * Shows a temporary hologram when a crop is harvested
     */
    public void flashHarvest(Location loc, Player player, String tier, double weight, double price, String cropName) {
        if (useFancyHolograms) {
            flashHarvestFancy(loc, player, tier, weight, price, cropName);
        } else {
            flashHarvestSimple(loc, player, tier, weight, price, cropName);
        }
    }
    
    /**
     * FancyHolograms implementation
     */
    private void flashHarvestFancy(Location loc, Player player, String tier, double weight, double price, String cropName) {
        if (fancyAPI == null) {
            plugin.getLogger().warning("FancyHologramsPlugin is null! Falling back to simple hologram.");
            flashHarvestSimple(loc, player, tier, weight, price, cropName);
            return;
        }
        
        try {
            // Get tier color
            String tierColor = getTierColor(tier);
            
            // Create hologram lines
            String line1 = tierColor + "✦ " + tier.toUpperCase() + " " + cropName + " " + tierColor + "✦";
            String line2 = ChatColor.GRAY + "Weight: " + ChatColor.WHITE + String.format("%.2f", weight) + "kg";
            String line3 = ChatColor.GOLD + "+$" + String.format("%.2f", price);
            
            // Create hologram data
            Location holoLoc = loc.clone().add(0.5, 1.5, 0.5); // Center and raise above crop
            TextHologramData data = new TextHologramData(
                "harvest_" + System.currentTimeMillis() + "_" + player.getUniqueId(),
                holoLoc
            );
            
            data.setText(List.of(line1, line2, line3));
            data.setPersistent(false);
            data.setVisibilityDistance(16);
            
            // Create and show hologram
            Hologram hologram = fancyAPI.getHologramManager().create(data);
            if (hologram != null) {
                hologram.createHologram();
                hologram.showHologram(player);
                
                plugin.getLogger().info("Created harvest hologram for " + player.getName() + " at " + holoLoc);
                
                // Remove after 2 seconds
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    try {
                        hologram.deleteHologram();
                        fancyAPI.getHologramManager().removeHologram(hologram);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to remove hologram: " + e.getMessage());
                    }
                }, 40L); // 2 seconds
            } else {
                plugin.getLogger().warning("Failed to create FancyHologram - falling back to simple hologram!");
                flashHarvestSimple(loc, player, tier, weight, price, cropName);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error creating FancyHologram: " + e.getMessage());
            e.printStackTrace();
            flashHarvestSimple(loc, player, tier, weight, price, cropName);
        }
    }
    
    /**
     * SimpleHologram (armor stand) implementation as fallback
     */
    private void flashHarvestSimple(Location loc, Player player, String tier, double weight, double price, String cropName) {
        try {
            // Get tier color
            String tierColor = getTierColor(tier);
            
            // Create hologram at raised location
            Location holoLoc = loc.clone().add(0.5, 1.5, 0.5);
            SimpleHologram hologram = new SimpleHologram(plugin, holoLoc);
            
            // Add lines
            hologram.addLine(tierColor + "✦ " + tier.toUpperCase() + " " + cropName + " " + tierColor + "✦");
            hologram.addLine(ChatColor.GRAY + "Weight: " + ChatColor.WHITE + String.format("%.2f", weight) + "kg");
            hologram.addLine(ChatColor.GOLD + "+$" + String.format("%.2f", price));
            
            // Show and remove after 2 seconds
            hologram.show();
            hologram.removeAfter(40L);
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error creating simple hologram: " + e.getMessage());
            e.printStackTrace();
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
