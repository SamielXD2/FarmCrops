package player.farmcrops;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * /farmstats [player] — View farming statistics.
 * Without an argument: shows your own stats.
 * With a player name: shows that player's stats (requires farmcrops.stats.others).
 */
public class StatsCommand implements CommandExecutor {

    private final FarmCrops plugin;

    public StatsCommand(FarmCrops plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        // If an argument is provided, look up another player's stats
        if (args.length > 0) {
            if (!player.hasPermission("farmcrops.stats.others")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to view other players' stats.");
                return true;
            }

            Player target = plugin.getServer().getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Player '" + args[0] + "' is not online.");
                return true;
            }

            displayStats(player, target.getName(), plugin.getStatsManager().getStats(target.getUniqueId()));
            return true;
        }

        // No argument — show own stats
        displayStats(player, player.getName(), plugin.getStatsManager().getStats(player.getUniqueId()));
        return true;
    }

    private void displayStats(Player viewer, String targetName, StatsManager.PlayerStats stats) {
        String bar = ChatColor.DARK_GREEN + "══════════════════════════════";

        viewer.sendMessage("");
        viewer.sendMessage(bar);
        viewer.sendMessage(ChatColor.GREEN + "  🌾 FarmCrops Stats — " + ChatColor.WHITE + targetName);
        viewer.sendMessage(bar);
        viewer.sendMessage("");

        // Overview
        viewer.sendMessage(ChatColor.YELLOW + "  Overview");
        viewer.sendMessage(ChatColor.GRAY + "  Total Harvests: " + ChatColor.WHITE + stats.totalHarvests);
        viewer.sendMessage(ChatColor.GRAY + "  Total Earnings: " + ChatColor.GOLD + "$" + String.format("%.2f", stats.totalEarnings));
        viewer.sendMessage("");

        // Tier breakdown
        viewer.sendMessage(ChatColor.YELLOW + "  Harvests by Tier");
        viewer.sendMessage(ChatColor.GRAY  + "  " + ChatColor.WHITE   + "Common:    " + stats.commonHarvests);
        viewer.sendMessage(ChatColor.AQUA  + "  " + ChatColor.AQUA    + "Rare:      " + stats.rareHarvests);
        viewer.sendMessage(ChatColor.LIGHT_PURPLE + "  " + ChatColor.LIGHT_PURPLE + "Epic:      " + stats.epicHarvests);
        viewer.sendMessage(ChatColor.GOLD  + "  " + ChatColor.GOLD    + "Legendary: " + stats.legendaryHarvests);
        viewer.sendMessage(ChatColor.RED   + "  " + ChatColor.RED     + "Mythic:    " + stats.mythicHarvests);
        viewer.sendMessage("");

        // Per-crop breakdown
        if (!stats.cropHarvests.isEmpty()) {
            viewer.sendMessage(ChatColor.YELLOW + "  Harvests by Crop");
            for (Map.Entry<String, Integer> entry : stats.cropHarvests.entrySet()) {
                String cropName = CropListener.formatName(org.bukkit.Material.valueOf(entry.getKey()));
                double earnings = stats.cropEarnings.getOrDefault(entry.getKey(), 0.0);
                viewer.sendMessage(ChatColor.GRAY + "  " + cropName + ": "
                    + ChatColor.WHITE + entry.getValue() + " harvests"
                    + ChatColor.GRAY + " | " + ChatColor.GOLD + "$" + String.format("%.2f", earnings));
            }
            viewer.sendMessage("");
        }

        // Personal records
        viewer.sendMessage(ChatColor.YELLOW + "  Personal Records");
        if (!stats.bestDropTier.equals("none")) {
            String bestCrop = stats.bestDropCrop.equals("none") ? "Unknown"
                : CropListener.formatName(org.bukkit.Material.valueOf(stats.bestDropCrop));
            viewer.sendMessage(ChatColor.GRAY + "  Best Drop:    " + ChatColor.GOLD + "$" + String.format("%.2f", stats.bestDropValue)
                + ChatColor.GRAY + " (" + stats.bestDropTier + " " + bestCrop + ", " + stats.bestDropWeight + " kg)");
        }
        if (!stats.heaviestTier.equals("none")) {
            String heavyCrop = stats.heaviestCrop.equals("none") ? "Unknown"
                : CropListener.formatName(org.bukkit.Material.valueOf(stats.heaviestCrop));
            viewer.sendMessage(ChatColor.GRAY + "  Heaviest Crop: " + ChatColor.WHITE + stats.heaviestWeight + " kg"
                + ChatColor.GRAY + " (" + stats.heaviestTier + " " + heavyCrop + ")");
        }

        viewer.sendMessage("");
        viewer.sendMessage(bar);
    }
}
