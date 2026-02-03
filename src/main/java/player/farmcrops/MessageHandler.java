package player.farmcrops;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Centralized message handler for consistent messaging
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
}
