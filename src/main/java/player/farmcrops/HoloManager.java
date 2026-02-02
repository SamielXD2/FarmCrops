package player.farmcrops;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * v0.9.0 - Hologram Manager with Fixed Features
 * 
 * Features:
 * - Invisible armor stands (no flash on spawn)
 * - Working growing cursor
 * - Per-player visibility checks
 * - Harvest flash holograms
 */
public class HoloManager {
    
    private final FarmCrops plugin;
    private final Map<UUID, Hologram> activeCursorHolograms = new HashMap<>();
    private final Map<Location, Long> recentHarvests = new HashMap<>();
    
    private static final Material[] CROP_TYPES = {
        Material.WHEAT, Material.CARROTS, Material.POTATOES,
        Material.BEETROOTS, Material.MELON
    };
    
    public HoloManager(FarmCrops plugin) {
        this.plugin = plugin;
        startGrowingCursorTask();
        startCleanupTask();
    }
    
    /**
     * v0.9.0 FIX: Growing cursor now works!
     * 
     * Checks what block the player is looking at every 10 ticks (0.5s)
     * If it's a crop, shows a hologram above it with growth status
     */
    private void startGrowingCursorTask() {
        if (!plugin.getConfig().getBoolean("holograms.growing-cursor", true)) {
            return;
        }
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.getConfig().getBoolean("holograms.growing-cursor", true)) {
                    return;
                }
                
                for (Player player : Bukkit.getOnlinePlayers()) {
                    // Check player preferences
                    PlayerSettings.PlayerPreferences prefs = plugin.getPlayerSettings()
                        .getPreferences(player.getUniqueId());
                    
                    if (!prefs.showHolograms) {
                        // Remove existing hologram if player disabled them
                        removeGrowingCursor(player);
                        continue;
                    }
                    
                    Block target = player.getTargetBlockExact(5);
                    
                    if (target != null && isCrop(target.getType())) {
                        updateGrowingCursor(player, target);
                    } else {
                        removeGrowingCursor(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L); // Every 0.5 seconds
    }
    
    private void updateGrowingCursor(Player player, Block cropBlock) {
        if (!(cropBlock.getBlockData() instanceof Ageable)) {
            return;
        }
        
        Ageable ageable = (Ageable) cropBlock.getBlockData();
        int age = ageable.getAge();
        int maxAge = ageable.getMaximumAge();
        int percentage = (int) ((age / (double) maxAge) * 100);
        
        String cropName = CropListener.formatName(cropBlock.getType());
        ChatColor color;
        String status;
        
        if (age >= maxAge) {
            color = ChatColor.GREEN;
            status = "READY TO HARVEST!";
        } else if (percentage >= 75) {
            color = ChatColor.YELLOW;
            status = percentage + "% grown";
        } else if (percentage >= 50) {
            color = ChatColor.GOLD;
            status = percentage + "% grown";
        } else {
            color = ChatColor.RED;
            status = percentage + "% grown";
        }
        
        Location holoLoc = cropBlock.getLocation().add(0.5, 1.2, 0.5);
        
        // Remove old hologram if exists
        removeGrowingCursor(player);
        
        // Create new hologram (INVISIBLE!)
        String holoName = "growing_cursor_" + player.getUniqueId();
        Hologram hologram = DHAPI.createHologram(holoName, holoLoc);
        
        DHAPI.addHologramLine(hologram, color + "" + ChatColor.BOLD + cropName);
        DHAPI.addHologramLine(hologram, ChatColor.GRAY + status);
        
        // CRITICAL FIX: Make it visible only to this player
        hologram.setDefaultVisibleState(false);
        hologram.setShowPlayer(player);
        hologram.show();
        
        activeCursorHolograms.put(player.getUniqueId(), hologram);
    }
    
    private void removeGrowingCursor(Player player) {
        Hologram existing = activeCursorHolograms.remove(player.getUniqueId());
        if (existing != null) {
            existing.delete();
        }
    }
    
    /**
     * Harvest flash hologram - shows when crop is broken
     */
    public void flashHarvest(Location location, String playerName, String tier, 
                           double weight, double price, String cropName) {
        
        // Check if system is enabled
        if (!plugin.getConfig().getBoolean("holograms.harvest-flash", true)) {
            return;
        }
        
        // Prevent spam at same location
        if (recentHarvests.containsKey(location)) {
            long lastHarvest = recentHarvests.get(location);
            if (System.currentTimeMillis() - lastHarvest < 500) {
                return; // Too soon
            }
        }
        recentHarvests.put(location, System.currentTimeMillis());
        
        String tierColor = plugin.getConfig().getString("tiers." + tier + ".color", "&7");
        ChatColor color = ChatColor.translateAlternateColorCodes('&', tierColor).charAt(0) == '§' ?
            ChatColor.getByChar(tierColor.charAt(1)) : ChatColor.GRAY;
        
        Location holoLoc = location.clone().add(0, 0.5, 0);
        String holoName = "harvest_" + UUID.randomUUID();
        
        Hologram hologram = DHAPI.createHologram(holoName, holoLoc);
        
        // Add lines
        DHAPI.addHologramLine(hologram, color + "" + ChatColor.BOLD + tier.toUpperCase() + " " + cropName);
        DHAPI.addHologramLine(hologram, ChatColor.GRAY + "" + weight + " kg " + ChatColor.GOLD + "$" + String.format("%.2f", price));
        
        // Make visible only to nearby players who have holograms enabled
        hologram.setDefaultVisibleState(false);
        for (Player nearbyPlayer : location.getWorld().getNearbyPlayers(location, 30)) {
            PlayerSettings.PlayerPreferences prefs = plugin.getPlayerSettings()
                .getPreferences(nearbyPlayer.getUniqueId());
            if (prefs.showHolograms) {
                hologram.setShowPlayer(nearbyPlayer);
                hologram.show();
            }
        }
        
        // Animate upward and fade
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                ticks++;
                
                if (ticks >= 40) { // 2 seconds
                    hologram.delete();
                    cancel();
                    return;
                }
                
                // Move up slowly
                holoLoc.add(0, 0.02, 0);
                hologram.setLocation(holoLoc);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    /**
     * Cleanup task - removes old harvest locations from map
     */
    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                recentHarvests.entrySet().removeIf(entry -> 
                    now - entry.getValue() > 5000); // 5 seconds old
            }
        }.runTaskTimer(plugin, 0L, 100L); // Every 5 seconds
    }
    
    /**
     * Cleanup player's cursor hologram on logout
     */
    public void cleanup(Player player) {
        removeGrowingCursor(player);
    }
    
    private boolean isCrop(Material material) {
        for (Material crop : CROP_TYPES) {
            if (crop == material) return true;
        }
        return false;
    }
}
