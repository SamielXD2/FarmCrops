package player.farmcrops;

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
 * v0.8.0 Main Menu GUI - Hub for all FarmCrops features
 * 
 * One command (/farm) to access everything:
 * - Sell Crops
 * - My Stats
 * - Leaderboard
 * - Settings (admin only)
 */
public class MainMenuGUI implements Listener {

    private final FarmCrops plugin;
    private final Map<Player, Inventory> playerGUIs = new HashMap<>();

    public MainMenuGUI(FarmCrops plugin) {
        this.plugin = plugin;
    }

    public void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.GREEN + "🌾 FarmCrops Menu");

        // Fill background
        ItemStack bgGlass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, bgGlass);
        }

        // Sell Crops button
        gui.setItem(11, createItem(Material.EMERALD_BLOCK,
            ChatColor.GREEN + "" + ChatColor.BOLD + "💰 Sell Crops",
            ChatColor.GRAY + "Open the sell GUI",
            ChatColor.GRAY + "Convert your harvests to money!",
            "",
            ChatColor.YELLOW + "Click to open"));

        // My Stats button
        gui.setItem(13, createItem(Material.BOOK,
            ChatColor.AQUA + "" + ChatColor.BOLD + "📊 My Stats",
            ChatColor.GRAY + "View your farming statistics",
            ChatColor.GRAY + "Harvests, earnings, records...",
            "",
            ChatColor.YELLOW + "Click to view"));

        // Leaderboard button
        gui.setItem(15, createItem(Material.GOLD_INGOT,
            ChatColor.GOLD + "" + ChatColor.BOLD + "🏆 Leaderboard",
            ChatColor.GRAY + "Top farmers on the server",
            ChatColor.GRAY + "See who's farming the most!",
            "",
            ChatColor.YELLOW + "Click to view"));

        // Settings button (admin only)
        if (player.hasPermission("farmcrops.settings")) {
            gui.setItem(22, createItem(Material.REDSTONE,
                ChatColor.RED + "" + ChatColor.BOLD + "⚙ Settings",
                ChatColor.GRAY + "Admin configuration panel",
                ChatColor.GRAY + "Toggle features on/off",
                "",
                ChatColor.YELLOW + "Click to open",
                ChatColor.DARK_RED + "Admin Only"));
        }

        // Close button
        gui.setItem(18, createItem(Material.BARRIER,
            ChatColor.RED + "" + ChatColor.BOLD + "✗ Close",
            ChatColor.GRAY + "Exit menu"));

        playerGUIs.put(player, gui);
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (!playerGUIs.containsKey(player)) return;

        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
            .serialize(event.getView().title());
        if (!title.contains("FarmCrops Menu")) return;

        event.setCancelled(true);

        Inventory gui = playerGUIs.get(player);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(gui)) return;

        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

        switch (slot) {
            case 11: // Sell Crops
                player.closeInventory();
                playerGUIs.remove(player);
                plugin.getSellGUI().openGUI(player);
                break;

            case 13: // My Stats
                player.closeInventory();
                playerGUIs.remove(player);
                plugin.getStatsGUI().openGUI(player);
                break;

            case 15: // Leaderboard
                player.closeInventory();
                playerGUIs.remove(player);
                plugin.getTopGUI().openGUI(player, 1);
                break;

            case 22: // Settings (admin only)
                if (player.hasPermission("farmcrops.settings")) {
                    player.closeInventory();
                    playerGUIs.remove(player);
                    plugin.getSettingsGUI().openGUI(player);
                }
                break;

            case 18: // Close
                player.closeInventory();
                playerGUIs.remove(player);
                break;
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
}
