package player.farmcrops;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class PlaceholderProvider extends PlaceholderExpansion {

    private final FarmCrops plugin;

    public PlaceholderProvider(FarmCrops plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "farmcrops";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "Player";
    }

    @Override
    @NotNull
    public String getVersion() {
        return "0.5.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        // %farmcrops_price% - Shows sell price of held item
        if (identifier.equals("price")) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
                return "N/A";
            }

            PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
            if (!pdc.has(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE)) {
                return "N/A";
            }

            double weight = pdc.get(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE);
            String tier = pdc.getOrDefault(CropListener.TIER_KEY, PersistentDataType.STRING, "common");

            double basePrice = plugin.getConfig().getDouble("prices.default", 1.0);
            double tierMultiplier = plugin.getConfig().getDouble("tiers." + tier + ".multiplier", 1.0);
            double price = basePrice * tierMultiplier * weight;

            return String.format("%.2f", price);
        }

        // %farmcrops_tier% - Shows tier of held item
        if (identifier.equals("tier")) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
                return "N/A";
            }

            PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
            String tier = pdc.getOrDefault(CropListener.TIER_KEY, PersistentDataType.STRING, null);
            if (tier == null) {
                return "N/A";
            }

            return capitalize(tier);
        }

        // %farmcrops_weight% - Shows weight of held item
        if (identifier.equals("weight")) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
                return "N/A";
            }

            PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
            if (!pdc.has(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE)) {
                return "N/A";
            }

            double weight = pdc.get(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE);
            return String.format("%.2f", weight);
        }

        // %farmcrops_crop% - Shows crop type of held item
        if (identifier.equals("crop")) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType().isAir()) {
                return "N/A";
            }

            return formatName(item.getType());
        }

        return null;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private String formatName(org.bukkit.Material m) {
        return m.name().charAt(0) + m.name().substring(1).toLowerCase().replace("_", " ");
    }
}
