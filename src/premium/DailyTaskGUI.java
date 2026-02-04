package player.farmcrops;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
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
 * PREMIUM EDITION - Daily Task GUI
 */
public class DailyTaskGUI implements Listener {
    
    private final FarmCrops plugin;
    
    public DailyTaskGUI(FarmCrops plugin) {
        this.plugin = plugin;
    }
    
    public void openDailyTasksGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_GREEN + "Daily Tasks");
        
        ConfigurationSection tasksSection = plugin.getConfig().getConfigurationSection("daily-tasks.tasks");
        if (tasksSection == null) {
            player.sendMessage(ChatColor.RED + "No daily tasks configured!");
            return;
        }
        
        int slot = 10;
        UUID uuid = player.getUniqueId();
        
        for (String taskId : tasksSection.getKeys(false)) {
            if (slot >= 44) break; // Prevent overflow
            
            ConfigurationSection task = tasksSection.getConfigurationSection(taskId);
            if (task == null) continue;
            
            // Get task info
            String name = task.getString("name", "Unknown Task");
            String description = task.getString("description", "");
            String cropType = task.getString("crop-type", "WHEAT");
            int requirement = task.getInt("requirement", 100);
            double reward = task.getDouble("rewards.money", 0);
            
            // Get progress
            int progress = plugin.getDailyTaskManager().getTaskProgress(uuid, taskId);
            boolean completed = plugin.getDailyTaskManager().isTaskCompleted(uuid, taskId);
            
            // Create item
            Material material;
            try {
                material = Material.valueOf(cropType);
            } catch (IllegalArgumentException e) {
                material = Material.WHEAT;
            }
            
            ItemStack item = new ItemStack(completed ? Material.LIME_DYE : material);
            ItemMeta meta = item.getItemMeta();
            
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + name);
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add(ChatColor.GRAY + description);
                lore.add("");
                
                if (completed) {
                    lore.add(ChatColor.GREEN + "✓ COMPLETED!");
                } else {
                    lore.add(ChatColor.YELLOW + "Progress: " + ChatColor.WHITE + progress + "/" + requirement);
                    double percentage = (double) progress / requirement * 100;
                    lore.add(getProgressBar(percentage));
                }
                
                lore.add("");
                lore.add(ChatColor.GOLD + "Reward: " + ChatColor.GREEN + "$" + reward);
                lore.add("");
                
                if (completed) {
                    lore.add(ChatColor.GREEN + "Come back tomorrow for new tasks!");
                } else {
                    lore.add(ChatColor.GRAY + "Keep farming to complete this task!");
                }
                
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            
            inv.setItem(slot, item);
            slot++;
            
            // Skip slots for formatting (create nice grid)
            if ((slot - 10) % 7 == 0) {
                slot += 2;
            }
        }
        
        // Info item
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(ChatColor.AQUA + "Daily Tasks Info");
            List<String> infoLore = new ArrayList<>();
            infoLore.add("");
            infoLore.add(ChatColor.GRAY + "Complete daily farming tasks");
            infoLore.add(ChatColor.GRAY + "to earn rewards!");
            infoLore.add("");
            infoLore.add(ChatColor.YELLOW + "Tasks reset daily at midnight");
            infoLore.add("");
            infoMeta.setLore(infoLore);
            info.setItemMeta(infoMeta);
        }
        inv.setItem(49, info);
        
        player.openInventory(inv);
    }
    
    private String getProgressBar(double percentage) {
        int bars = 20;
        int filled = (int) (bars * percentage / 100);
        
        StringBuilder bar = new StringBuilder(ChatColor.GRAY + "[");
        for (int i = 0; i < bars; i++) {
            if (i < filled) {
                bar.append(ChatColor.GREEN).append("█");
            } else {
                bar.append(ChatColor.DARK_GRAY).append("█");
            }
        }
        bar.append(ChatColor.GRAY).append("] ").append(ChatColor.YELLOW).append(String.format("%.1f%%", percentage));
        
        return bar.toString();
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.DARK_GREEN + "Daily Tasks")) {
            event.setCancelled(true);
        }
    }
}
