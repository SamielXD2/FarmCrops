package player.farmcrops;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDate;
import java.util.*;

/**
 * Daily Tasks System
 * Players complete daily farming tasks for rewards
 */
public class DailyTaskManager {
    
    private final FarmCrops plugin;
    private final Map<UUID, Map<String, Integer>> playerProgress; // UUID -> TaskID -> Progress
    private final Map<UUID, Set<String>> completedTasks; // UUID -> Completed Task IDs
    private final Map<UUID, LocalDate> lastReset; // Track when player's tasks were last reset
    
    public DailyTaskManager(FarmCrops plugin) {
        this.plugin = plugin;
        this.playerProgress = new HashMap<>();
        this.completedTasks = new HashMap<>();
        this.lastReset = new HashMap<>();
        
        // Start daily reset timer (checks every hour)
        startResetTimer();
    }
    
    private void startResetTimer() {
        new BukkitRunnable() {
            @Override
            public void run() {
                checkDailyReset();
            }
        }.runTaskTimer(plugin, 20L * 60 * 60, 20L * 60 * 60); // Every hour
    }
    
    private void checkDailyReset() {
        LocalDate today = LocalDate.now();
        
        for (UUID uuid : new HashSet<>(lastReset.keySet())) {
            LocalDate lastDate = lastReset.get(uuid);
            if (lastDate != null && lastDate.isBefore(today)) {
                resetPlayerTasks(uuid);
            }
        }
    }
    
    private void resetPlayerTasks(UUID uuid) {
        playerProgress.remove(uuid);
        completedTasks.remove(uuid);
        lastReset.put(uuid, LocalDate.now());
    }
    
    public void onCropHarvest(Player player, Material cropType) {
        UUID uuid = player.getUniqueId();
        
        // Check if need to reset for this player
        LocalDate today = LocalDate.now();
        LocalDate lastDate = lastReset.get(uuid);
        if (lastDate == null || lastDate.isBefore(today)) {
            resetPlayerTasks(uuid);
        }
        
        // Get tasks from config
        ConfigurationSection tasksSection = plugin.getConfig().getConfigurationSection("daily-tasks.tasks");
        if (tasksSection == null) return;
        
        for (String taskId : tasksSection.getKeys(false)) {
            ConfigurationSection task = tasksSection.getConfigurationSection(taskId);
            if (task == null) continue;
            
            // Check if already completed
            if (isTaskCompleted(uuid, taskId)) continue;
            
            // Check if crop type matches
            String requiredCrop = task.getString("crop-type", "");
            if (!requiredCrop.equalsIgnoreCase(cropType.name())) continue;
            
            // Increment progress
            Map<String, Integer> progress = playerProgress.computeIfAbsent(uuid, k -> new HashMap<>());
            int current = progress.getOrDefault(taskId, 0) + 1;
            progress.put(taskId, current);
            
            // Check if completed
            int requirement = task.getInt("requirement", 100);
            if (current >= requirement) {
                completeTask(player, taskId, task);
            } else {
                // Show progress
                if (current % 10 == 0) { // Every 10 crops
                    player.sendMessage(ChatColor.GRAY + "Daily Task Progress: " + 
                                     ChatColor.YELLOW + current + "/" + requirement + 
                                     ChatColor.GRAY + " - " + task.getString("name"));
                }
            }
        }
    }
    
    private void completeTask(Player player, String taskId, ConfigurationSection task) {
        UUID uuid = player.getUniqueId();
        
        // Mark as completed
        Set<String> completed = completedTasks.computeIfAbsent(uuid, k -> new HashSet<>());
        completed.add(taskId);
        
        // Give rewards
        double money = task.getDouble("rewards.money", 0);
        if (money > 0 && plugin.getEconomy() != null) {
            plugin.getEconomy().depositPlayer(player, money);
        }
        
        // Send message
        player.sendMessage("");
        player.sendMessage(ChatColor.GREEN + "═══════════════════════════════");
        player.sendMessage(ChatColor.GOLD + "★ DAILY TASK COMPLETED! ★");
        player.sendMessage(ChatColor.YELLOW + task.getString("name", "Task"));
        player.sendMessage(ChatColor.GRAY + task.getString("description", ""));
        if (money > 0) {
            player.sendMessage(ChatColor.GREEN + "Reward: " + ChatColor.GOLD + "$" + money);
        }
        player.sendMessage(ChatColor.GREEN + "═══════════════════════════════");
        player.sendMessage("");
    }
    
    public boolean isTaskCompleted(UUID uuid, String taskId) {
        return completedTasks.getOrDefault(uuid, new HashSet<>()).contains(taskId);
    }
    
    public int getTaskProgress(UUID uuid, String taskId) {
        return playerProgress.getOrDefault(uuid, new HashMap<>()).getOrDefault(taskId, 0);
    }
    
    public Set<String> getCompletedTasks(UUID uuid) {
        return new HashSet<>(completedTasks.getOrDefault(uuid, new HashSet<>()));
    }
}
