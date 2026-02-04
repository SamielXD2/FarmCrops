package player.farmcrops;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Daily Tasks command - works for both Lite and Premium
 * Lite: Shows upgrade message
 * Premium: Opens Daily Tasks GUI
 */
public class DailyTasksCommand implements CommandExecutor {
    
    private final FarmCrops plugin;
    
    public DailyTasksCommand(FarmCrops plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check if premium features are available
        if (!plugin.hasPremiumFeatures()) {
            // Lite version - show upgrade message
            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "⭐ PREMIUM FEATURE ⭐");
            player.sendMessage(ChatColor.YELLOW + "Daily Tasks are only available in");
            player.sendMessage(ChatColor.YELLOW + "FarmCrops " + ChatColor.GOLD + "Premium Edition" + ChatColor.YELLOW + "!");
            player.sendMessage("");
            player.sendMessage(ChatColor.GRAY + "Premium Edition includes:");
            player.sendMessage(ChatColor.GREEN + "  ✓ Daily Task System");
            player.sendMessage(ChatColor.GREEN + "  ✓ Achievement System");
            player.sendMessage(ChatColor.GREEN + "  ✓ Crop Collections");
            player.sendMessage(ChatColor.GREEN + "  ✓ Custom Titles");
            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "Upgrade at: " + ChatColor.WHITE + "[Your store link here]");
            player.sendMessage("");
            return true;
        }
        
        // Premium version - check if DailyTaskGUI is available
        if (plugin.getDailyTaskGUI() != null) {
            plugin.getDailyTaskGUI().openDailyTasksGUI(player);
        } else {
            player.sendMessage(ChatColor.RED + "✗ Daily Tasks are disabled in the config!");
            player.sendMessage(ChatColor.GRAY + "Ask an admin to enable them.");
        }
        
        return true;
    }
}
