package player.farmcrops;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * PREMIUM EDITION
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
        
        // Check if DailyTaskGUI is available
        if (plugin.getDailyTaskGUI() != null) {
            plugin.getDailyTaskGUI().openDailyTasksGUI(player);
        } else {
            player.sendMessage("§cDaily Tasks system is not enabled!");
        }
        
        return true;
    }
}
