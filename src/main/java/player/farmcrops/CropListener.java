package player.farmcrops;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
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
    public static final NamespacedKey CROP_KEY   = new NamespacedKey("farmcrops", "crop");

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
        if (ageable.getAge() < ageable.getMaximumAge()) return;

        // --- Read config ---
        double minWeight = plugin.getConfig().getDouble("weight.min", 0.5);
        double maxWeight = plugin.getConfig().getDouble("weight.max", 10.0);

        // --- Roll tier and weight ---
        String tier   = rollTier();
        String color  = plugin.getConfig().getString("tiers." + tier + ".color", "&7");
        double weight = ThreadLocalRandom.current().nextDouble(minWeight, maxWeight);
        weight = Math.round(weight * 100.0) / 100.0;

        // --- Build the item ---
        Material dropMat = getDropMaterial(block.getType());
        ItemStack item = new ItemStack(dropMat, 1);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(WEIGHT_KEY, PersistentDataType.DOUBLE, weight);
            pdc.set(TIER_KEY,   PersistentDataType.STRING, tier);
            pdc.set(CROP_KEY,   PersistentDataType.STRING, formatName(block.getType()));

            // Calculate price for lore display
            double basePrice      = plugin.getConfig().getDouble("prices.default", 1.0);
            double tierMultiplier = plugin.getConfig().getDouble("tiers." + tier + ".multiplier", 1.0);
            double price          = basePrice * tierMultiplier * weight;

            List<String> lore = new ArrayList<>();
            lore.add(colorize(color) + "Tier: " + capitalize(tier));
            lore.add("§7Weight: §f" + weight + " kg");
            lore.add("§7Price: §a$" + String.format("%.2f", price));
            meta.setLore(lore);

            meta.setDisplayName(colorize(color) + capitalize(tier) + " " + formatName(block.getType()));
            item.setItemMeta(meta);
        }

        // --- Cancel default drop, give ours instead ---
        event.setDropItems(false);
        Location dropLoc = block.getLocation().add(0.5, 0.5, 0.5);
        event.getPlayer().getWorld().dropItemNaturally(dropLoc, item);

        // --- Console log (no debug spam) ---
        double worth = plugin.getConfig().getDouble("prices.default", 1.0)
                * plugin.getConfig().getDouble("tiers." + tier + ".multiplier", 1.0) * weight;
        plugin.getLogger().info("✓ " + event.getPlayer().getName() + " harvested " + tier.toUpperCase()
                + " " + formatName(block.getType()) + " (" + weight + "kg) — $" + String.format("%.2f", worth));

        // --- Hologram flash if DecentHolograms is enabled ---
        if (plugin.isHoloEnabled()) {
            plugin.getHoloManager().flashHarvest(block.getLocation(), tier, color, weight, formatName(block.getType()));
        }
    }

    // ---------------------------------------------------------------
    // Tier rolling
    // ---------------------------------------------------------------
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

    // ---------------------------------------------------------------
    // Utility
    // ---------------------------------------------------------------
    private boolean isTrackedCrop(Material material) {
        for (Material m : TRACKED_CROPS) {
            if (m == material) return true;
        }
        return false;
    }

    private Material getDropMaterial(Material cropBlock) {
        if (cropBlock == Material.BEETROOT) return Material.BEETROOT;
        if (cropBlock == Material.MELON)    return Material.MELON_SLICE;
        return cropBlock;
    }

    public static String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public static String formatName(Material material) {
        String name = material.name();
        return name.charAt(0) + name.substring(1).toLowerCase().replace("_", " ");
    }

    public static String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
                }
        
