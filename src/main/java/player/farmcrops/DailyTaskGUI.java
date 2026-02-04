package player.farmcrops;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Daily Tasks GUI (Premium Feature)
 * Shows players their daily farming objectives
 * 
 * Features:
 * - View all daily tasks
 * - See progress on each task
 * - Claim rewards when complete
 * - Resets daily at midnight
 */
public class DailyTaskGUI implements Listener {
    
    private final FarmCrops plugin;
    private static final String GUI_TITLE = ChatColor.DARK_GREEN + "Daily Tasks";
    
    public DailyTaskGUI(FarmCrops plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Open the Daily Tasks GUI for a player
     */
    public void openGUI(Player player) {
        // Premium check
        if (!plugin.isPremiumEdition()) {
            player.sendMessage(ChatColor.RED + "✗ Daily Tasks is a Premium feature!");
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);
        
        DailyTaskManager taskManager = plugin.getDailyTaskManager();
        if (taskManager == null) {
            player.sendMessage(ChatColor.RED + "✗ Daily Tasks system is not available!");
            return;
        }
        
        UUID uuid = player.getUniqueId();
        
        // Header decoration
        fillBorders(gui);
        
        // Get stats for dynamic task progress
        StatsManager.PlayerStats stats = plugin.getStatsManager().getStats(uuid);
        
        // Daily Tasks with actual progress from DailyTaskManager
        addTaskItem(gui, 10, Material.WHEAT, "Harvest Wheat",
            taskManager.getTaskProgress(uuid, "harvest_wheat"),
            plugin.getConfig().getInt("daily-tasks.tasks.harvest_wheat.requirement", 50),
            "Harvest wheat crops", 100, "experience",
            taskManager.isTaskCompleted(uuid, "harvest_wheat"));
        
        addTaskItem(gui, 11, Material.CARROT, "Harvest Carrots",
            taskManager.getTaskProgress(uuid, "harvest_carrot"),
            plugin.getConfig().getInt("daily-tasks.tasks.harvest_carrot.requirement", 50),
            "Harvest carrot crops", 100, "experience",
            taskManager.isTaskCompleted(uuid, "harvest_carrot"));
        
        addTaskItem(gui, 12, Material.POTATO, "Harvest Potatoes",
            taskManager.getTaskProgress(uuid, "harvest_potato"),
            plugin.getConfig().getInt("daily-tasks.tasks.harvest_potato.requirement", 50),
            "Harvest potato crops", 100, "experience",
            taskManager.isTaskCompleted(uuid, "harvest_potato"));
        
        addTaskItem(gui, 13, Material.BEETROOT, "Harvest Beetroots",
            taskManager.getTaskProgress(uuid, "harvest_beetroot"),
            plugin.getConfig().getInt("daily-tasks.tasks.harvest_beetroot.requirement", 50),
            "Harvest beetroot crops", 100, "experience",
            taskManager.isTaskCompleted(uuid, "harvest_beetroot"));
        
        addTaskItem(gui, 14, Material.MELON, "Harvest Melons",
            taskManager.getTaskProgress(uuid, "harvest_melon"),
            plugin.getConfig().getInt("daily-tasks.tasks.harvest_melon.requirement", 30),
            "Harvest melon blocks", 100, "experience",
            taskManager.isTaskCompleted(uuid, "harvest_melon"));
        
        // Tier-based tasks (using actual stats)
        addTaskItem(gui, 19, Material.EMERALD, "Harvest Legendary Crops",
            stats.legendaryHarvests,
            plugin.getConfig().getInt("daily-tasks.legendary-target", 10),
            "Harvest legendary tier crops", 250, "experience",
            stats.legendaryHarvests >= plugin.getConfig().getInt("daily-tasks.legendary-target", 10));
        
        addTaskItem(gui, 20, Material.DIAMOND, "Earn Money",
            (int)stats.totalEarnings,
            plugin.getConfig().getInt("daily-tasks.money-target", 1000),
            "Earn money from farming", 500, "money",
            stats.totalEarnings >= plugin.getConfig().getInt("daily-tasks.money-target", 1000));
        
        addTaskItem(gui, 21, Material.GOLD_INGOT, "Total Harvests",
            stats.totalHarvests,
            plugin.getConfig().getInt("daily-tasks.harvest-target", 100),
            "Harvest any crops", 150, "experience",
            stats.totalHarvests >= plugin.getConfig().getInt("daily-tasks.harvest-target", 100));
        
        // Info item
        ItemStack infoItem = createItem(Material.BOOK,
            ChatColor.YELLOW + "Daily Tasks Info",
            ChatColor.GRAY + "Complete tasks to earn rewards!",
            "",
            ChatColor.AQUA + "Tasks reset at midnight",
            ChatColor.GREEN + "Claim rewards when complete"
        );
        gui.setItem(40, infoItem);
        
        // Refresh button
        ItemStack refreshItem = createItem(Material.ARROW,
            ChatColor.GREEN + "Refresh",
            ChatColor.GRAY + "Click to refresh task progress"
        );
        gui.setItem(49, refreshItem);
        
        // Close button
        ItemStack closeItem = createItem(Material.BARRIER,
            ChatColor.RED + "Close",
            ChatColor.GRAY + "Close this menu"
        );
        gui.setItem(45, closeItem);
        
        player.openInventory(gui);
    }
    
    /**
     * Add a task item to the GUI
     */
    private void addTaskItem(Inventory gui, int slot, Material material, String taskName,
                             int progress, int target, String description, int reward, 
                             String rewardType, boolean completed) {
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + description);
        lore.add("");
        
        // Progress bar
        double percentage = Math.min(100.0, (progress * 100.0) / target);
        int filledBars = (int)(percentage / 10);
        
        StringBuilder progressBar = new StringBuilder(ChatColor.GRAY + "[");
        for (int i = 0; i < 10; i++) {
            if (i < filledBars) {
                progressBar.append(ChatColor.GREEN + "█");
            } else {
                progressBar.append(ChatColor.DARK_GRAY + "█");
            }
        }
        progressBar.append(ChatColor.GRAY + "] " + ChatColor.YELLOW + (int)percentage + "%");
        
        lore.add(progressBar.toString());
        lore.add(ChatColor.GRAY + "Progress: " + ChatColor.WHITE + progress + ChatColor.GRAY + "/" + ChatColor.WHITE + target);
        lore.add("");
        
        if (completed) {
            lore.add(ChatColor.GREEN + "✓ COMPLETED!");
            lore.add(ChatColor.GOLD + "Reward: " + ChatColor.YELLOW + "+" + reward + " " + rewardType);
            lore.add("");
            lore.add(ChatColor.GREEN + "» Click to claim reward «");
        } else {
            lore.add(ChatColor.YELLOW + "Reward: " + ChatColor.WHITE + "+" + reward + " " + rewardType);
            lore.add(ChatColor.RED + "Not yet completed");
        }
        
        ItemStack item = createItem(
            completed ? Material.LIME_STAINED_GLASS_PANE : material,
            (completed ? ChatColor.GREEN : ChatColor.YELLOW) + taskName,
            lore.toArray(new String[0])
        );
        
        gui.setItem(slot, item);
    }
    
