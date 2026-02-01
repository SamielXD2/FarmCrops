package player.farmcrops;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

public class HoloManager {

    private final FarmCrops plugin;

    public HoloManager(FarmCrops plugin) {
        this.plugin = plugin;
    }

    /**
     * Flash harvest hologram with tier, weight, and price
     */
    public void flashHarvest(Location location, String playerName, String tier, double weight, double price, String cropName) {
        try {
            // Create hologram name (unique per location and time)
            String holoName = "farmcrops_harvest_" + playerName + "_" + System.currentTimeMillis();
            
            // Position above the block
            Location holoLoc = location.clone().add(0.0, 1.5, 0.0);
            
            // Get tier color
            String color = plugin.getConfig().getString("tiers." + tier + ".color", "&7");
            
            // Create hologram lines
            List<String> lines = new ArrayList<>();
            lines.add(colorize(color + "&l" + capitalize(tier) + " " + cropName));
            lines.add(colorize("&7Weight: &f" + String.format("%.2f", weight) + " kg"));
            lines.add(colorize("&7Value: &a$" + String.format("%.2f", price)));
            
            // Create the hologram using DHAPI
            Hologram hologram = DHAPI.createHologram(holoName, holoLoc, lines);
            
            // Remove after 3 seconds
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (DHAPI.getHologram(holoName) != null) {
                    DHAPI.removeHologram(holoName);
                }
            }, 60L); // 60 ticks = 3 seconds
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create harvest hologram: " + e.getMessage());
        }
    }

    /**
     * Show hologram above growing crop when player looks at it
     * Displays growth progress and estimated price range
     */
    public void showGrowingCropHologram(Block block, Material cropType, int age, int maxAge) {
        try {
            String holoName = "farmcrops_growing_" + block.getWorld().getName() + "_" + 
                             block.getX() + "_" + block.getY() + "_" + block.getZ();
            
            Location holoLoc = block.getLocation().add(0.5, 1.0, 0.5);
            
            double growthPercent = (age / (double) maxAge) * 100;
            
            // Calculate min and max possible prices
            double minWeight = plugin.getConfig().getDouble("weight.min", 0.5);
            double maxWeight = plugin.getConfig().getDouble("weight.max", 10.0);
            double basePrice = plugin.getConfig().getDouble("prices.default", 10.0);
            
            // Use common tier as minimum, legendary as maximum
            double minTierMultiplier = plugin.getConfig().getDouble("tiers.common.multiplier", 1.0);
            double maxTierMultiplier = plugin.getConfig().getDouble("tiers.legendary.multiplier", 12.0);
            
            double minPrice = basePrice * minTierMultiplier * minWeight;
            double maxPrice = basePrice * maxTierMultiplier * maxWeight;
            
            List<String> lines = new ArrayList<>();
            lines.add(colorize("&e" + formatName(cropType)));
            
            // Growth progress bar
            String progressBar = getProgressBar(growthPercent);
            lines.add(colorize("&7Growth: " + progressBar + " &f" + String.format("%.0f", growthPercent) + "%"));
            
            // Estimated price range (only show when crop is mature)
            if (age >= maxAge) {
                lines.add(colorize("&7Ready to harvest!"));
                lines.add(colorize("&7Value: &a$" + String.format("%.2f", minPrice) + " - $" + String.format("%.2f", maxPrice)));
            } else {
                lines.add(colorize("&7Est. Value: &a$" + String.format("%.2f", minPrice) + " - $" + String.format("%.2f", maxPrice)));
            }
            
            // Check if hologram already exists and remove it
            if (DHAPI.getHologram(holoName) != null) {
                DHAPI.removeHologram(holoName);
            }
            
            DHAPI.createHologram(holoName, holoLoc, lines);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create growing crop hologram: " + e.getMessage());
        }
    }

    /**
     * Remove a growing crop hologram
     */
    public void removeGrowingCropHologram(Block block) {
        try {
            String holoName = "farmcrops_growing_" + block.getWorld().getName() + "_" + 
                             block.getX() + "_" + block.getY() + "_" + block.getZ();
            
            if (DHAPI.getHologram(holoName) != null) {
                DHAPI.removeHologram(holoName);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to remove growing crop hologram: " + e.getMessage());
        }
    }
    
    /**
     * Generate a colored progress bar
     */
    private String getProgressBar(double percent) {
        int totalBars = 10;
        int filledBars = (int) Math.round((percent / 100.0) * totalBars);
        
        StringBuilder bar = new StringBuilder();
        
        // Color based on progress
        String fillColor;
        if (percent < 33) {
            fillColor = "&c"; // Red
        } else if (percent < 66) {
            fillColor = "&e"; // Yellow
        } else if (percent < 100) {
            fillColor = "&a"; // Green
        } else {
            fillColor = "&2"; // Dark Green
        }
        
        bar.append(fillColor);
        for (int i = 0; i < filledBars; i++) {
            bar.append("█");
        }
        
        bar.append("&7");
        for (int i = filledBars; i < totalBars; i++) {
            bar.append("█");
        }
        
        return bar.toString();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private String formatName(Material m) {
        String name = m.name();
        if (name.endsWith("S")) {
            // Remove trailing S for plural crop names
            name = name.substring(0, name.length() - 1);
        }
        return name.charAt(0) + name.substring(1).toLowerCase().replace("_", " ");
    }

    private String colorize(String s) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', s);
    }
}
