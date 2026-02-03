package player.farmcrops;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.List;

/**
 * Manages temporary harvest flash holograms using FancyHolograms
 */
public class HarvestHologramManager {
    
    private final FarmCrops plugin;
    private final FancyHologramsPlugin fancyAPI;
    
    public HarvestHologramManager(FarmCrops plugin) {
        this.plugin = plugin;
        this.fancyAPI = (FancyHologramsPlugin) Bukkit.getPluginManager().getPlugin("FancyHolograms");
    }
    
    /**
     * Shows a temporary hologram when a crop is harvested
     */
    public void flashHarvest(Location loc, String playerName, String tier, double weight, double price, String cropName) {
        if (fancyAPI == null) return;
        
        // Get tier color
        String tierColor = getTierColor(tier);
        
        // Create hologram lines
        String line1 = tierColor + "✦ " + tier + " " + cropName + " " + tierColor + "✦";
        String line2 = ChatColor.GRAY + "Weight: " + ChatColor.WHITE + String.format("%.2f", weight) + "kg";
        String line3 = ChatColor.GOLD + "+$" + String.format("%.2f", price);
        
        // Create hologram data
        TextHologramData data = new TextHologramData(
            "harvest_" + System.currentTimeMillis() + "_" + playerName,
            loc.clone().add(0.5, 1.5, 0.5) // Center and raise above crop
        );
        
        data.setText(List.of(line1, line2, line3));
        data.setPersistent(false);
        data.setVisibilityDistance(16.0f);
        
        // Create and show hologram
        Hologram hologram = fancyAPI.getHologramManager().create(data);
        if (hologram != null) {
            hologram.createHologram();
            hologram.showHologram(Bukkit.getPlayer(playerName));
            
            // Remove after 2 seconds
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                hologram.deleteHologram();
                fancyAPI.getHologramManager().removeHologram(hologram);
            }, 40L); // 2 seconds
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
