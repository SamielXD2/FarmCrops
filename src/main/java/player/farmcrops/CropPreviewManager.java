package player.farmcrops;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CropPreviewManager implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Hologram> activeHolograms = new HashMap<>();
    private final Map<UUID, Block> lastLookedBlock = new HashMap<>();
    private final FancyHologramsPlugin fancyHolograms;

    public CropPreviewManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.fancyHolograms = (FancyHologramsPlugin) plugin.getServer().getPluginManager().getPlugin("FancyHolograms");
    }

    // ────────────────────────────────────────
    // Event: Looking at a crop block
    // ────────────────────────────────────────

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only check when player actually moves their head (not just position)
        if (event.getFrom().getYaw() == event.getTo().getYaw() && 
            event.getFrom().getPitch() == event.getTo().getPitch() &&
            event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        
        // Check if player has permission to see previews
        if (!player.hasPermission("farmcrops.preview")) {
            return;
        }
        
        // Master toggle
        if (!plugin.getConfig().getBoolean("holograms.right-click-preview", true)) return;

        // Ray trace to see what block player is looking at
        RayTraceResult result = player.rayTraceBlocks(5.0); // 5 block range
        if (result == null || result.getHitBlock() == null) {
            // Not looking at any block, remove hologram if exists
            Block lastBlock = lastLookedBlock.get(player.getUniqueId());
            if (lastBlock != null) {
                removeHologram(player);
                lastLookedBlock.remove(player.getUniqueId());
            }
            return;
        }
        
        Block block = result.getHitBlock();
        
        // Check if this is a different block than last time
        Block lastBlock = lastLookedBlock.get(player.getUniqueId());
        if (lastBlock != null && lastBlock.equals(block)) {
            return; // Still looking at same block
        }

        // Must be a tracked crop
        if (!isTrackedCrop(block.getType())) {
            // Looking at non-crop, remove hologram
            if (lastBlock != null) {
                removeHologram(player);
                lastLookedBlock.remove(player.getUniqueId());
            }
            return;
        }

        // Get crop stage info
        if (!(block.getBlockData() instanceof Ageable)) return;
        Ageable ageable = (Ageable) block.getBlockData();

        int currentAge = ageable.getAge();
        int maxAge = ageable.getMaximumAge();

        // Update the last looked block
        lastLookedBlock.put(player.getUniqueId(), block);

        // Show the hologram
        showCropPreview(player, block.getLocation(), currentAge, maxAge, block.getType());
    }

    private void showCropPreview(Player player, Location location, int currentAge, int maxAge, Material crop) {
        // Remove any existing hologram for this player
        removeHologram(player);

        // Create hologram text
        String text = formatCropInfo(crop, currentAge, maxAge);
        
        // Position hologram above the block
        Location hologramLoc = location.clone().add(0.5, 1.5, 0.5);

        // Create hologram data
        TextHologramData hologramData = new TextHologramData("crop-preview-" + player.getUniqueId(), hologramLoc);
        hologramData.setText(java.util.Collections.singletonList(text)); // FancyHolograms expects List<String>
        hologramData.setTextShadow(true);
        // Background setting removed - may not be available in this FancyHolograms version
        // hologramData.setBackground(TextHologramData.Background.FULL);
        
        // Create and show hologram
        Hologram hologram = fancyHolograms.getHologramManager().create(hologramData);
        hologram.createHologram();
        hologram.showHologram(player);

        // Store reference
        activeHolograms.put(player.getUniqueId(), hologram);

        // Note: Hologram will be removed when player looks away (handled in onPlayerMove)
    }

    private void removeHologram(Player player) {
        Hologram hologram = activeHolograms.remove(player.getUniqueId());
        if (hologram != null) {
            hologram.hideHologram(player);
            hologram.deleteHologram();
        }
    }

    private String formatCropInfo(Material crop, int currentAge, int maxAge) {
        String cropName = crop.name().replace("_", " ");
        String status;
        String color;

        if (currentAge == maxAge) {
            status = "READY TO HARVEST";
            color = "§a"; // Green
        } else {
            int percentage = (int) ((currentAge / (double) maxAge) * 100);
            status = "Growing: " + percentage + "%";
            color = "§e"; // Yellow
        }

        return color + "§l" + cropName + "\n" + color + status;
    }

    private boolean isTrackedCrop(Material material) {
        return material == Material.WHEAT ||
               material == Material.CARROTS ||
               material == Material.POTATOES ||
               material == Material.BEETROOTS ||
               material == Material.NETHER_WART ||
               material == Material.COCOA ||
               material == Material.SWEET_BERRY_BUSH;
    }

    // Clean up on disable
    public void cleanup() {
        for (Hologram hologram : activeHolograms.values()) {
            hologram.deleteHologram();
        }
        activeHolograms.clear();
        lastLookedBlock.clear();
    }
}
