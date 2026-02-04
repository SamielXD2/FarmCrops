package player.farmcrops;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to open the Daily Tasks GUI (Premium feature)
 * Usage: /dailytasks or /tasks
 */
public class DailyTasksCommand implements CommandExecutor {
    
    private final FarmCrops plugin;
    
    public DailyTasksCommand(FarmCrops plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Player check
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Permission check
        if (!player.hasPermission("farmcrops.dailytasks")) {
            player.sendMessage(ChatColor.RED + "✗ You don't have permission to use this command!");
            return true;
        }
        
        // Premium check
        if (!plugin.isPremiumEdition()) {
            player.sendMessage("");
            player.sendMessage(ChatColor.RED + "✗ Daily Tasks is a Premium Feature!");
            player.sendMessage(ChatColor.GRAY + "You are using " + ChatColor.YELLOW + "FarmCrops Lite v0.9.0");
            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "⭐ Upgrade to Premium to unlock:");
            player.sendMessage(ChatColor.YELLOW + "  • Daily Tasks & Objectives");
            player.sendMessage(ChatColor.YELLOW + "  • Achievement System");
            player.sendMessage(ChatColor.YELLOW + "  • Crop Collections");
            player.sendMessage(ChatColor.YELLOW + "  • Title System");
            player.sendMessage("");
            player.sendMessage(ChatColor.GREEN + "Get Premium at: " + ChatColor.AQUA + "spigotmc.org/resources/farmcrops");
            player.sendMessage("");
            return true;
        }
        
        // Open GUI
        try {
            DailyTaskGUI gui = plugin.getDailyTaskGUI();
            if (gui == null) {
                player.sendMessage(ChatColor.RED + "✗ Daily Tasks GUI is not available!");
                plugin.getLogger().severe("DailyTaskGUI is null - this should not happen in Premium edition!");
                return true;
            }
            
            gui.openGUI(player);
            
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "✗ An error occurred while opening Daily Tasks!");
            plugin.getLogger().severe("Error opening Daily Tasks GUI: " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }
}
