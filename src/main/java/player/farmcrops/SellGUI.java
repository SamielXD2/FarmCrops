package player.farmcrops;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
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

import java.util.*;
import java.util.Arrays;

/**
 * v0.6.0 Sell GUI — Crops are GROUPED by tier + crop type into single slots.
 *
 * Why: Each crop has a unique weight in its PDC, so Minecraft will never stack them
 * natively (ItemMeta must be identical for stacking). Instead of fighting that,
 * we group them visually in the GUI and track individual weights server-side.
 *
 * Layout (54-slot inventory):
 *   Rows 0-4 (slots 0-44): Grouped crop slots
 *   Row 5 (slots 45-53):   Bottom bar — glass panes, Sell All (49), Close (53)
 *
 * Each grouped slot displays:
 *   - Name: "<Tier> <Crop>" (e.g. "Rare Wheat")
 *   - Lore: count, combined weight, total value
 *   - Click: sells ONE item from the group
 *   - Shift+Click: sells ALL items in that group
 */
public class SellGUI implements Listener {

    private final FarmCrops plugin;

    // Maps player -> their open GUI inventory
    private final Map<Player, Inventory> playerGUIs = new HashMap<>();

    // Maps player -> the list of CropEntry objects backing their GUI
    // Each CropEntry holds the list of individual weights for one group
    private final Map<Player, List<CropEntry>> playerCropData = new HashMap<>();

    public SellGUI(FarmCrops plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────
    // Data class: represents a group of crops in one GUI slot
    // ─────────────────────────────────────────────
    static class CropEntry {
        final String tier;
        final Material cropType;   // The source block type (WHEAT, CARROTS, etc.)
        final Material dropType;   // What the item actually is (WHEAT, CARROT, etc.)
        final List<Double> weights; // Individual weight of each crop in this group
        final double basePrice;
        final double tierMultiplier;

        CropEntry(String tier, Material cropType, Material dropType, double basePrice, double tierMultiplier) {
            this.tier = tier;
            this.cropType = cropType;
            this.dropType = dropType;
            this.weights = new ArrayList<>();
            this.basePrice = basePrice;
            this.tierMultiplier = tierMultiplier;
        }

        double totalValue() {
            double total = 0;
            for (double w : weights) {
                total += basePrice * tierMultiplier * w;
            }
            return total;
        }

        double totalWeight() {
            double sum = 0;
            for (double w : weights) sum += w;
            return Math.round(sum * 100.0) / 100.0;
        }

        int count() {
            return weights.size();
        }

        // Sell one item (removes the first weight in the list)
        double sellOne() {
            if (weights.isEmpty()) return 0;
            double w = weights.remove(0);
            return basePrice * tierMultiplier * w;
        }

        // Sell all items in this group
        double sellAll() {
            double total = totalValue();
            weights.clear();
            return total;
        }

        // Generate the key used to group crops: "tier:cropType"
        String groupKey() {
            return tier + ":" + cropType.name();
        }
    }

    // ─────────────────────────────────────────────
    // Open GUI
    // ─────────────────────────────────────────────

    public void openGUI(Player player) {
        // Scan player inventory, group crops, remove them from inv
        List<CropEntry> entries = scanAndRemoveCrops(player);

        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.GREEN + "Sell Crops");

        // Bottom row: glass panes
        ItemStack grayGlass = createNamedItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            gui.setItem(i, grayGlass);
        }

        // Close button
        ItemStack closeBtn = createNamedItem(Material.BARRIER,
            ChatColor.RED + "" + ChatColor.BOLD + "CLOSE",
            ChatColor.GRAY + "Returns unsold crops to your inventory");
        gui.setItem(53, closeBtn);

        // Place grouped crop entries into slots 0-44
        for (int i = 0; i < entries.size() && i < 45; i++) {
            gui.setItem(i, buildGroupedSlot(entries.get(i)));
        }

        // Sell All button
        refreshSellAllButton(gui, entries);

