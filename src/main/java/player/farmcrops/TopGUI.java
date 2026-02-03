package player.farmcrops;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * v0.8.0 Leaderboard GUI - Replaces chat-based /farmtop
 * 
 * Features:
 * - Player heads for top 10
 * - Medal emojis for top 3
 * - Your entry highlighted in yellow
 * - Sort toggle (earnings ↔ harvests)
 * - Page navigation
 * - Back to main menu button
 */
public class TopGUI implements Listener {

    private final FarmCrops plugin;
    private final Map<Player, Inventory> playerGUIs = new HashMap<>();
    private final Map<Player, Integer> playerPages = new HashMap<>();
    private final Map<Player, String> playerSortMode = new HashMap<>();

    public TopGUI(FarmCrops plugin) {
        this.plugin = plugin;
    }

    public void openGUI(Player player, int page) {
        String sortBy = playerSortMode.getOrDefault(player, 
            plugin.getConfig().getString("leaderboard.sort-by", "earnings"));
        
        int pageSize = 10;
        int totalNeeded = page * pageSize + pageSize; // Get extra for next page check
        List<StatsManager.LeaderboardEntry> entries = plugin.getStatsManager().getLeaderboard(totalNeeded);
        
        int totalPages = (int) Math.ceil((double) entries.size() / pageSize);
        if (totalPages == 0) totalPages = 1;
        if (page > totalPages) page = totalPages;
        if (page < 1) page = 1;

        Inventory gui = Bukkit.createInventory(null, 54, 
            ChatColor.GOLD + "🏆 Top Farmers " + ChatColor.GRAY + "(Page " + page + "/" + totalPages + ")");

        // Fill background
        ItemStack bgGlass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, bgGlass);
        }

        // Display top 10 players for this page
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, entries.size());

        for (int i = startIndex; i < endIndex; i++) {
            StatsManager.LeaderboardEntry entry = entries.get(i);
            int rank = i + 1;
            int guiSlot = 10 + ((i - startIndex) % 5) + ((i - startIndex) / 5) * 9;
            
            gui.setItem(guiSlot, createPlayerHead(entry, rank, player, sortBy));
        }

        // Navigation buttons
        ItemStack grayGlass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            gui.setItem(i, grayGlass);
        }

        // Previous page
        if (page > 1) {
            gui.setItem(45, createItem(Material.ARROW,
                ChatColor.YELLOW + "← Previous Page",
                ChatColor.GRAY + "Go to page " + (page - 1)));
        }

        // Sort toggle
        Material sortMat = "earnings".equalsIgnoreCase(sortBy) ? Material.EMERALD : Material.WHEAT;
        gui.setItem(49, createItem(sortMat,
            ChatColor.GOLD + "Sort: " + ChatColor.YELLOW + capitalize(sortBy),
            ChatColor.GRAY + "Click to toggle",
            "",
            "earnings".equalsIgnoreCase(sortBy) 
                ? ChatColor.GREEN + "● Earnings" : ChatColor.GRAY + "○ Earnings",
            "harvests".equalsIgnoreCase(sortBy)
                ? ChatColor.GREEN + "● Harvests" : ChatColor.GRAY + "○ Harvests"));

        // Next page
        if (page < totalPages) {
            gui.setItem(53, createItem(Material.ARROW,
                ChatColor.YELLOW + "Next Page →",
                ChatColor.GRAY + "Go to page " + (page + 1)));
        }

        // Back button
        gui.setItem(48, createItem(Material.BARRIER,
            ChatColor.RED + "← Back to Menu",
            ChatColor.GRAY + "Return to main menu"));

        playerGUIs.put(player, gui);
        playerPages.put(player, page);
        player.openInventory(gui);
    }

    private ItemStack createPlayerHead(StatsManager.LeaderboardEntry entry, int rank, Player viewer, String sortBy) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        
        if (meta != null) {
            // Try to get player from UUID
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.uuid);
            meta.setOwningPlayer(offlinePlayer);

            // Medal for top 3
            String rankPrefix;
            if (rank == 1) rankPrefix = ChatColor.GOLD + "🥇 #1 ";
            else if (rank == 2) rankPrefix = ChatColor.GRAY + "🥈 #2 ";
            else if (rank == 3) rankPrefix = ChatColor.RED + "🥉 #3 ";
            else rankPrefix = ChatColor.WHITE + "#" + rank + " ";

            // Highlight viewer
            String nameColor = entry.uuid.equals(viewer.getUniqueId()) 
                ? ChatColor.YELLOW.toString() : ChatColor.WHITE.toString();

            meta.setDisplayName(rankPrefix + nameColor + entry.name);

            List<String> lore = new ArrayList<>();
            lore.add("");
            
            if ("earnings".equalsIgnoreCase(sortBy)) {
                lore.add(ChatColor.GOLD + "💰 Earnings: $" + String.format("%.2f", entry.earnings));
                lore.add(ChatColor.GRAY + "🌾 Harvests: " + entry.harvests);
            } else {
                lore.add(ChatColor.GRAY + "🌾 Harvests: " + entry.harvests);
                lore.add(ChatColor.GOLD + "💰 Earnings: $" + String.format("%.2f", entry.earnings));
            }

            if (entry.uuid.equals(viewer.getUniqueId())) {
                lore.add("");
                lore.add(ChatColor.YELLOW + "⭐ This is you!");
            }

            meta.setLore(lore);
            skull.setItemMeta(meta);
        }

        return skull;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (!playerGUIs.containsKey(player)) return;

        String title = InventoryUtil.getTitle(event.getView());
        if (!title.contains("Top Farmers")) return;

        event.setCancelled(true);

        Inventory gui = playerGUIs.get(player);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(gui)) return;

        int slot = event.getSlot();
        int currentPage = playerPages.getOrDefault(player, 1);

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

        if (slot == 45) { // Previous
            openGUI(player, currentPage - 1);
        } else if (slot == 53) { // Next
            openGUI(player, currentPage + 1);
        } else if (slot == 49) { // Sort toggle
            String currentSort = playerSortMode.getOrDefault(player, "earnings");
            String newSort = "earnings".equalsIgnoreCase(currentSort) ? "harvests" : "earnings";
            playerSortMode.put(player, newSort);
            openGUI(player, currentPage);
        } else if (slot == 48) { // Back
            player.closeInventory();
            cleanup(player);
            plugin.getMainMenuGUI().openGUI(player);
        }
    }

    private void cleanup(Player player) {
        playerGUIs.remove(player);
        playerPages.remove(player);
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
