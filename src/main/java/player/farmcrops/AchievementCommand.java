package player.farmcrops;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

public class AchievementCommand implements CommandExecutor {

    private final FarmCrops plugin;

    public AchievementCommand(FarmCrops plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        
        if (plugin.getAchievementGUI() == null) {
            player.sendMessage(ChatColor.RED + "Achievements are not enabled!");
            return true;
        }
        
        plugin.getAchievementGUI().openGUI(player);
        return true;
    }
}
