package player.farmcrops;

import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles DecentHolograms integration via reflection.
 * This way the plugin compiles fine even without DecentHolograms — it only
 * actually calls DH at runtime when it's confirmed to be loaded.
 */
public class HoloManager {

    private final FarmCrops plugin;
    private final List<String> activeHoloIds = new ArrayList<>();

    // Cached reflection references
    private Class<?> holoProviderClass;
    private Object providerInstance;
    private Method createMethod;
    private Method removeMethod;

    public HoloManager(FarmCrops plugin) {
        this.plugin = plugin;
        initReflection();
    }

    private void initReflection() {
        try {
            // DecentHolograms main API class
            holoProviderClass = Class.forName("com.dg.dg.api.hologram.provider.HologramProvider");
            Method getInstance = holoProviderClass.getMethod("getInstance");
            providerInstance = getInstance.invoke(null);

            // createHologram(String id, Location loc, List<String> lines)
            createMethod = holoProviderClass.getMethod("createHologram", String.class, Location.class, List.class);

            // removeHologram(String id)
            removeMethod = holoProviderClass.getMethod("removeHologram", String.class);

        } catch (Exception e) {
            // If reflection fails, try the alternate package name some versions use
            try {
                holoProviderClass = Class.forName("com.decent.api.hologram.provider.HologramProvider");
                Method getInstance = holoProviderClass.getMethod("getInstance");
                providerInstance = getInstance.invoke(null);
                createMethod = holoProviderClass.getMethod("createHologram", String.class, Location.class, List.class);
                removeMethod = holoProviderClass.getMethod("removeHologram", String.class);
            } catch (Exception e2) {
                plugin.getLogger().warning("DecentHolograms reflection failed: " + e2.getMessage());
                plugin.getLogger().warning("Holograms will not work. Check your DecentHolograms version.");
            }
        }
    }

    /**
     * Flashes a hologram above a harvest location for 3 seconds.
     */
    public void flashHarvest(Location loc, String tier, String color, double weight, String cropName) {
        if (createMethod == null || providerInstance == null) return;

        Location holoLoc = loc.clone().add(0, 1.5, 0);
        String colorCode = CropListener.colorize(color);
        String holoId = "farmcrops_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 9999);

        List<String> lines = new ArrayList<>();
        lines.add(colorCode + CropListener.capitalize(tier) + " " + cropName);
        lines.add("§7" + weight + " kg");
        lines.add("§a$" + String.format("%.2f", calculatePrice(tier, weight)));

        try {
            Object holo = createMethod.invoke(providerInstance, holoId, holoLoc, lines);
            activeHoloIds.add(holoId);

            // Auto-remove after 3 seconds (60 ticks)
            new BukkitRunnable() {
                @Override
                public void run() {
                    removeHolo(holoId);
                }
            }.runTaskLater(plugin, 60);

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create hologram: " + e.getMessage());
        }
    }

    /**
     * Clears all active holograms. Called on plugin disable.
     */
    public void clearAll() {
        for (String id : new ArrayList<>(activeHoloIds)) {
            removeHolo(id);
        }
        activeHoloIds.clear();
    }

    private void removeHolo(String id) {
        if (removeMethod == null || providerInstance == null) return;
        try {
            removeMethod.invoke(providerInstance, id);
            activeHoloIds.remove(id);
        } catch (Exception e) {
            // Already gone, ignore
        }
    }

    private double calculatePrice(String tier, double weight) {
        double basePrice      = plugin.getConfig().getDouble("prices.default", 1.0);
        double tierMultiplier = plugin.getConfig().getDouble("tiers." + tier + ".multiplier", 1.0);
        return basePrice * tierMultiplier * weight;
    }
                  }
  
