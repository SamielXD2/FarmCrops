package player.farmcrops;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /farm command - Opens the main menu hub
 */
public class FarmCommand implements CommandExecutor {

    private final FarmCrops plugin;

    public FarmCommand(FarmCrops plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;
        plugin.getMainMenuGUI().openGUI(player);
        return true;
    }
}
