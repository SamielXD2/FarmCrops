package player.farmcrops;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * v0.8.0 Stats GUI - Visual display replacing chat output
 * 
 * Layout (54-slot):
 * Row 1: Overview (harvests, earnings, best drop, heaviest)
 * Row 2: Tier breakdown (Common, Rare, Epic, Legendary, Mythic)
 * Row 3-4: Per-crop breakdown
 * Row 5: Navigation (back button)
 */
public class StatsGUI implements Listener {

    private final FarmCrops plugin;
    private final Map<Player, Inventory> playerGUIs = new HashMap<>();

    public StatsGUI(FarmCrops plugin) {
        this.plugin = plugin;
    }

    public void openGUI(Player player) {
        StatsManager.PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());
        
        Inventory gui = Bukkit.createInventory(null, 54, 
            ChatColor.GREEN + "📊 " + player.getName() + "'s Stats");

        // Fill background
        ItemStack bgGlass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, bgGlass);
        }

        // === ROW 1: OVERVIEW ===
        // Total Harvests
        gui.setItem(10, createItem(Material.WHEAT,
            ChatColor.YELLOW + "" + ChatColor.BOLD + "Total Harvests",
            ChatColor.GRAY + "All crops combined",
            "",
            ChatColor.WHITE + "🌾 " + stats.totalHarvests + " crops"));

        // Total Earnings
        gui.setItem(11, createItem(Material.EMERALD,
            ChatColor.GOLD + "" + ChatColor.BOLD + "Total Earnings",
            ChatColor.GRAY + "Money from selling crops",
            "",
            ChatColor.GREEN + "💰 $" + String.format("%.2f", stats.totalEarnings)));

        // Best Drop
        if (stats.bestDropValue > 0) {
            String bestCrop = stats.bestDropCrop.equals("none") ? "Unknown" 
                : CropListener.formatName(Material.valueOf(stats.bestDropCrop));
            gui.setItem(12, createItem(Material.GOLD_INGOT,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "Best Drop",
                ChatColor.GRAY + "Your most valuable single crop",
                "",
                ChatColor.GOLD + "💎 $" + String.format("%.2f", stats.bestDropValue),
                ChatColor.GRAY + capitalize(stats.bestDropTier) + " " + bestCrop,
                ChatColor.GRAY + "" + stats.bestDropWeight + " kg"));
        }

        // Heaviest Crop
        if (stats.heaviestWeight > 0) {
            String heavyCrop = stats.heaviestCrop.equals("none") ? "Unknown"
                : CropListener.formatName(Material.valueOf(stats.heaviestCrop));
            gui.setItem(13, createItem(Material.IRON_BLOCK,
                ChatColor.AQUA + "" + ChatColor.BOLD + "Heaviest Crop",
                ChatColor.GRAY + "Your biggest harvest",
                "",
                ChatColor.WHITE + "⚖ " + stats.heaviestWeight + " kg",
                ChatColor.GRAY + capitalize(stats.heaviestTier) + " " + heavyCrop));
        }

        // === ROW 2: TIER BREAKDOWN ===
        gui.setItem(19, createItem(Material.STONE,
            ChatColor.WHITE + "Common",
            ChatColor.GRAY + "70% chance",
            "",
            ChatColor.WHITE + "🌾 " + stats.commonHarvests + " harvests"));

        gui.setItem(20, createItem(Material.DIAMOND,
            ChatColor.AQUA + "Rare",
            ChatColor.GRAY + "19% chance",
            "",
            ChatColor.AQUA + "🌾 " + stats.rareHarvests + " harvests"));

        gui.setItem(21, createItem(Material.AMETHYST_SHARD,
            ChatColor.LIGHT_PURPLE + "Epic",
            ChatColor.GRAY + "7% chance",
            "",
            ChatColor.LIGHT_PURPLE + "🌾 " + stats.epicHarvests + " harvests"));

        gui.setItem(22, createItem(Material.GOLD_INGOT,
            ChatColor.GOLD + "Legendary",
            ChatColor.GRAY + "3% chance",
            "",
            ChatColor.GOLD + "🌾 " + stats.legendaryHarvests + " harvests"));

        gui.setItem(23, createItem(Material.REDSTONE,
            ChatColor.RED + "Mythic",
            ChatColor.GRAY + "1% chance",
            "",
            ChatColor.RED + "🌾 " + stats.mythicHarvests + " harvests"));

        // === ROW 3-4: PER-CROP BREAKDOWN ===
        if (!stats.cropHarvests.isEmpty()) {
            int slot = 28;
            for (Map.Entry<String, Integer> entry : stats.cropHarvests.entrySet()) {
                if (slot >= 44) break;
                
                try {
                    Material cropMat = getCropDisplayMaterial(Material.valueOf(entry.getKey()));
                    String cropName = CropListener.formatName(Material.valueOf(entry.getKey()));
                    double earnings = stats.cropEarnings.getOrDefault(entry.getKey(), 0.0);
                    
                    gui.setItem(slot, createItem(cropMat,
                        ChatColor.YELLOW + cropName,
                        ChatColor.GRAY + "Harvests & earnings",
                        "",
                        ChatColor.WHITE + "🌾 " + entry.getValue() + " harvests",
                        ChatColor.GOLD + "💰 $" + String.format("%.2f", earnings)));
                    
                    slot++;
                } catch (IllegalArgumentException e) {
                    // Skip invalid crop names
                }
            }
        }

        // === ROW 5: NAVIGATION ===
        ItemStack grayGlass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            gui.setItem(i, grayGlass);
        }

        gui.setItem(49, createItem(Material.BARRIER,
            ChatColor.RED + "← Back to Menu",
            ChatColor.GRAY + "Return to main menu"));

        playerGUIs.put(player, gui);
        player.openInventory(gui);
    }

    private Material getCropDisplayMaterial(Material cropType) {
        switch (cropType) {
            case WHEAT: return Material.WHEAT;
            case CARROTS: return Material.CARROT;
            case POTATOES: return Material.POTATO;
            case BEETROOTS: return Material.BEETROOT;
            case MELON: return Material.MELON_SLICE;
            default: return Material.WHEAT;
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (!playerGUIs.containsKey(player)) return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!title.contains("Stats")) return;

        event.setCancelled(true);

        Inventory gui = playerGUIs.get(player);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(gui)) return;

        int slot = event.getSlot();

        if (slot == 49) { // Back button
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            player.closeInventory();
            playerGUIs.remove(player);
            plugin.getMainMenuGUI().openGUI(player);
        }
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
