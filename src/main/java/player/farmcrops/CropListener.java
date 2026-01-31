package player.farmcrops;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class CropListener implements Listener {

    private final FarmCrops plugin;

    public static final NamespacedKey WEIGHT_KEY = new NamespacedKey("farmcrops", "weight");
    public static final NamespacedKey TIER_KEY   = new NamespacedKey("farmcrops", "tier");

    private static final Material[] TRACKED_CROPS = {
            Material.WHEAT, Material.CARROT, Material.POTATO,
            Material.BEETROOT, Material.MELON
    };

    public CropListener(FarmCrops plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (!isTrackedCrop(block.getType())) return;

        if (!(block.getBlockData() instanceof Ageable)) return;
        Ageable ageable = (Ageable) block.getBlockData();
        if (ageable.getAge() < ageable.getMaximumAge()) {
            return;
        }

        Player player = event.getPlayer();
        
        double minWeight = plugin.getConfig().getDouble("weight.min", 0.5);
        double maxWeight = plugin.getConfig().getDouble("weight.max", 10.0);

        String tier  = rollTier();
        String color = plugin.getConfig().getString("tiers." + tier + ".color", "&7");

        double weight = ThreadLocalRandom.current().nextDouble(minWeight, maxWeight);
        weight = Math.round(weight * 100.0) / 100.0;

        ItemStack item = new ItemStack(getDropMaterial(block.getType()), 1);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(WEIGHT_KEY, PersistentDataType.DOUBLE, weight);
            pdc.set(TIER_KEY,   PersistentDataType.STRING, tier);

            // IMPORTANT: Only show tier in lore, NOT weight
            // This allows items of same tier to stack together
            // Weight is still saved in PDC for selling calculations
            List<String> lore = new ArrayList<>();
            lore.add(colorize(color) + "Tier: " + capitalize(tier));
            meta.setLore(lore);

            meta.setDisplayName(colorize(color) + capitalize(tier) + " " + formatName(block.getType()));
            item.setItemMeta(meta);
        }

        event.setDropItems(false);
        Location dropLoc = block.getLocation().add(0.5, 0.5, 0.5);
        event.getPlayer().getWorld().dropItemNaturally(dropLoc, item);

        // Show hologram if enabled
        if (plugin.isHoloEnabled()) {
            plugin.getHoloManager().flashHarvest(
                dropLoc, 
                player.getName(), 
                tier, 
                weight, 
                formatName(block.getType())
            );
        }

        double worth = plugin.getConfig().getDouble("prices.default", 10.0)
            * plugin.getConfig().getDouble("tiers." + tier + ".multiplier", 1.0) * weight;
        plugin.getLogger().info("✓ " + event.getPlayer().getName() + " harvested " + tier.toUpperCase()
            + " " + formatName(block.getType()) + " (" + weight + "kg) - Worth: $" + String.format("%.2f", worth));
    }

    private String rollTier() {
        int roll = ThreadLocalRandom.current().nextInt(1, 101);
        int common = plugin.getConfig().getInt("tiers.common.chance", 70);
        int rare   = plugin.getConfig().getInt("tiers.rare.chance", 20);
        int epic   = plugin.getConfig().getInt("tiers.epic.chance", 7);

        if (roll <= common)                    return "common";
        else if (roll <= common + rare)        return "rare";
        else if (roll <= common + rare + epic) return "epic";
        else                                   return "legendary";
    }

    private boolean isTrackedCrop(Material m) {
        for (Material t : TRACKED_CROPS) { if (t == m) return true; }
        return false;
    }

    private Material getDropMaterial(Material crop) {
        if (crop == Material.BEETROOT) return Material.BEETROOT;
        if (crop == Material.MELON)    return Material.MELON_SLICE;
        return crop;
    }

    private String capitalize(String s) { return s.substring(0,1).toUpperCase() + s.substring(1); }
    private String formatName(Material m) { return m.name().charAt(0) + m.name().substring(1).toLowerCase().replace("_"," "); }
    private String colorize(String s) { return ChatColor.translateAlternateColorCodes('&', s); }
        }
