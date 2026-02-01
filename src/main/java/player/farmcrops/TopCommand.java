package player.farmcrops;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /farmtop [page] — View the top farmers leaderboard.
 * Supports pagination. Sort criteria configured in config.yml (earnings or harvests).
 */
public class TopCommand implements CommandExecutor {

    private final FarmCrops plugin;

    public TopCommand(FarmCrops plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        int pageSize = plugin.getConfig().getInt("leaderboard.page-size", 10);
        String sortBy = plugin.getConfig().getString("leaderboard.sort-by", "earnings");

        // Parse page number
        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
                if (page < 1) page = 1;
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid page number. Usage: /farmtop [page]");
                return true;
            }
        }

        // Get enough entries for pagination
        // We fetch all and paginate client-side since the list is small
        int totalNeeded = page * pageSize;
        List<StatsManager.LeaderboardEntry> allEntries = plugin.getStatsManager().getLeaderboard(totalNeeded);

        int totalPages = (int) Math.ceil((double) allEntries.size() / pageSize);
        if (totalPages == 0) totalPages = 1;

        if (page > totalPages) {
            player.sendMessage(ChatColor.RED + "Page " + page + " doesn't exist. Total pages: " + totalPages);
            return true;
        }

        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, allEntries.size());

        // Display
        String bar = ChatColor.DARK_GREEN + "══════════════════════════════";
        String sortLabel = "earnings".equalsIgnoreCase(sortBy) ? "Earnings" : "Harvests";

        player.sendMessage("");
        player.sendMessage(bar);
        player.sendMessage(ChatColor.GREEN + "  🏆 Top Farmers — " + ChatColor.GRAY + "by " + sortLabel
            + " (Page " + page + "/" + totalPages + ")");
        player.sendMessage(bar);
        player.sendMessage("");

        if (allEntries.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "  No farmers yet. Start harvesting!");
        } else {
            for (int i = startIndex; i < endIndex; i++) {
                StatsManager.LeaderboardEntry entry = allEntries.get(i);
                int rank = i + 1;

                // Medal emoji for top 3
                String rankPrefix;
                if (rank == 1)      rankPrefix = ChatColor.GOLD   + "  🥇 ";
                else if (rank == 2) rankPrefix = ChatColor.GRAY   + "  🥈 ";
                else if (rank == 3) rankPrefix = ChatColor.RED    + "  🥉 ";
                else                rankPrefix = ChatColor.WHITE  + "  " + rank + ". ";

                String name = entry.name;
                // Highlight if it's the viewer
                if (entry.uuid.equals(player.getUniqueId())) {
                    name = ChatColor.YELLOW + name + ChatColor.YELLOW;
                }

                String stats;
                if ("earnings".equalsIgnoreCase(sortBy)) {
                    stats = ChatColor.GOLD + "$" + String.format("%.2f", entry.earnings)
                        + ChatColor.GRAY + " (" + entry.harvests + " harvests)";
                } else {
                    stats = "" + ChatColor.WHITE + entry.harvests + " harvests"
                        + ChatColor.GRAY + " ($" + String.format("%.2f", entry.earnings) + ")";
                }

                player.sendMessage(rankPrefix + ChatColor.WHITE + name + ChatColor.GRAY + " — " + stats);
            }
        }

        player.sendMessage("");

        // Pagination hint
        if (totalPages > 1) {
            player.sendMessage(ChatColor.GRAY + "  Use /farmtop <page> to navigate.");
        }

        player.sendMessage(bar);
        return true;
    }
}
