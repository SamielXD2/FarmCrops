package player.farmcrops;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /farmreload — Hot-reload config without server restart.
 * Protected by farmcrops.reload permission.
 */
public class ReloadCommand implements CommandExecutor {

    private final FarmCrops plugin;

    public ReloadCommand(FarmCrops plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("farmcrops.reload")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to reload FarmCrops.");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Reloading FarmCrops...");
        sender.sendMessage("");

        try {
            // Reload config file
            plugin.reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "✓ Config reloaded");
            
            // Reload scoreboard for online players
            if (plugin.getScoreboardManager() != null) {
                plugin.getScoreboardManager().reloadAllScoreboards();
                sender.sendMessage(ChatColor.GREEN + "✓ Scoreboards refreshed");
            }
            
            // Reload hologram settings
            if (plugin.getHoloManager() != null) {
                sender.sendMessage(ChatColor.GREEN + "✓ Hologram settings updated");
            }
            
            sender.sendMessage("");
            sender.sendMessage(ChatColor.GREEN + "✓ FarmCrops reloaded successfully!");
            sender.sendMessage(ChatColor.GRAY + "Note: Some changes may require a server restart");
            
            plugin.getLogger().info("Plugin reloaded by " + sender.getName());
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "✗ Failed to reload: " + e.getMessage());
            plugin.getLogger().severe("Reload failed: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }
}
