package player.farmcrops;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * LITE EDITION STUB
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
        
        sender.sendMessage("§c✗ Daily Tasks are a Premium feature!");
        sender.sendMessage("§eUpgrade to Premium to unlock daily tasks!");
        return true;
    }
}
