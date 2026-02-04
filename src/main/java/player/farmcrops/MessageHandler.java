package player.farmcrops;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Enhanced Message Handler v2.0
 * 
 * Features:
 * - Comprehensive error handling
 * - Premium feature detection
 * - Player-friendly error messages
 * - Debug logging support
 * - Exception handling utilities
 */
public class MessageHandler {
    
    private final FarmCrops plugin;
    private final String prefix;
    
    public MessageHandler(FarmCrops plugin) {
        this.plugin = plugin;
        this.prefix = plugin.getConfig().getString("messages.prefix", "§a§lFarmCrops §8» ");
    }
    
    // ══════════════════════════════════════════════════════════
    // Premium Feature Messages
    // ══════════════════════════════════════════════════════════
    
    public void sendPremiumOnly(CommandSender sender, String feature) {
        sender.sendMessage("");
        sender.sendMessage(ChatColor.RED + "✗ " + feature + " is a Premium Feature!");
        sender.sendMessage(ChatColor.GRAY + "You are using " + ChatColor.YELLOW + "FarmCrops Lite v" + 
            plugin.getConfig().getString("edition.version", "0.9.0"));
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "⭐ Upgrade to Premium to unlock:");
        sender.sendMessage(ChatColor.YELLOW + "  • Achievement System (30+ achievements)");
        sender.sendMessage(ChatColor.YELLOW + "  • Daily Tasks & Objectives");
        sender.sendMessage(ChatColor.YELLOW + "  • Crop Collections Tracker");
        sender.sendMessage(ChatColor.YELLOW + "  • Title System (earn & equip titles)");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GREEN + "Get Premium at: " + ChatColor.AQUA + "spigotmc.org/resources/farmcrops");
        sender.sendMessage("");
    }
    
    public void sendPremiumOnlyShort(CommandSender sender, String feature) {
        sender.sendMessage(colorize(prefix + "&c✗ " + feature + " is a &6&lPremium Feature&c!"));
        sender.sendMessage(colorize("&7Upgrade to unlock achievements, tasks, titles & more!"));
    }
    
    // ══════════════════════════════════════════════════════════
    // Error Messages
    // ══════════════════════════════════════════════════════════
    
    public void sendError(CommandSender sender, String message) {
        sender.sendMessage(colorize(prefix + "&c✗ " + message));
    }
    
    public void sendNoPermission(CommandSender sender) {
        sender.sendMessage(colorize(prefix + "&cYou don't have permission to do that!"));
    }
    
    public void sendPlayerOnly(CommandSender sender) {
        sender.sendMessage(colorize(prefix + "&cThis command can only be used by players!"));
    }
    
    public void sendPlayerNotFound(CommandSender sender, String playerName) {
        sender.sendMessage(colorize(prefix + "&cPlayer not found: &f" + playerName));
    }
    
    public void sendInvalidUsage(CommandSender sender, String usage) {
        sender.sendMessage(colorize(prefix + "&cInvalid usage! &7" + usage));
    }
    
    // ══════════════════════════════════════════════════════════
    // Success Messages
    // ══════════════════════════════════════════════════════════
    
    public void sendSuccess(CommandSender sender, String message) {
        sender.sendMessage(colorize(prefix + "&a✓ " + message));
    }
    
    public void sendInfo(CommandSender sender, String message) {
        sender.sendMessage(colorize(prefix + "&e" + message));
    }
    
    // ══════════════════════════════════════════════════════════
    // Feature Lock Detection
    // ══════════════════════════════════════════════════════════
    
    public boolean isPremiumEdition() {
        String edition = plugin.getConfig().getString("edition.type", "Lite");
        return "Premium".equalsIgnoreCase(edition);
    }
    
    public boolean isLiteEdition() {
        return !isPremiumEdition();
    }
    
    public boolean checkPremiumFeature(CommandSender sender, String featureName) {
        if (isLiteEdition()) {
            sendPremiumOnly(sender, featureName);
            return false;
        }
        return true;
    }
    
    // ══════════════════════════════════════════════════════════
    // Utility
    // ══════════════════════════════════════════════════════════
    
    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    public void sendEditionInfo(CommandSender sender) {
        String edition = plugin.getConfig().getString("edition.type", "Unknown");
        String version = plugin.getConfig().getString("edition.version", "Unknown");
        
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GREEN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "    🌾 FarmCrops " + edition + " Edition");
        sender.sendMessage(ChatColor.GRAY + "         Version: " + ChatColor.WHITE + version);
        sender.sendMessage("");
        
        if ("Lite".equalsIgnoreCase(edition)) {
            sender.sendMessage(ChatColor.YELLOW + "  You're using the FREE Lite version!");
            sender.sendMessage(ChatColor.GRAY + "  Upgrade to Premium for:");
            sender.sendMessage(ChatColor.YELLOW + "   ⭐ Achievements & Titles");
            sender.sendMessage(ChatColor.YELLOW + "   ⭐ Daily Tasks");
            sender.sendMessage(ChatColor.YELLOW + "   ⭐ Collections Tracker");
        } else {
            sender.sendMessage(ChatColor.GOLD + "  ⭐ Premium Edition - All features unlocked!");
        }
        
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GREEN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("");
    }
    
    // ══════════════════════════════════════════════════════════
    // Exception & Error Handling
    // ══════════════════════════════════════════════════════════
    
    /**
     * Handle and log an exception with user-friendly message
     */
    public void handleException(CommandSender sender, String action, Exception e) {
        // Send user-friendly message
        sender.sendMessage(colorize(prefix + "&c✗ An error occurred while " + action + "!"));
        sender.sendMessage(colorize("&7Please contact an administrator if this persists."));
        
        // Log detailed error
        plugin.getLogger().severe("═══════════════════════════════════════");
        plugin.getLogger().severe("ERROR: Failed to " + action);
        plugin.getLogger().severe("Player: " + (sender instanceof Player ? sender.getName() : "Console"));
        plugin.getLogger().severe("Error Type: " + e.getClass().getSimpleName());
        plugin.getLogger().severe("Message: " + e.getMessage());
        plugin.getLogger().severe("═══════════════════════════════════════");
        e.printStackTrace();
    }
    
    /**
     * Send data corruption error
     */
    public void sendDataError(CommandSender sender, String dataType) {
        sender.sendMessage(colorize(prefix + "&c✗ Failed to load " + dataType + " data!"));
        sender.sendMessage(colorize("&7Your data may be corrupted. Contact an administrator."));
        
        plugin.getLogger().warning("Data corruption detected for " + dataType);
    }
    
    /**
     * Send save error message
     */
    public void sendSaveError(CommandSender sender) {
        sender.sendMessage(colorize(prefix + "&c✗ Failed to save data!"));
        sender.sendMessage(colorize("&7Changes may not be saved. Please try again."));
    }
    
    /**
     * Send economy error
     */
    public void sendEconomyError(CommandSender sender) {
        sender.sendMessage(colorize(prefix + "&c✗ Economy system error!"));
        sender.sendMessage(colorize("&7Could not process transaction. Contact an administrator."));
        
        plugin.getLogger().warning("Economy system error - check Vault integration!");
    }
    
    /**
     * Send permission denied with specific permission
     */
    public void sendPermissionDenied(CommandSender sender, String permission) {
        sender.sendMessage(colorize(prefix + "&c✗ Permission denied!"));
        sender.sendMessage(colorize("&7Required: &f" + permission));
    }
    
    /**
     * Send feature disabled message
     */
    public void sendFeatureDisabled(CommandSender sender, String feature) {
        sender.sendMessage(colorize(prefix + "&c✗ " + feature + " is currently disabled!"));
        sender.sendMessage(colorize("&7Contact an administrator to enable it."));
    }
    
    /**
     * Send configuration error
     */
    public void sendConfigError(CommandSender sender, String setting) {
        sender.sendMessage(colorize(prefix + "&c✗ Configuration error!"));
        sender.sendMessage(colorize("&7Invalid setting: &f" + setting));
        sender.sendMessage(colorize("&7Contact an administrator to fix the config."));
    }
    
    /**
     * Send cooldown message
     */
    public void sendCooldown(CommandSender sender, long secondsRemaining) {
        sender.sendMessage(colorize(prefix + "&c✗ Please wait &f" + secondsRemaining + " seconds &cbefore doing that again!"));
    }
    
    /**
     * Send inventory full error
     */
    public void sendInventoryFull(Player player) {
        player.sendMessage(colorize(prefix + "&c✗ Your inventory is full!"));
        player.sendMessage(colorize("&7Free up some space and try again."));
    }
    
    /**
     * Send debug message (only if debug mode enabled)
     */
    public void sendDebug(String message) {
        if (plugin.getConfig().getBoolean("debug-mode", false)) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }
    
    /**
     * Send detailed error to console
     */
    public void logError(String context, Exception e) {
        plugin.getLogger().severe("═══════════════════════════════════════");
        plugin.getLogger().severe("ERROR in " + context);
        plugin.getLogger().severe("Type: " + e.getClass().getName());
        plugin.getLogger().severe("Message: " + e.getMessage());
        plugin.getLogger().severe("Stack trace:");
        e.printStackTrace();
        plugin.getLogger().severe("═══════════════════════════════════════");
    }
    
    /**
     * Send warning message
     */
    public void sendWarning(CommandSender sender, String message) {
        sender.sendMessage(colorize(prefix + "&e⚠ " + message));
    }
    
    /**
     * Send maintenance mode message
     */
    public void sendMaintenanceMode(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(colorize("&c&l⚠ MAINTENANCE MODE ⚠"));
        sender.sendMessage(colorize("&7FarmCrops is currently undergoing maintenance."));
        sender.sendMessage(colorize("&7Please try again later."));
        sender.sendMessage("");
    }
    
    /**
     * Send tips/help message
     */
    public void sendTip(Player player, String tip) {
        player.sendMessage(colorize(prefix + "&e💡 Tip: &7" + tip));
    }
    
    /**
     * Send generic message with prefix
     */
    public void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(colorize(prefix + message));
    }
    
    /**
     * Broadcast message to all players
     */
    public void broadcast(String message) {
        plugin.getServer().broadcastMessage(colorize(prefix + message));
    }
}
