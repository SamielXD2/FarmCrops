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

        sender.sendMessage(ChatColor.YELLOW + "Reloading FarmCrops config...");

        try {
            plugin.reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "✓ Config reloaded successfully!");
            plugin.getLogger().info("Config reloaded by " + sender.getName());
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "✗ Failed to reload config: " + e.getMessage());
            plugin.getLogger().severe("Config reload failed: " + e.getMessage());
        }

        return true;
    }
}
