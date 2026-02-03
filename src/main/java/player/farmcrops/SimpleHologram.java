package player.farmcrops;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple custom hologram using armor stands as a fallback when FancyHolograms fails
 */
public class SimpleHologram {
    
    private final JavaPlugin plugin;
    private final List<ArmorStand> armorStands = new ArrayList<>();
    private final Location location;
    
    public SimpleHologram(JavaPlugin plugin, Location location) {
        this.plugin = plugin;
        this.location = location;
    }
    
    /**
     * Add a line of text to the hologram
     */
    public void addLine(String text) {
        Location lineLoc = location.clone().add(0, -0.25 * armorStands.size(), 0);
        
        ArmorStand stand = location.getWorld().spawn(lineLoc, ArmorStand.class, as -> {
            as.setVisible(false);
            as.setGravity(false);
            as.setCustomNameVisible(true);
            as.setCustomName(text);
            as.setMarker(true);
            as.setInvulnerable(true);
            as.setSmall(true);
        });
        
        armorStands.add(stand);
    }
    
    /**
     * Show the hologram (already visible when spawned)
     */
    public void show() {
        // Armor stands are automatically visible
    }
    
    /**
     * Remove the hologram
     */
    public void remove() {
        for (ArmorStand stand : armorStands) {
            if (stand != null && stand.isValid()) {
                stand.remove();
            }
        }
        armorStands.clear();
    }
    
    /**
     * Remove the hologram after a delay
     */
    public void removeAfter(long ticks) {
        Bukkit.getScheduler().runTaskLater(plugin, this::remove, ticks);
    }
}
