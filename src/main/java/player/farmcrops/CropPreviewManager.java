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
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CropPreviewManager implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Hologram> activeHolograms = new HashMap<>();
    private final FancyHologramsPlugin fancyHolograms;

    public CropPreviewManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.fancyHolograms = (FancyHologramsPlugin) plugin.getServer().getPluginManager().getPlugin("FancyHolograms");
    }

    // ────────────────────────────────────────
    // Event: right-click on a crop block
    // ────────────────────────────────────────

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only care about right-click on a block
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Block block = event.getClickedBlock();

        // Master toggle - check permission
        if (!plugin.getConfig().getBoolean("holograms.right-click-preview", true)) return;

        // Must be a tracked crop
        if (!isTrackedCrop(block.getType())) return;

        Player player = event.getPlayer();

        // Check if player has permission to see previews
        if (!player.hasPermission("farmcrops.preview")) {
            return; // Silently ignore if no permission
        }

        // Get crop stage info
        if (!(block.getBlockData() instanceof Ageable)) return;
        Ageable ageable = (Ageable) block.getBlockData();

        int currentAge = ageable.getAge();
        int maxAge = ageable.getMaximumAge();

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
        hologramData.setText(text);
        hologramData.setTextShadow(true);
        hologramData.setBackground(TextHologramData.Background.FULL);
        
        // Create and show hologram
        Hologram hologram = fancyHolograms.getHologramManager().create(hologramData);
        hologram.createHologram();
        hologram.showHologram(player);

        // Store reference
        activeHolograms.put(player.getUniqueId(), hologram);

        // Auto-remove after 5 seconds
        int duration = plugin.getConfig().getInt("holograms.preview-duration", 5);
        new BukkitRunnable() {
            @Override
            public void run() {
                removeHologram(player);
            }
        }.runTaskLater(plugin, duration * 20L);
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
    }
}
