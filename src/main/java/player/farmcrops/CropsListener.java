package player.farmcrops;

import org.bukkit.*;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class CropListener implements Listener {

    private final FarmCrops plugin;

    // Namespaced keys for storing data inside the item's NBT
    public static final NamespacedKey WEIGHT_KEY = new NamespacedKey("farmcrops", "weight");
    public static final NamespacedKey TIER_KEY   = new NamespacedKey("farmcrops", "tier");

    // Which crop materials this plugin will intercept
    private static final Material[] TRACKED_CROPS = {
            Material.WHEAT, Material.CARROT, Material.POTATO,
            Material.BEETROOT, Material.MELON_SLICE
    };

    public CropListener(FarmCrops plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        // Only care about crops we're tracking
        if (!isTrackedCrop(block.getType())) {
            return;
        }

        // Only intercept FULLY GROWN crops
        if (!(block.getBlockData() instanceof Ageable ageable)) {
            return;
        }
        if (ageable.getAge() < ageable.getMaximumAge()) {
            return; // Not fully grown yet — let normal break happen
        }

        // --- Read config values ---
        double minWeight = plugin.getConfig().getDouble("weight.min", 0.5);
        double maxWeight = plugin.getConfig().getDouble("weight.max", 10.0);

        // --- Roll the tier ---
        String tier   = rollTier();
        double multiplier = plugin.getConfig().getDouble("tiers." + tier + ".multiplier", 1.0);
        String color  = plugin.getConfig().getString("tiers." + tier + ".color", "&7");

        // --- Roll the weight ---
        double weight = ThreadLocalRandom.current().nextDouble(minWeight, maxWeight);
        // Round to 2 decimal places for display
        weight = Math.round(weight * 100.0) / 100.0;

        // --- Build the custom item ---
        ItemStack item = new ItemStack(getDropMaterial(block.getType()), 1);
        item.editMeta(meta -> {
            // --- Stamp weight + tier into PersistentDataContainer (cheat-proof) ---
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(WEIGHT_KEY, PersistentDataType.DOUBLE, weight);
            pdc.set(TIER_KEY,   PersistentDataType.STRING, tier);

            // --- Set visible lore so players can see it ---
            List<String> lore = new ArrayList<>();
            lore.add(colorize(color) + "Tier: " + tier.substring(0, 1).toUpperCase() + tier.substring(1));
            lore.add("§7Weight: §f" + weight + " kg");
            meta.setLore(lore);

            // Optional: rename the item to include tier
            meta.setDisplayName(colorize(color) + tier.substring(0, 1).toUpperCase()
                    + tier.substring(1) + " " + formatName(block.getType()));
        });

        // --- Cancel the default drop and give our custom item instead ---
        event.setDropItems(false);
        event.getPlayer().getWorld().dropItemNaturally(block.getLocation(), item);
    }

    // ---------------------------------------------------------------
    // Tier rolling logic — reads chances from config.yml
    // ---------------------------------------------------------------
    private String rollTier() {
        int roll = ThreadLocalRandom.current().nextInt(1, 101); // 1–100

        int commonChance     = plugin.getConfig().getInt("tiers.common.chance", 70);
        int rareChance       = plugin.getConfig().getInt("tiers.rare.chance", 20);
        int epicChance       = plugin.getConfig().getInt("tiers.epic.chance", 7);
        // Legendary gets whatever's left (default 3)

        if (roll <= commonChance) {
            return "common";
        } else if (roll <= commonChance + rareChance) {
            return "rare";
        } else if (roll <= commonChance + rareChance + epicChance) {
            return "epic";
        } else {
            return "legendary";
        }
    }

    // ---------------------------------------------------------------
    // Utility helpers
    // ---------------------------------------------------------------
    private boolean isTrackedCrop(Material material) {
        for (Material m : TRACKED_CROPS) {
            if (m == material) return true;
        }
        return false;
    }

    private Material getDropMaterial(Material cropBlock) {
        // Beetroot block drops beetroots, melon block drops melon slice, etc.
        return switch (cropBlock) {
            case BEETROOT -> Material.BEETROOT;
            case MELON    -> Material.MELON_SLICE;
            default       -> cropBlock; // WHEAT, CARROT, POTATO drop themselves
        };
    }

    private String formatName(Material material) {
        // "BEETROOT" → "Beetroot"
        return material.name().charAt(0)
                + material.name().substring(1).toLowerCase().replace("_", " ");
    }

    // Converts §-style color codes (e.g. "&7") to Bukkit format
    private String colorize(String input) {
        return org.bukkit.ChatColor.translateAlternateColorCode('&', input);
    }
                  }
  
