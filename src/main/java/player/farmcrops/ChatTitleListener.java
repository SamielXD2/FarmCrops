package player.farmcrops;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Chat Title System - Shows player titles in chat!
 * 
 * Example chat format:
 * [Master Farmer] PlayerName: Hello everyone!
 * 
 * This is how chat title plugins work - they intercept chat events
 * and modify the display format to include the title prefix.
 */
public class ChatTitleListener implements Listener {
    
    private final FarmCrops plugin;
    
    // Store player titles (titleId -> formatted title text)
    private final Map<UUID, String> playerTitles = new HashMap<>();
    
    public ChatTitleListener(FarmCrops plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Main chat event - formats chat with titles
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Get player's title
        String title = playerTitles.get(uuid);
        
        // Format the chat message
        String formattedMessage;
        
        if (title != null && !title.isEmpty()) {
            // With title: [Title] PlayerName: message
            formattedMessage = ChatColor.translateAlternateColorCodes('&', title) 
                             + " " + ChatColor.WHITE + player.getName() 
                             + ChatColor.GRAY + ": " + ChatColor.WHITE + event.getMessage();
        } else {
            // No title: PlayerName: message
            formattedMessage = ChatColor.WHITE + player.getName() 
                             + ChatColor.GRAY + ": " + ChatColor.WHITE + event.getMessage();
        }
        
        // Set the custom format
        event.setFormat(formattedMessage);
        
        // OR use setMessage if you want more control:
        // event.setCancelled(true);
        // Bukkit.broadcastMessage(formattedMessage);
    }
    
    /**
     * Set a player's chat title
     */
    public void setTitle(UUID uuid, String formattedTitle) {
        if (formattedTitle == null || formattedTitle.trim().isEmpty()) {
            playerTitles.remove(uuid);
        } else {
            playerTitles.put(uuid, formattedTitle);
        }
    }
    
    /**
     * Remove a player's title
     */
    public void removeTitle(UUID uuid) {
        playerTitles.remove(uuid);
    }
    
    /**
     * Get a player's current title
     */
    public String getTitle(UUID uuid) {
        return playerTitles.getOrDefault(uuid, "");
    }
    
    /**
     * Check if player has a title
     */
    public boolean hasTitle(UUID uuid) {
        return playerTitles.containsKey(uuid) && !playerTitles.get(uuid).isEmpty();
    }
    
    /**
     * Clear all titles (for reload)
     */
    public void clearAll() {
        playerTitles.clear();
    }
    
    /**
     * Get all titles (for saving)
     */
    public Map<UUID, String> getAllTitles() {
        return new HashMap<>(playerTitles);
    }
    
    /**
     * Load titles (from file)
     */
    public void loadTitles(Map<UUID, String> titles) {
        playerTitles.clear();
        playerTitles.putAll(titles);
    }
}
