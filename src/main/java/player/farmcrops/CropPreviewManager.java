package player.farmcrops;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
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
import org.bukkit.util.RayTraceResult;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CropPreviewManager implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Hologram> activeHolograms = new HashMap<>();
    private final Map<UUID, Block> lastLookedBlock = new HashMap<>();

    public CropPreviewManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only check when player actually moves their head
        if (event.getFrom().getYaw() == event.getTo().getYaw() && 
            event.getFrom().getPitch() == event.getTo().getPitch() &&
            event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        
        if (!player.hasPermission("farmcrops.preview")) {
            return;
        }
        
        if (!plugin.getConfig().getBoolean("holograms.right-click-preview", true)) return;

        RayTraceResult result = player.rayTraceBlocks(5.0);
        if (result == null || result.getHitBlock() == null) {
            Block lastBlock = lastLookedBlock.get(player.getUniqueId());
            if (lastBlock != null) {
                removeHologram(player);
                lastLookedBlock.remove(player.getUniqueId());
            }
            return;
        }
        
        Block block = result.getHitBlock();
        Block lastBlock = lastLookedBlock.get(player.getUniqueId());
        if (lastBlock != null && lastBlock.equals(block)) {
            return;
        }

        // Check if this is a melon block (the actual melon, not the stem)
        if (block.getType() == Material.MELON) {
            if (lastBlock != null) {
                removeHologram(player);
                lastLookedBlock.remove(player.getUniqueId());
            }
            lastLookedBlock.put(player.getUniqueId(), block);
            showMelonInfo(player, block.getLocation());
            return;
        }

        if (!isTrackedCrop(block.getType())) {
            if (lastBlock != null) {
                removeHologram(player);
                lastLookedBlock.remove(player.getUniqueId());
            }
            return;
        }

        if (!(block.getBlockData() instanceof Ageable)) return;
        Ageable ageable = (Ageable) block.getBlockData();

        int currentAge = ageable.getAge();
        int maxAge = ageable.getMaximumAge();

        lastLookedBlock.put(player.getUniqueId(), block);
        showCropPreview(player, block.getLocation(), currentAge, maxAge, block.getType());
    }

    private void showMelonInfo(Player player, Location location) {
        removeHologram(player);

        String[] lines = new String[] {
            "§a§lMELON",
            "§a✓ READY TO HARVEST"
        };
        Location hologramLoc = location.clone().add(0.5, 1.5, 0.5);

        String holoName = "crop-preview-" + player.getUniqueId();
        Hologram hologram = DHAPI.createHologram(holoName, hologramLoc, Arrays.asList(lines));

        activeHolograms.put(player.getUniqueId(), hologram);
    }

    private void showCropPreview(Player player, Location location, int currentAge, int maxAge, Material crop) {
        removeHologram(player);

        String[] lines = formatCropInfo(crop, currentAge, maxAge);
        Location hologramLoc = location.clone().add(0.5, 1.5, 0.5);

        String holoName = "crop-preview-" + player.getUniqueId();
        Hologram hologram = DHAPI.createHologram(holoName, hologramLoc, Arrays.asList(lines));

        activeHolograms.put(player.getUniqueId(), hologram);
    }

    private void removeHologram(Player player) {
        Hologram hologram = activeHolograms.remove(player.getUniqueId());
        if (hologram != null) {
            hologram.delete();
        }
    }

    private String[] formatCropInfo(Material crop, int currentAge, int maxAge) {
        String cropName = crop.name().replace("_", " ");
        String status;
        String color;

        if (currentAge == maxAge) {
            status = "READY TO HARVEST";
            color = "§a";
        } else {
            int percentage = (int) ((currentAge / (double) maxAge) * 100);
            status = "Growing: " + percentage + "%";
            color = "§e";
        }

        return new String[] {
            color + "§l" + cropName,
            color + status
        };
    }

    private boolean isTrackedCrop(Material material) {
        return material == Material.WHEAT ||
               material == Material.CARROTS ||
               material == Material.POTATOES ||
               material == Material.BEETROOTS ||
               material == Material.MELON_STEM ||
               material == Material.ATTACHED_MELON_STEM ||
               material == Material.NETHER_WART ||
               material == Material.COCOA ||
               material == Material.SWEET_BERRY_BUSH;
    }

    public void cleanup() {
        for (Hologram hologram : activeHolograms.values()) {
            hologram.delete();
        }
        activeHolograms.clear();
        lastLookedBlock.clear();
    }
}
