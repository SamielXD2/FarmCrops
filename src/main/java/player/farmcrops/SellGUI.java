package player.farmcrops;

import org.bukkit.*;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SellGUI implements Listener {

    private final FarmCrops plugin;

    // Title the GUI inventory uses — we match on this to know if a click is inside our GUI
    public static final String GUI_TITLE = ChatColor.DARK_GREEN + "FarmCrops — Sell Crops";

    // Slots for the buttons (bottom row of a 54-slot chest)
    private static final int SLOT_SELL_ALL  = 49; // center bottom
    private static final int SLOT_CLOSE     = 53; // bottom right

    public SellGUI(FarmCrops plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------
    // Opens the sell GUI for the player
    // ---------------------------------------------------------------
    public static void openSellGUI(Player player, FarmCrops plugin) {
        // 54 slots = 6 rows. Top 4 rows (0–44) show crops. Row 5 (45–53) is the button bar.
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);

        // --- Fill top rows with the player's farmcrops items ---
        int slot = 0;
        double totalValue = 0.0;
        int totalCount = 0;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir() || !item.hasItemMeta()) continue;

            PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
            if (!pdc.has(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE)) continue;

            // It's one of ours — put it in the GUI (slots 0–44 only)
            if (slot > 44) break; // GUI full

            gui.setItem(slot, item.clone());
            slot++;

            // Tally up value
            double weight = pdc.get(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE);
            String tier   = pdc.getOrDefault(CropListener.TIER_KEY, PersistentDataType.STRING, "common");
            double basePrice      = plugin.getConfig().getDouble("prices.default", 1.0);
            double tierMultiplier = plugin.getConfig().getDouble("tiers." + tier + ".multiplier", 1.0);
            totalValue += basePrice * tierMultiplier * weight * item.getAmount();
            totalCount += item.getAmount();
        }

        // --- Fill remaining crop slots with glass panes (visual filler) ---
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        for (int i = slot; i <= 44; i++) {
            gui.setItem(i, filler);
        }

        // --- Bottom row: buttons ---
        // Filler for bottom row background
        ItemStack bottomFiller = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bfMeta = bottomFiller.getItemMeta();
        bfMeta.setDisplayName(" ");
        bottomFiller.setItemMeta(bfMeta);
        for (int i = 45; i <= 53; i++) {
            gui.setItem(i, bottomFiller);
        }

        // Sell All button
        ItemStack sellAllBtn = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta sellMeta = sellAllBtn.getItemMeta();
        sellMeta.setDisplayName(ChatColor.GREEN + "Sell All Crops");
        List<String> sellLore = new ArrayList<>();
        sellLore.add(ChatColor.GRAY + "Total crops: " + ChatColor.WHITE + totalCount);
        sellLore.add(ChatColor.GRAY + "Total value: " + ChatColor.GOLD + "$" + String.format("%.2f", totalValue));
        sellLore.add("");
        sellLore.add(ChatColor.YELLOW + "Click to sell everything!");
        sellMeta.setLore(sellLore);
        sellAllBtn.setItemMeta(sellMeta);
        gui.setItem(SLOT_SELL_ALL, sellAllBtn);

        // Close button
        ItemStack closeBtn = new ItemStack(Material.RED_CONCRETE);
        ItemMeta closeMeta = closeBtn.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "Close");
        closeBtn.setItemMeta(closeMeta);
        gui.setItem(SLOT_CLOSE, closeBtn);

        player.openInventory(gui);
    }

    // ---------------------------------------------------------------
    // Click handler
    // ---------------------------------------------------------------
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        // Check if this click is inside our GUI
        if (!event.getInventory().getName().equals(GUI_TITLE)) return;

        event.setCancelled(true); // Block all normal interactions in this GUI
        Player player = (Player) event.getPlayer();
        int slot = event.getRawSlot();

        if (slot == SLOT_SELL_ALL) {
            sellAll(player);
        } else if (slot == SLOT_CLOSE) {
            player.closeInventory();
        } else if (slot >= 0 && slot <= 44) {
            // Clicked on a crop in the GUI — sell just that one
            ItemStack clicked = event.getInventory().getItem(slot);
            if (clicked == null || clicked.getType().isAir()) return;
            if (!clicked.hasItemMeta()) return;

            PersistentDataContainer pdc = clicked.getItemMeta().getPersistentDataContainer();
            if (!pdc.has(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE)) return;

            double weight = pdc.get(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE);
            String tier   = pdc.getOrDefault(CropListener.TIER_KEY, PersistentDataType.STRING, "common");
            double basePrice      = plugin.getConfig().getDouble("prices.default", 1.0);
            double tierMultiplier = plugin.getConfig().getDouble("tiers." + tier + ".multiplier", 1.0);
            double itemValue      = basePrice * tierMultiplier * weight * clicked.getAmount();

            // Pay and remove from GUI
            plugin.getEconomy().depositPlayer(player, itemValue);
            event.getInventory().setItem(slot, filler());

            // Also remove from player's actual inventory
            removeFromPlayerInventory(player, clicked);

            String currency = plugin.getEconomy().currencyNamePlural();
            player.sendMessage(ChatColor.GREEN + "Sold " + ChatColor.WHITE + clicked.getAmount()
                    + "x " + CropListener.capitalize(tier) + " crop"
                    + ChatColor.GREEN + " for " + ChatColor.GOLD + "$" + String.format("%.2f", itemValue)
                    + " " + currency);

            // Refresh the GUI so the sell all button totals update
            player.closeInventory();
            openSellGUI(player, plugin);
        }
    }

    // ---------------------------------------------------------------
    // Sell all crops at once
    // ---------------------------------------------------------------
    private void sellAll(Player player) {
        double totalEarnings = 0.0;
        int totalItems = 0;

        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType().isAir() || !item.hasItemMeta()) continue;

            PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
            if (!pdc.has(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE)) continue;

            double weight = pdc.get(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE);
            String tier   = pdc.getOrDefault(CropListener.TIER_KEY, PersistentDataType.STRING, "common");
            double basePrice      = plugin.getConfig().getDouble("prices.default", 1.0);
            double tierMultiplier = plugin.getConfig().getDouble("tiers." + tier + ".multiplier", 1.0);

            totalEarnings += basePrice * tierMultiplier * weight * item.getAmount();
            totalItems += item.getAmount();

            player.getInventory().setItem(i, null);
        }

        if (totalItems == 0) {
            player.sendMessage(ChatColor.RED + "You have no crops to sell!");
            player.closeInventory();
            return;
        }

        plugin.getEconomy().depositPlayer(player, totalEarnings);

        String currency = plugin.getEconomy().currencyNamePlural();
        player.sendMessage(ChatColor.GREEN + "Sold " + ChatColor.WHITE + totalItems
                + " crop(s)" + ChatColor.GREEN + " for " + ChatColor.GOLD + "$"
                + String.format("%.2f", totalEarnings) + " " + currency);

        plugin.getLogger().info(player.getName() + " sold " + totalItems + " crops for $"
                + String.format("%.2f", totalEarnings));

        player.closeInventory();
    }

    // ---------------------------------------------------------------
    // Utility
    // ---------------------------------------------------------------
    private ItemStack filler() {
        ItemStack f = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta m = f.getItemMeta();
        m.setDisplayName(" ");
        f.setItemMeta(m);
        return f;
    }

    private void removeFromPlayerInventory(Player player, ItemStack guiItem) {
        // Match by PDC data to find and remove the exact item from the real inventory
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || !item.hasItemMeta()) continue;
            if (item.getItemMeta().getPersistentDataContainer()
                    .has(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE)) {

                double realWeight = item.getItemMeta().getPersistentDataContainer()
                        .get(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE);
                double guiWeight  = guiItem.getItemMeta().getPersistentDataContainer()
                        .get(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE);

                String realTier = item.getItemMeta().getPersistentDataContainer()
                        .getOrDefault(CropListener.TIER_KEY, PersistentDataType.STRING, "common");
                String guiTier  = guiItem.getItemMeta().getPersistentDataContainer()
                        .getOrDefault(CropListener.TIER_KEY, PersistentDataType.STRING, "common");

                // Match on weight + tier + material
                if (realWeight == guiWeight && realTier.equals(guiTier) && item.getType() == guiItem.getType()) {
                    player.getInventory().setItem(i, null);
                    return;
                }
            }
        }
    }
  }
  
