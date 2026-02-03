package player.farmcrops.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import player.farmcrops.PluginConfig;

/**
 * Example command that is only available in Premium edition
 */
public class AchievementsCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check if achievements are enabled (Premium only)
        if (!PluginConfig.ACHIEVEMENTS_ENABLED) {
            player.sendMessage(PluginConfig.getLockedMessage("Achievements"));
            player.sendMessage("§7This feature is available in FarmCrops Premium v1.0.0+");
            return true;
        }
        
        // Open achievements GUI (Premium only)
        openAchievementsGUI(player);
        return true;
    }
    
    private void openAchievementsGUI(Player player) {
        // Your achievements GUI code here
        player.sendMessage("§aOpening Achievements...");
    }
}
