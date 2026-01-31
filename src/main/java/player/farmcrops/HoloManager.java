package player.farmcrops;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class HoloManager {

    private final FarmCrops plugin;

    public HoloManager(FarmCrops plugin) {
        this.plugin = plugin;
    }

    /**
     * Display a temporary hologram above a harvested crop showing tier and weight
     */
    public void showHarvestHologram(Player player, Location location, String tier, double weight, Material cropType) {
        try {
            // Create hologram name (unique per location and time)
            String holoName = "farmcrops_harvest_" + player.getName() + "_" + System.currentTimeMillis();
            
            // Position above the block
            Location holoLoc = location.clone().add(0.5, 1.5, 0.5);
            
            // Get tier color
            String color = plugin.getConfig().getString("tiers." + tier + ".color", "&7");
            
            // Create hologram lines
            List<String> lines = new ArrayList<>();
            lines.add(colorize(color + "&l" + capitalize(tier) + " " + formatName(cropType)));
            lines.add(colorize("&7Weight: &f" + String.format("%.2f", weight) + " kg"));
            
            // Create the hologram using DHAPI
            Hologram hologram = DHAPI.createHologram(holoName, holoLoc, lines);
            
            // Remove after 3 seconds
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (hologram != null) {
                    DHAPI.removeHologram(holoName);
                }
            }, 60L); // 60 ticks = 3 seconds
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create harvest hologram: " + e.getMessage());
        }
    }

    /**
     * Display a hologram above a growing crop (optional future feature)
     */
    public void showGrowingCropHologram(Block block, Material cropType, int age, int maxAge) {
        try {
            String holoName = "farmcrops_growing_" + block.getWorld().getName() + "_" + 
                             block.getX() + "_" + block.getY() + "_" + block.getZ();
            
            Location holoLoc = block.getLocation().add(0.5, 1.0, 0.5);
            
            double growthPercent = (age / (double) maxAge) * 100;
            
            List<String> lines = new ArrayList<>();
            lines.add(colorize("&e" + formatName(cropType)));
            lines.add(colorize("&7Growth: &a" + String.format("%.0f", growthPercent) + "%"));
            
            // Check if hologram already exists
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

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private String formatName(Material m) {
        return m.name().charAt(0) + m.name().substring(1).toLowerCase().replace("_", " ");
    }

    private String colorize(String s) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', s);
    }
}
