package player.farmcrops;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI integration.
 *
 * Placeholders:
 *   %farmcrops_price%           — Sell price of item in main hand
 *   %farmcrops_tier%            — Tier of item in main hand
 *   %farmcrops_weight%          — Weight of item in main hand
 *   %farmcrops_crop%            — Crop type of item in main hand
 *   %farmcrops_stats_harvests%  — Player's total harvests
 *   %farmcrops_stats_earnings%  — Player's total earnings
 */
public class PlaceholderProvider extends PlaceholderExpansion {

    private final FarmCrops plugin;

    public PlaceholderProvider(FarmCrops plugin) {
        this.plugin = plugin;
    }

    @Override @NotNull public String getIdentifier() { return "farmcrops"; }
    @Override @NotNull public String getAuthor()     { return "Player"; }
    @Override @NotNull public String getVersion()    { return "0.6.0"; }
    @Override public boolean persist()               { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) return "";

        switch (identifier) {
            case "price": {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType().isAir() || !item.hasItemMeta()) return "N/A";

                PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
                if (!pdc.has(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE)) return "N/A";

                double weight = pdc.get(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE);
                String tier = pdc.getOrDefault(CropListener.TIER_KEY, PersistentDataType.STRING, "common");

                // Use per-crop pricing — determine crop type from PDC or fall back to default
                String cropName = pdc.getOrDefault(CropListener.CROP_KEY, PersistentDataType.STRING, null);
                double basePrice;
                if (cropName != null) {
                    try {
                        basePrice = plugin.getCropPrice(org.bukkit.Material.valueOf(cropName));
                    } catch (IllegalArgumentException e) {
                        basePrice = plugin.getConfig().getDouble("prices.default", 10.0);
                    }
                } else {
                    basePrice = plugin.getConfig().getDouble("prices.default", 10.0);
                }

                double tierMultiplier = plugin.getConfig().getDouble("tiers." + tier + ".multiplier", 1.0);
                return String.format("%.2f", basePrice * tierMultiplier * weight);
            }

            case "tier": {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType().isAir() || !item.hasItemMeta()) return "N/A";

                PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
                String tier = pdc.getOrDefault(CropListener.TIER_KEY, PersistentDataType.STRING, null);
                return tier != null ? capitalize(tier) : "N/A";
            }

            case "weight": {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType().isAir() || !item.hasItemMeta()) return "N/A";

                PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
                if (!pdc.has(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE)) return "N/A";

                return String.format("%.2f", pdc.get(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE));
            }

            case "crop": {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType().isAir()) return "N/A";

                PersistentDataContainer pdc = item.hasItemMeta()
                    ? item.getItemMeta().getPersistentDataContainer() : null;

                if (pdc != null && pdc.has(CropListener.CROP_KEY, PersistentDataType.STRING)) {
                    String cropName = pdc.get(CropListener.CROP_KEY, PersistentDataType.STRING);
                    try {
                        return CropListener.formatName(org.bukkit.Material.valueOf(cropName));
                    } catch (IllegalArgumentException e) {
                        // fall through
                    }
                }
                return CropListener.formatName(item.getType());
            }

            // Stats placeholders
            case "stats_harvests": {
                StatsManager.PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());
                return String.valueOf(stats.totalHarvests);
            }

            case "stats_earnings": {
                StatsManager.PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());
                return String.format("%.2f", stats.totalEarnings);
            }

            default:
                return null;
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
