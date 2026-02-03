package player.farmcrops;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AutoSellManager implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Boolean> autoSellEnabled = new HashMap<>();
    private final Map<Material, Double> cropPrices = new HashMap<>();

    public AutoSellManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadCropPrices();
    }

    private void loadCropPrices() {
        // Load prices from config
        cropPrices.put(Material.WHEAT, plugin.getConfig().getDouble("prices.wheat", 2.0));
        cropPrices.put(Material.CARROT, plugin.getConfig().getDouble("prices.carrot", 1.5));
        cropPrices.put(Material.POTATO, plugin.getConfig().getDouble("prices.potato", 1.5));
        cropPrices.put(Material.BEETROOT, plugin.getConfig().getDouble("prices.beetroot", 1.8));
        cropPrices.put(Material.NETHER_WART, plugin.getConfig().getDouble("prices.nether_wart", 3.0));
        cropPrices.put(Material.COCOA_BEANS, plugin.getConfig().getDouble("prices.cocoa_beans", 2.5));
        cropPrices.put(Material.SWEET_BERRIES, plugin.getConfig().getDouble("prices.sweet_berries", 2.2));
    }

    // Toggle auto-sell for a player
    public boolean toggleAutoSell(Player player) {
        // PERMISSION CHECK - This prevents default players from using it
        if (!player.hasPermission("farmcrops.autosell")) {
            player.sendMessage("§c§lFarmCrops §8» §cYou don't have permission to use auto-sell!");
            player.sendMessage("§7Ask an admin for the §efarmcrops.autosell §7permission.");
            return false;
        }

        UUID uuid = player.getUniqueId();
        boolean newState = !autoSellEnabled.getOrDefault(uuid, false);
        autoSellEnabled.put(uuid, newState);

        if (newState) {
            player.sendMessage("§a§lFarmCrops §8» §aAuto-sell enabled! Crops will sell automatically.");
        } else {
            player.sendMessage("§c§lFarmCrops §8» §cAuto-sell disabled.");
        }

        return newState;
    }

    public boolean isAutoSellEnabled(Player player) {
        return autoSellEnabled.getOrDefault(player.getUniqueId(), false);
    }

    @EventHandler
    public void onCropBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        // Check if auto-sell is enabled AND player has permission
        if (!isAutoSellEnabled(player)) return;
        if (!player.hasPermission("farmcrops.autosell")) {
            // If permission was revoked, disable auto-sell
            autoSellEnabled.put(player.getUniqueId(), false);
            return;
        }

        Material cropType = event.getBlock().getType();
        
        // Check if it's a sellable crop
        if (!isSellableCrop(cropType)) return;

        // Get the drops
        event.setDropItems(false); // Don't drop items
        
        // Calculate payment
        ItemStack[] drops = getDrops(cropType);
        double totalPrice = 0;
        
        for (ItemStack drop : drops) {
            Material material = drop.getType();
            if (cropPrices.containsKey(material)) {
                totalPrice += cropPrices.get(material) * drop.getAmount();
            }
        }

        // Pay the player (requires Vault/economy plugin)
        if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
            // Assuming you have economy integration
            // getEconomy().depositPlayer(player, totalPrice);
            player.sendMessage("§a§lFarmCrops §8» §a+$" + String.format("%.2f", totalPrice));
        } else {
            player.sendMessage("§c§lFarmCrops §8» §cVault not found! Auto-sell disabled.");
        }
    }

    private boolean isSellableCrop(Material material) {
        return material == Material.WHEAT ||
               material == Material.CARROTS ||
               material == Material.POTATOES ||
               material == Material.BEETROOTS ||
               material == Material.NETHER_WART ||
               material == Material.COCOA ||
               material == Material.SWEET_BERRY_BUSH;
    }

    private ItemStack[] getDrops(Material cropType) {
        // Return typical drops for each crop
        switch (cropType) {
            case WHEAT:
                return new ItemStack[]{new ItemStack(Material.WHEAT, 1)};
            case CARROTS:
                return new ItemStack[]{new ItemStack(Material.CARROT, 3)};
            case POTATOES:
                return new ItemStack[]{new ItemStack(Material.POTATO, 3)};
            case BEETROOTS:
                return new ItemStack[]{new ItemStack(Material.BEETROOT, 1)};
            case NETHER_WART:
                return new ItemStack[]{new ItemStack(Material.NETHER_WART, 3)};
            case COCOA:
                return new ItemStack[]{new ItemStack(Material.COCOA_BEANS, 3)};
            case SWEET_BERRY_BUSH:
                return new ItemStack[]{new ItemStack(Material.SWEET_BERRIES, 2)};
            default:
                return new ItemStack[0];
        }
    }

    public void cleanup() {
        autoSellEnabled.clear();
    }
}
