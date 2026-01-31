package player.farmcrops;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class SellCommand implements CommandExecutor {

    private final FarmCrops plugin;

    public SellCommand(FarmCrops plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Only players can run this
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        double totalEarnings = 0.0;
        int    totalItems    = 0;

        // --- Scan every slot in the player's inventory ---
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];

            // Skip empty or null slots
            if (item == null || item.getType().isAir()) {
                continue;
            }

            // Check if this item has our weight tag in its PDC
            if (!item.hasItemMeta()) continue;
            PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();

            if (!pdc.has(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE)) {
                continue; // Not one of our custom crops
            }

            // --- Read the stored data ---
            double weight = pdc.get(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE);
            String tier   = pdc.getOrDefault(CropListener.TIER_KEY, PersistentDataType.STRING, "common");

            // --- Calculate price: basePrice * tierMultiplier * weight ---
            // Base price defaults to 1.0 per kg if not configured
            double basePrice      = plugin.getConfig().getDouble("prices.default", 1.0);
            double tierMultiplier = plugin.getConfig().getDouble("tiers." + tier + ".multiplier", 1.0);

            double itemValue = basePrice * tierMultiplier * weight;

            // If there are multiple of the same item in a stack (unlikely for crops, but safe)
            itemValue *= item.getAmount();

            totalEarnings += itemValue;
            totalItems    += item.getAmount();

            // Remove the item from inventory
            player.getInventory().setItem(i, null);
        }

        // --- If nothing was sold, tell the player ---
        if (totalItems == 0) {
            player.sendMessage(ChatColor.RED + "You have no custom crops to sell.");
            return true;
        }

        // --- Pay the player via Vault ---
        plugin.getEconomy().depositPlayer(player, totalEarnings);

        // --- Send a receipt ---
        String currencyName = plugin.getEconomy().currencyNamePlural();
        player.sendMessage(ChatColor.GREEN + "Sold " + totalItems + " crop(s) for "
                + ChatColor.GOLD + String.format("%.2f", totalEarnings)
                + " " + currencyName + ChatColor.GREEN + ".");

        return true;
    }
                                             }
                           
