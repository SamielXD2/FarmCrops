package player.farmcrops;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SellGUI implements Listener {

    private final FarmCrops plugin;
    private final Map<Player, Inventory> playerGUIs = new HashMap<>();

    public SellGUI(FarmCrops plugin) {
        this.plugin = plugin;
    }

    public void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.GREEN + "Sell Crops");

        // Fill bottom row with glass panes
        ItemStack grayGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = grayGlass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            grayGlass.setItemMeta(glassMeta);
        }

        for (int i = 45; i < 54; i++) {
            gui.setItem(i, grayGlass);
        }

        // Add player's crops to top 4 rows
        loadPlayerCrops(player, gui);

        // Update "Sell All" button
        refreshSellAllButton(gui);

        // Create "Close" button
        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeButton.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "CLOSE");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Returns items to inventory");
            closeMeta.setLore(lore);
            closeButton.setItemMeta(closeMeta);
        }
        gui.setItem(53, closeButton);

        playerGUIs.put(player, gui);
        player.openInventory(gui);
    }

    private void loadPlayerCrops(Player player, Inventory gui) {
        int slot = 0;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            if (slot >= 45) break;

            ItemStack item = contents[i];
            if (item != null && !item.getType().isAir() && item.hasItemMeta()) {
                PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
                if (pdc.has(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE)) {
                    // Clone item and add price info to lore if not already there
                    ItemStack displayItem = item.clone();
                    ItemMeta meta = displayItem.getItemMeta();
                    
                    if (meta != null) {
                        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                        
                        // Check if price line already exists
                        boolean hasPriceLine = false;
                        for (String line : lore) {
                            if (line.contains("Price:") || line.contains("Value:")) {
                                hasPriceLine = true;
                                break;
                            }
                        }
                        
                        // Add price line if not present
                        if (!hasPriceLine) {
                            double weight = pdc.get(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE);
                            String tier = pdc.getOrDefault(CropListener.TIER_KEY, PersistentDataType.STRING, "common");
                            double basePrice = plugin.getConfig().getDouble("prices.default", 10.0);
                            double tierMultiplier = plugin.getConfig().getDouble("tiers." + tier + ".multiplier", 1.0);
                            double price = basePrice * tierMultiplier * weight;
                            
                            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Price: &a$" + String.format("%.2f", price)));
                            meta.setLore(lore);
                            displayItem.setItemMeta(meta);
                        }
                    }
                    
                    // Add to GUI
                    gui.setItem(slot++, displayItem);
                    
                    // CRITICAL: Remove from player inventory to prevent duplication
                    player.getInventory().setItem(i, null);
                }
            }
        }
    }

    private double calculateTotalValue(Inventory gui) {
        double total = 0.0;

        for (int i = 0; i < 45; i++) {
            ItemStack item = gui.getItem(i);
            if (item == null || item.getType().isAir() || !item.hasItemMeta()) continue;

            PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
            if (!pdc.has(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE)) continue;

            double weight = pdc.get(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE);
            String tier = pdc.getOrDefault(CropListener.TIER_KEY, PersistentDataType.STRING, "common");

            double basePrice = plugin.getConfig().getDouble("prices.default", 1.0);
            double tierMultiplier = plugin.getConfig().getDouble("tiers." + tier + ".multiplier", 1.0);

            total += basePrice * tierMultiplier * weight * item.getAmount();
        }

        return total;
    }

    private int countTotalCrops(Inventory gui) {
        int count = 0;

        for (int i = 0; i < 45; i++) {
            ItemStack item = gui.getItem(i);
            if (item != null && !item.getType().isAir() && item.hasItemMeta()) {
                PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
                if (pdc.has(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE)) {
                    count += item.getAmount();
                }
            }
        }

        return count;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (!playerGUIs.containsKey(player)) return;

        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
            .serialize(event.getView().title());
        if (!title.equals("Sell Crops")) return;

        Inventory gui = playerGUIs.get(player);

        // CRITICAL: Cancel ALL clicks to prevent any item manipulation
        event.setCancelled(true);

        // Also block shift-clicking from player inventory
        if (event.getClickedInventory() != null && 
            event.getClickedInventory().equals(player.getInventory()) && 
            event.isShiftClick()) {
            return; // Don't allow shift-clicking items into the GUI
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        // Only handle clicks in the GUI itself, not player inventory
        if (!event.getClickedInventory().equals(gui)) {
            return;
        }

        int slot = event.getSlot();

        // Sell All button (slot 49)
        if (slot == 49 && clicked.getType() == Material.EMERALD_BLOCK) {
            sellAllCrops(player, gui);
            return;
        }

        // Close button (slot 53)
        if (slot == 53 && clicked.getType() == Material.BARRIER) {
            returnCropsToPlayer(player, gui);
            player.closeInventory();
            playerGUIs.remove(player);
            return;
        }

        // Bottom row glass panes (45-52, excluding 49) - ignore
        if (slot >= 45 && slot <= 52 && slot != 49) {
            return;
        }

        // Top 4 rows (0-44) - Sell individual crop
        if (slot < 45 && clicked.hasItemMeta()) {
            PersistentDataContainer pdc = clicked.getItemMeta().getPersistentDataContainer();
            if (pdc.has(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE)) {
                sellSingleCrop(player, gui, slot, clicked);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        if (!playerGUIs.containsKey(player)) return;

        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
            .serialize(event.getView().title());
        if (title.equals("Sell Crops")) {
            Inventory gui = playerGUIs.get(player);
            returnCropsToPlayer(player, gui);
            playerGUIs.remove(player);
        }
    }

    private void sellSingleCrop(Player player, Inventory gui, int slot, ItemStack crop) {
        PersistentDataContainer pdc = crop.getItemMeta().getPersistentDataContainer();
        double weight = pdc.get(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE);
        String tier = pdc.getOrDefault(CropListener.TIER_KEY, PersistentDataType.STRING, "common");

        double basePrice = plugin.getConfig().getDouble("prices.default", 1.0);
        double tierMultiplier = plugin.getConfig().getDouble("tiers." + tier + ".multiplier", 1.0);

        double value = basePrice * tierMultiplier * weight * crop.getAmount();

        Economy economy = plugin.getEconomy();
        economy.depositPlayer(player, value);

        gui.setItem(slot, null);

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        player.sendMessage(ChatColor.GREEN + "Sold " + crop.getAmount() + "x " + 
                          crop.getItemMeta().getDisplayName() + ChatColor.GREEN + " for " + 
                          ChatColor.GOLD + "$" + String.format("%.2f", value));

        refreshSellAllButton(gui);
    }

    private void sellAllCrops(Player player, Inventory gui) {
        double totalEarnings = 0.0;
        int totalItems = 0;

        for (int i = 0; i < 45; i++) {
            ItemStack item = gui.getItem(i);
            if (item == null || item.getType().isAir() || !item.hasItemMeta()) continue;

            PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
            if (!pdc.has(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE)) continue;

            double weight = pdc.get(CropListener.WEIGHT_KEY, PersistentDataType.DOUBLE);
            String tier = pdc.getOrDefault(CropListener.TIER_KEY, PersistentDataType.STRING, "common");

            double basePrice = plugin.getConfig().getDouble("prices.default", 1.0);
            double tierMultiplier = plugin.getConfig().getDouble("tiers." + tier + ".multiplier", 1.0);

            totalEarnings += basePrice * tierMultiplier * weight * item.getAmount();
            totalItems += item.getAmount();

            gui.setItem(i, null);
        }

        if (totalItems == 0) {
            player.sendMessage(ChatColor.RED + "You have no crops to sell!");
            return;
        }

        Economy economy = plugin.getEconomy();
        economy.depositPlayer(player, totalEarnings);

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.sendMessage(ChatColor.GREEN + "Sold " + totalItems + " crop(s) for " + 
                          ChatColor.GOLD + "$" + String.format("%.2f", totalEarnings) + ChatColor.GREEN + "!");

        player.closeInventory();
        playerGUIs.remove(player);
    }

    private void refreshSellAllButton(Inventory gui) {
        ItemStack sellAllButton = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta sellMeta = sellAllButton.getItemMeta();

        if (sellMeta != null) {
            sellMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "SELL ALL");

            double totalValue = calculateTotalValue(gui);
            int totalItems = countTotalCrops(gui);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Click to sell all crops");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Total Items: " + ChatColor.WHITE + totalItems);
            lore.add(ChatColor.YELLOW + "Total Value: " + ChatColor.GOLD + "$" + String.format("%.2f", totalValue));

            sellMeta.setLore(lore);
            sellAllButton.setItemMeta(sellMeta);
        }

        gui.setItem(49, sellAllButton);
    }

    private void returnCropsToPlayer(Player player, Inventory gui) {
        for (int i = 0; i < 45; i++) {
            ItemStack item = gui.getItem(i);
            if (item != null && !item.getType().isAir()) {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
        }
    }
}
