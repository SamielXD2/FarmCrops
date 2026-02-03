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
        
        // Check if premium features are available
        if (!plugin.hasPremiumFeatures()) {
            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "⭐ PREMIUM FEATURE ⭐");
            player.sendMessage(ChatColor.YELLOW + "Achievements are only available in");
            player.sendMessage(ChatColor.YELLOW + "FarmCrops " + ChatColor.GOLD + "Premium Edition" + ChatColor.YELLOW + "!");
            player.sendMessage("");
            player.sendMessage(ChatColor.GRAY + "Premium Edition includes:");
            player.sendMessage(ChatColor.GREEN + "  ✓ Achievement System");
            player.sendMessage(ChatColor.GREEN + "  ✓ Daily Tasks & Challenges");
            player.sendMessage(ChatColor.GREEN + "  ✓ Crop Collections");
            player.sendMessage(ChatColor.GREEN + "  ✓ Custom Titles");
            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "Upgrade at: " + ChatColor.WHITE + "[Your store link here]");
            player.sendMessage("");
            return true;
        }
        
        // Check if achievements are disabled in config
        if (plugin.getAchievementGUI() == null) {
            player.sendMessage(ChatColor.RED + "✗ Achievements are disabled in the config!");
            player.sendMessage(ChatColor.GRAY + "Ask an admin to enable them.");
            return true;
        }
        
        plugin.getAchievementGUI().openGUI(player, 1);
        return true;
    }
}
