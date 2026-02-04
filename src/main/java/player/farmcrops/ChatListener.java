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
     * Priority is HIGH to run after other plugins but before final formatting
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // Only run in Premium edition with title system
        if (!plugin.isPremiumEdition() || plugin.getTitleManager() == null) {
            return;
        }
        
        Player player = event.getPlayer();
        TitleManager titleManager = plugin.getTitleManager();
        
        // Check if player has a title equipped
        if (titleManager.hasTitle(player.getUniqueId())) {
            String title = titleManager.getEquippedTitle(player.getUniqueId());
            String coloredTitle = ChatColor.translateAlternateColorCodes('&', title);
            
            // Get current format (usually "%1$s: %2$s" or similar from EssentialsX)
            String currentFormat = event.getFormat();
            
            // Insert title before player name
            // If format contains %1$s (player name placeholder), add title there
            if (currentFormat.contains("%1$s")) {
                // Replace %1$s with [Title] %1$s
                String newFormat = currentFormat.replace("%1$s", coloredTitle + ChatColor.RESET + " %1$s");
                event.setFormat(newFormat);
            } else {
                // Fallback: just prepend title to the format
                event.setFormat(coloredTitle + ChatColor.RESET + " " + currentFormat);
            }
        }
    }
}
