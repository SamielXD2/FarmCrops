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
 * v0.9.6 - Player Settings GUI (FIXED ALL BUGS)
 * 
 * Allows players to customize their own farming experience:
 * - Auto-sell crops on harvest (requires farmcrops.autosell.use permission)
 * - Show/hide holograms
 * - Show/hide particles
 * - Enable/disable sounds
 * - Show/hide harvest chat messages
 * - Achievement notifications (Premium only)
 * - Broadcast achievements (Premium only)
 * - Title display (Premium only)
 */
public class PlayerSettingsGUI implements Listener {
    
    private final FarmCrops plugin;
    private final Map<Player, Inventory> playerGUIs = new HashMap<>();
    
    public PlayerSettingsGUI(FarmCrops plugin) {
        this.plugin = plugin;
    }
    
    public void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, 
            ChatColor.AQUA + "⚙ My Settings");
        
        PlayerSettings.PlayerPreferences prefs = plugin.getPlayerSettings()
            .getPreferences(player.getUniqueId());
        
        boolean isPremium = plugin.hasPremiumFeatures();
        
        // Fill background
        ItemStack bgGlass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, bgGlass);
        }
        
        // Auto-sell toggle — locked if player lacks permission
        if (player.hasPermission("farmcrops.autosell.use")) {
            gui.setItem(10, createToggleItem(
                prefs.autoSell ? Material.EMERALD : Material.REDSTONE,
                "Auto-Sell Crops",
                prefs.autoSell,
                "Automatically sell crops when harvested",
                "No need to open sell GUI"
            ));
        } else {
            gui.setItem(10, createItem(Material.IRON_BLOCK,
                ChatColor.RED + "" + ChatColor.BOLD + "Auto-Sell Crops " + ChatColor.RED + "LOCKED",
                ChatColor.GRAY + "Automatically sell crops when harvested",
                "",
                ChatColor.RED + "✗ You don't have permission",
                ChatColor.GRAY + "Contact an admin to unlock"
            ));
        }
        
        // Holograms toggle
        gui.setItem(11, createToggleItem(
            prefs.showHolograms ? Material.ENDER_EYE : Material.ENDER_PEARL,
            "Holograms",
            prefs.showHolograms,
            "Show floating text above crops",
            "Harvest info and growing status"
        ));
        
        // Particles toggle
        gui.setItem(12, createToggleItem(
            prefs.showParticles ? Material.BLAZE_POWDER : Material.GUNPOWDER,
            "Particle Effects",
            prefs.showParticles,
            "Show particles when harvesting",
            "Visual effects for different tiers"
        ));
        
        // Sounds toggle
        gui.setItem(13, createToggleItem(
            prefs.playSounds ? Material.NOTE_BLOCK : Material.BARRIER,
            "Sounds",
            prefs.playSounds,
            "Play sounds when harvesting",
            "Audio feedback for actions"
        ));
        
        // Harvest messages toggle
        gui.setItem(14, createToggleItem(
            prefs.showHarvestMessages ? Material.BOOK : Material.WRITABLE_BOOK,
            "Harvest Messages",
            prefs.showHarvestMessages,
            "Show harvest info in chat",
            "Displays tier, weight, and value"
        ));
        
        // Achievement notifications toggle (Premium only or locked)
        if (isPremium) {
            gui.setItem(15, createToggleItem(
                prefs.achievementNotifications ? Material.BELL : Material.BELL,
                "Achievement Notifications",
                prefs.achievementNotifications,
                "Get notified for new achievements",
                "Sound and message alerts"
            ));
        } else {
            gui.setItem(15, createLockedItem(
                Material.IRON_INGOT,
                "Achievement Notifications",
                "Get notified for new achievements",
                "Premium Edition feature"
            ));
        }
        
        // Broadcast toggle (Premium only or locked)
        if (isPremium) {
            gui.setItem(16, createToggleItem(
                prefs.broadcastAchievements ? Material.BEACON : Material.GLASS,
                "Broadcast Achievements",
                prefs.broadcastAchievements,
                "Announce your achievements to server",
                "Let everyone know your progress!"
            ));
        } else {
            gui.setItem(16, createLockedItem(
                Material.IRON_INGOT,
                "Broadcast Achievements",
                "Announce achievements to server",
                "Premium Edition feature"
            ));
        }
        
        // Action bar toggle
        gui.setItem(28, createToggleItem(
            prefs.showActionBar ? Material.EXPERIENCE_BOTTLE : Material.GLASS_BOTTLE,
            "Action Bar Info",
            prefs.showActionBar,
            "Show harvest info above hotbar",
            "Alternative to chat messages"
        ));
        
        // Scoreboard toggle
        gui.setItem(29, createToggleItem(
            prefs.showScoreboard ? Material.PAINTING : Material.ITEM_FRAME,
            "Scoreboard Display",
            prefs.showScoreboard,
            "Show farm stats on scoreboard",
            "Live stats on the side"
        ));
        
        // Title display toggle (Premium only or locked)
        if (isPremium) {
            gui.setItem(30, createToggleItem(
                prefs.showTitle ? Material.NAME_TAG : Material.PAPER,
                "Title Display",
                prefs.showTitle,
                "Show your equipped title",
                "Display title to other players"
            ));
        } else {
            gui.setItem(30, createLockedItem(
                Material.IRON_INGOT,
                "Title Display",
                "Show your equipped title",
                "Premium Edition feature"
            ));
        }
        
        // Edition info
        gui.setItem(49, createItem(isPremium ? Material.NETHER_STAR : Material.PAPER,
            isPremium ? 
                ChatColor.GOLD + "" + ChatColor.BOLD + "⭐ Premium Edition" :
                ChatColor.YELLOW + "" + ChatColor.BOLD + "💎 Lite Edition",
            isPremium ?
                ChatColor.GRAY + "You have access to all features!" :
                ChatColor.GRAY + "Upgrade for more features:",
            isPremium ?
                "" :
                ChatColor.GOLD + "  ⭐ Achievements",
            isPremium ?
                ChatColor.GREEN + "All settings save automatically" :
                ChatColor.GOLD + "  ⭐ Daily Tasks",
            isPremium ?
                "" :
                ChatColor.GOLD + "  ⭐ Collections & Titles"
        ));
        
        // Back button (FIXED slot from 22 to 53)
        gui.setItem(53, createItem(Material.ARROW,
            ChatColor.RED + "← Back to Menu",
            ChatColor.GRAY + "Return to main menu"
        ));
        
        playerGUIs.put(player, gui);
        player.openInventory(gui);
    }
    
    private ItemStack createToggleItem(Material mat, String name, boolean enabled,
                                       String description, String extra) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName((enabled ? ChatColor.GREEN : ChatColor.RED) + "" + 
                ChatColor.BOLD + name + " " +
                (enabled ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + description);
            lore.add(ChatColor.GRAY + extra);
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to toggle");
            lore.add("");
            lore.add(enabled ? 
                ChatColor.GREEN + "✓ Currently Enabled" : 
                ChatColor.RED + "✗ Currently Disabled");
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createLockedItem(Material mat, String name, String description, String reason) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + name + " " + ChatColor.DARK_GRAY + "LOCKED");
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + description);
            lore.add("");
            lore.add(ChatColor.GOLD + "⭐ " + reason);
            lore.add(ChatColor.YELLOW + "Upgrade to unlock!");
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        if (!playerGUIs.containsKey(player)) return;
        
        String title = InventoryUtil.getTitle(event.getView());
        if (!title.contains("My Settings")) return;
        
        event.setCancelled(true);
        
        Inventory gui = playerGUIs.get(player);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(gui)) return;
        
        int slot = event.getSlot();
        
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        
        PlayerSettings settings = plugin.getPlayerSettings();
        boolean isPremium = plugin.hasPremiumFeatures();
        String message = "";
        
        switch (slot) {
            case 10: // Auto-sell
                if (!player.hasPermission("farmcrops.autosell.use")) {
                    player.sendMessage(ChatColor.RED + "✗ You don't have permission to use Auto-Sell.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                settings.toggleAutoSell(player);
                message = "Auto-Sell: " + (settings.getPreferences(player.getUniqueId()).autoSell ?
                    ChatColor.GREEN + "ON" : ChatColor.RED + "OFF");
                break;
                
            case 11: // Holograms
                settings.toggleHolograms(player);
                message = "Holograms: " + (settings.getPreferences(player.getUniqueId()).showHolograms ?
                    ChatColor.GREEN + "ON" : ChatColor.RED + "OFF");
                break;
                
            case 12: // Particles
                settings.toggleParticles(player);
                message = "Particles: " + (settings.getPreferences(player.getUniqueId()).showParticles ?
                    ChatColor.GREEN + "ON" : ChatColor.RED + "OFF");
                break;
                
            case 13: // Sounds
                settings.toggleSounds(player);
                message = "Sounds: " + (settings.getPreferences(player.getUniqueId()).playSounds ?
                    ChatColor.GREEN + "ON" : ChatColor.RED + "OFF");
                break;
                
            case 14: // Harvest messages
                settings.toggleHarvestMessages(player);
                message = "Harvest Messages: " + (settings.getPreferences(player.getUniqueId()).showHarvestMessages ?
                    ChatColor.GREEN + "ON" : ChatColor.RED + "OFF");
                break;
                
            case 15: // Achievement notifications (Premium only)
                if (!isPremium) {
                    showPremiumMessage(player);
                    return;
                }
                settings.toggleAchievementNotifications(player);
                message = "Achievement Notifications: " + (settings.getPreferences(player.getUniqueId()).achievementNotifications ?
                    ChatColor.GREEN + "ON" : ChatColor.RED + "OFF");
                break;
                
            case 16: // Broadcast achievements (Premium only)
                if (!isPremium) {
                    showPremiumMessage(player);
                    return;
                }
                settings.toggleBroadcastAchievements(player);
                message = "Broadcast Achievements: " + (settings.getPreferences(player.getUniqueId()).broadcastAchievements ?
                    ChatColor.GREEN + "ON" : ChatColor.RED + "OFF");
                break;
                
            case 28: // Action bar
                settings.toggleActionBar(player);
                message = "Action Bar: " + (settings.getPreferences(player.getUniqueId()).showActionBar ?
                    ChatColor.GREEN + "ON" : ChatColor.RED + "OFF");
                break;
                
            case 29: // Scoreboard
                settings.toggleScoreboard(player);
                message = "Scoreboard: " + (settings.getPreferences(player.getUniqueId()).showScoreboard ?
                    ChatColor.GREEN + "ON" : ChatColor.RED + "OFF");
                break;
                
            case 30: // Title display (Premium only)
                if (!isPremium) {
                    showPremiumMessage(player);
                    return;
                }
                settings.toggleTitleDisplay(player);
                message = "Title Display: " + (settings.getPreferences(player.getUniqueId()).showTitle ?
                    ChatColor.GREEN + "ON" : ChatColor.RED + "OFF");
                break;
                
            case 53: // Back (FIXED from slot 22)
                player.closeInventory();
                playerGUIs.remove(player);
                plugin.getMainMenuGUI().openGUI(player);
                return;
                
            case 49: // Info - do nothing
                return;
                
            default:
                return; // Unknown slot
        }
        
        if (!message.isEmpty()) {
            player.sendMessage(ChatColor.GREEN + "✓ " + message);
        }
        
        // Refresh GUI
        player.closeInventory();
        playerGUIs.remove(player);
        openGUI(player);
    }
    
    private void showPremiumMessage(Player player) {
        player.closeInventory();
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "⭐ PREMIUM FEATURE ⭐");
        player.sendMessage(ChatColor.YELLOW + "This setting is only available in");
        player.sendMessage(ChatColor.YELLOW + "FarmCrops " + ChatColor.GOLD + "Premium Edition" + ChatColor.YELLOW + "!");
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "Upgrade at: " + ChatColor.WHITE + "[Your store link here]");
        player.sendMessage("");
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
