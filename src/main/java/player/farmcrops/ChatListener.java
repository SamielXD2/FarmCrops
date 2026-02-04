package player.farmcrops;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Handles chat integration for the Title system
 * Adds player titles before their name in chat
 */
public class ChatListener implements Listener {
    
    private final FarmCrops plugin;
    
    public ChatListener(FarmCrops plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Add equipped title to chat messages
     * Priority is LOWEST to run BEFORE other chat plugins like EssentialsX
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // Only run in Premium edition with title system
        if (!plugin.isPremiumEdition() || plugin.getTitleManager() == null) {
            return;
        }
        
        Player player = event.getPlayer();
        TitleManager titleManager = plugin.getTitleManager();
        
        // Check if player has a title equipped and wants to show it
        PlayerSettings.PlayerPreferences prefs = plugin.getPlayerSettings().getPreferences(player.getUniqueId());
        if (!prefs.showTitle) {
            return; // Player has titles disabled
        }
        
        // Check if player has a title equipped
        if (titleManager.hasTitle(player.getUniqueId())) {
            String title = titleManager.getEquippedTitle(player.getUniqueId());
            if (title == null || title.isEmpty()) {
                return;
            }
            
            String coloredTitle = ChatColor.translateAlternateColorCodes('&', title);
            
            // Modify the player's display name to include title
            String originalDisplayName = player.getDisplayName();
            player.setDisplayName(coloredTitle + " " + ChatColor.RESET + originalDisplayName);
        }
    }
}
