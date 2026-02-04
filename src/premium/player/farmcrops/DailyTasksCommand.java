package player.farmcrops;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * PREMIUM EDITION - Daily Tasks Command
 * Opens the Daily Tasks GUI for players
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
        
        // Check if this is Premium edition
        if (!plugin.isPremiumEdition()) {
            player.sendMessage("§c§l╔═══════════════════════════════╗");
            player.sendMessage("§c§l║  §6§lPREMIUM FEATURE LOCKED§c§l  ║");
            player.sendMessage("§c§l╚═══════════════════════════════╝");
            player.sendMessage("");
            player.sendMessage("§7Daily Tasks are only available in");
            player.sendMessage("§7the §6Premium Edition§7 of FarmCrops!");
            player.sendMessage("");
            player.sendMessage("§6Premium Edition includes:");
            player.sendMessage("§e  ⭐ Daily Tasks System");
            player.sendMessage("§e  ⭐ Achievement System");
            player.sendMessage("§e  ⭐ Crop Collections");
            player.sendMessage("§e  ⭐ Title System");
            player.sendMessage("");
            return true;
        }
        
        // Check if Daily Tasks are enabled in config
        if (!plugin.getConfig().getBoolean("daily-tasks.enabled", false)) {
            player.sendMessage("§c§lDaily Tasks are currently disabled!");
            player.sendMessage("§7An administrator needs to enable them in the config.");
            return true;
        }
        
        // Check if DailyTaskGUI and DailyTaskManager are initialized
        if (plugin.getDailyTaskGUI() == null) {
            player.sendMessage("§c§lDaily Tasks system failed to load!");
            player.sendMessage("§7Please contact an administrator.");
            player.sendMessage("§7Error: DailyTaskGUI is not initialized");
            return true;
        }
        
        if (plugin.getDailyTaskManager() == null) {
            player.sendMessage("§c§lDaily Tasks system failed to load!");
            player.sendMessage("§7Please contact an administrator.");
            player.sendMessage("§7Error: DailyTaskManager is not initialized");
            return true;
        }
        
        // All checks passed - open the GUI!
        plugin.getDailyTaskGUI().openDailyTasksGUI(player);
        
        return true;
    }
}
