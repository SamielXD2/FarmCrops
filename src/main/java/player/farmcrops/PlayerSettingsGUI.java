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
 * v0.9.5 - Player Settings GUI (Fixed getTitle() for Paper 1.21)
 * 
 * Allows players to customize their own farming experience:
 * - Auto-sell crops on harvest (requires farmcrops.autosell.use permission)
 * - Show/hide holograms
 * - Show/hide particles
 * - Enable/disable sounds
 * - Show/hide harvest chat messages
 */
public class PlayerSettingsGUI implements Listener {
    
    private final FarmCrops plugin;
    private final Map<Player, Inventory> playerGUIs = new HashMap<>();
    
    public PlayerSettingsGUI(FarmCrops plugin) {
        this.plugin = plugin;
    }
    
    public void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, 
            ChatColor.AQUA + "⚙ My Settings");
        
        PlayerSettings.PlayerPreferences prefs = plugin.getPlayerSettings()
            .getPreferences(player.getUniqueId());
        
        // Fill background
        ItemStack bgGlass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, bgGlass);
        }
        
        // Auto-sell toggle
        gui.setItem(10, createToggleItem(
            prefs.autoSell ? Material.EMERALD : Material.REDSTONE,
            "Auto-Sell Crops",
            prefs.autoSell,
            "Automatically sell crops when harvested",
            "No need to open sell GUI"
        ));
        
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
        
        // Info button
        gui.setItem(16, createItem(Material.PAPER,
            ChatColor.YELLOW + "" + ChatColor.BOLD + "ℹ Info",
            ChatColor.GRAY + "These settings are personal",
            ChatColor.GRAY + "They only affect YOU",
            "",
            ChatColor.GREEN + "All changes save automatically"
        ));
        
        // Back button
        gui.setItem(22, createItem(Material.ARROW,
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
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        if (!playerGUIs.containsKey(player)) return;
        
        if (!event.getView().getTitle().contains(ChatColor.AQUA + "⚙ My Settings")) return;
        
        event.setCancelled(true);
        
        Inventory gui = playerGUIs.get(player);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(gui)) return;
        
        int slot = event.getSlot();
        
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        
        PlayerSettings settings = plugin.getPlayerSettings();
        String message = "";
        
        switch (slot) {
            case 10: // Auto-sell
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
                
            case 22: // Back
                player.closeInventory();
                playerGUIs.remove(player);
                plugin.getMainMenuGUI().openGUI(player);
                return;
                
            case 16: // Info - do nothing
                return;
        }
        
        if (!message.isEmpty()) {
            player.sendMessage(ChatColor.GREEN + "✓ " + message);
        }
        
        // Refresh GUI
        player.closeInventory();
        playerGUIs.remove(player);
        openGUI(player);
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