    /**
     * Fill GUI borders with decoration
     */
    private void fillBorders(Inventory gui) {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            border.setItemMeta(meta);
        }
        
        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, border);
            gui.setItem(i + 45, border);
        }
        
        // Left and right columns
        for (int i = 1; i < 5; i++) {
            gui.setItem(i * 9, border);
            gui.setItem(i * 9 + 8, border);
        }
    }
    
    /**
     * Create an item with name and lore
     */
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            
            if (lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String line : lore) {
                    loreList.add(line);
                }
                meta.setLore(loreList);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        int slot = event.getSlot();
        
        // Close button
        if (slot == 45) {
            player.closeInventory();
            return;
        }
        
        // Refresh button
        if (slot == 49) {
            openGUI(player); // Refresh the GUI
            player.sendMessage(ChatColor.GREEN + "✓ Tasks refreshed!");
            return;
        }
        
        // Task claim buttons (slots 10-14, 19-21)
        if (isTaskSlot(slot)) {
            // Check if task is completed
            if (clicked.getType() == Material.LIME_STAINED_GLASS_PANE) {
                player.sendMessage(ChatColor.GREEN + "✓ Reward claimed!");
                player.sendMessage(ChatColor.GRAY + "(Reward system coming soon!)");
                
                // TODO: Actually give rewards when reward system is implemented
                
                // Refresh GUI
                openGUI(player);
            } else {
                player.sendMessage(ChatColor.RED + "✗ Complete this task first!");
            }
        }
    }
    
    /**
     * Check if a slot is a task slot
     */
    private boolean isTaskSlot(int slot) {
        return (slot >= 10 && slot <= 14) || (slot >= 19 && slot <= 21);
    }
}