        playerGUIs.put(player, gui);
        playerCropData.put(player, entries);
        player.openInventory(gui);
    }

    // ─────────────────────────────────────────────
    // Scan player inventory → build grouped CropEntry list
    // ─────────────────────────────────────────────

    private List<CropEntry> scanAndRemoveCrops(Player player) {
        // Use a map to group by "tier:cropType"
        Map<String, CropEntry> groups = new LinkedHashMap<>();

        ItemStack[] inv = player.getInventory().getContents();
        for (int i = 0; i < inv.length; i++) {
            ItemStack item = inv[i];
            if (item == null || item.getType().isAir() || !item.hasItemMeta()) continue;

            PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
            if (!pdc.has(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE)) continue;

            double weight = pdc.get(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE);
            String tier = pdc.getOrDefault(CropListener.TIER_KEY, PersistentDataType.STRING, "common");
            String cropName = pdc.getOrDefault(CropListener.CROP_KEY, PersistentDataType.STRING, null);

            // Determine crop type from stored key, or fall back to the item material
            Material cropType;
            if (cropName != null) {
                cropType = Material.valueOf(cropName);
            } else {
                cropType = item.getType(); // fallback for items from v0.5.0
            }

            Material dropType = item.getType();
            double basePrice = plugin.getCropPrice(cropType);
            double tierMult = plugin.getConfig().getDouble("tiers." + tier + ".multiplier", 1.0);

            String key = tier + ":" + cropType.name();
            CropEntry entry = groups.computeIfAbsent(key,
                k -> new CropEntry(tier, cropType, dropType, basePrice, tierMult));
            entry.weights.add(weight);

            // Remove from player inventory
            player.getInventory().setItem(i, null);
        }

        return new ArrayList<>(groups.values());
    }

    // ─────────────────────────────────────────────
    // Build the ItemStack displayed in a grouped GUI slot
    // ─────────────────────────────────────────────

    private ItemStack buildGroupedSlot(CropEntry entry) {
        if (entry.count() == 0) return null;

        String color = plugin.getConfig().getString("tiers." + entry.tier + ".color", "&7");
        String tierName = entry.tier.substring(0, 1).toUpperCase() + entry.tier.substring(1);
        String cropName = CropListener.formatName(entry.cropType);

        ItemStack item = new ItemStack(entry.dropType, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
            color + tierName + " " + cropName));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Count: " + ChatColor.WHITE + entry.count());
        lore.add(ChatColor.GRAY + "Total Weight: " + ChatColor.WHITE + entry.totalWeight() + " kg");
        lore.add(ChatColor.GRAY + "Total Value: " + ChatColor.GOLD + "$" + String.format("%.2f", entry.totalValue()));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click " + ChatColor.WHITE + "— Sell one");
        lore.add(ChatColor.YELLOW + "Shift+Click " + ChatColor.WHITE + "— Sell all in group");
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    // ─────────────────────────────────────────────
    // Click handler
    // ─────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (!playerGUIs.containsKey(player)) return;

        if (!event.getView().getTitle().equals(ChatColor.GREEN + "Sell Crops")) return;

        event.setCancelled(true);

        Inventory gui = playerGUIs.get(player);
        List<CropEntry> entries = playerCropData.get(player);

        // Ignore clicks on player's own inventory
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(gui)) return;

        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        // Sell All button (slot 49)
        if (slot == 49 && clicked.getType() == Material.EMERALD_BLOCK) {
            sellAll(player, gui, entries);
            return;
        }

        // Close button (slot 53)
        if (slot == 53 && clicked.getType() == Material.BARRIER) {
            returnCropsToPlayer(player, entries);
            player.closeInventory();
            cleanup(player);
            return;
        }

        // Bottom bar glass (slots 45-52 except 49) — ignore
        if (slot >= 45) return;

        // Crop slot (0-44)
        if (slot < entries.size()) {
            CropEntry entry = entries.get(slot);
            if (entry.count() == 0) return;

            Economy economy = plugin.getEconomy();

            if (event.isShiftClick()) {
                // Shift+Click = sell entire group
                double value = entry.sellAll();
                economy.depositPlayer(player, value);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                player.sendMessage(ChatColor.GREEN + "Sold group for " + ChatColor.GOLD + "$" + String.format("%.2f", value));
            } else {
                // Regular click = sell one
                double value = entry.sellOne();
                economy.depositPlayer(player, value);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                player.sendMessage(ChatColor.GREEN + "Sold 1x " + clicked.getItemMeta().getDisplayName()
                    + ChatColor.GREEN + " for " + ChatColor.GOLD + "$" + String.format("%.2f", value));
            }

            // Refresh the slot (update or clear it)
            if (entry.count() > 0) {
                gui.setItem(slot, buildGroupedSlot(entry));
            } else {
                gui.setItem(slot, null);
            }

            refreshSellAllButton(gui, entries);
        }
    }

    // ─────────────────────────────────────────────
    // Close handler — return unsold crops
    // ─────────────────────────────────────────────

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        if (!playerGUIs.containsKey(player)) return;

        if (!event.getView().getTitle().equals(ChatColor.GREEN + "Sell Crops")) return;

        List<CropEntry> entries = playerCropData.get(player);
        if (entries != null) {
            returnCropsToPlayer(player, entries);
        }
        cleanup(player);
    }

    // ─────────────────────────────────────────────
    // Sell All
    // ─────────────────────────────────────────────

    private void sellAll(Player player, Inventory gui, List<CropEntry> entries) {
        double totalEarnings = 0;
        int totalItems = 0;

        for (int i = 0; i < entries.size(); i++) {
            CropEntry entry = entries.get(i);
            if (entry.count() == 0) continue;

            totalItems += entry.count();
            totalEarnings += entry.sellAll();
            gui.setItem(i, null);
        }

        if (totalItems == 0) {
            player.sendMessage(ChatColor.RED + "You have no crops to sell!");
            return;
        }

        plugin.getEconomy().depositPlayer(player, totalEarnings);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.sendMessage(ChatColor.GREEN + "Sold " + totalItems + " crop(s) for "
            + ChatColor.GOLD + "$" + String.format("%.2f", totalEarnings) + ChatColor.GREEN + "!");

        player.closeInventory();
        cleanup(player);
    }

    // ─────────────────────────────────────────────
    // Return unsold crops to player inventory as individual items
    // ─────────────────────────────────────────────

    private void returnCropsToPlayer(Player player, List<CropEntry> entries) {
        for (CropEntry entry : entries) {
            for (double weight : entry.weights) {
                // Rebuild each individual item with its weight
                ItemStack item = new ItemStack(entry.dropType, 1);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    PersistentDataContainer pdc = meta.getPersistentDataContainer();
                    pdc.set(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE, weight);
                    pdc.set(CropListener.TIER_KEY, PersistentDataType.STRING, entry.tier);
                    pdc.set(CropListener.CROP_KEY, PersistentDataType.STRING, entry.cropType.name());

                    String color = plugin.getConfig().getString("tiers." + entry.tier + ".color", "&7");
                    String tierName = entry.tier.substring(0, 1).toUpperCase() + entry.tier.substring(1);
                    double price = entry.basePrice * entry.tierMultiplier * weight;

                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                        color + tierName + " " + CropListener.formatName(entry.cropType)));

                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.translateAlternateColorCodes('&', color + "Tier: " + tierName));
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7Weight: &f" + weight + " kg"));
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7Price: &a$" + String.format("%.2f", price)));
                    meta.setLore(lore);

                    item.setItemMeta(meta);
                }

                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
        }
    }

    // ─────────────────────────────────────────────
    // Refresh Sell All button with current totals
    // ─────────────────────────────────────────────

    private void refreshSellAllButton(Inventory gui, List<CropEntry> entries) {
        double totalValue = 0;
        int totalItems = 0;
        for (CropEntry e : entries) {
            totalValue += e.totalValue();
            totalItems += e.count();
        }

        ItemStack btn = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta = btn.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "SELL ALL");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Click to sell all crops");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Total Items: " + ChatColor.WHITE + totalItems);
            lore.add(ChatColor.YELLOW + "Total Value: " + ChatColor.GOLD + "$" + String.format("%.2f", totalValue));
            meta.setLore(lore);
            btn.setItemMeta(meta);
        }
        gui.setItem(49, btn);
    }

    // ─────────────────────────────────────────────
    // Cleanup
    // ─────────────────────────────────────────────

    private void cleanup(Player player) {
        playerGUIs.remove(player);
        playerCropData.remove(player);
    }

    // ─────────────────────────────────────────────
    // Utility
    // ─────────────────────────────────────────────

    private ItemStack createNamedItem(Material mat, String name, String... lore) {
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
}
