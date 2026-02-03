package player.farmcrops;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Command to create manual backups of player data
 * Usage: /farmbackup
 */
public class BackupCommand implements CommandExecutor {
    
    private final FarmCrops plugin;
    
    public BackupCommand(FarmCrops plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("farmcrops.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }
        
        sender.sendMessage(ChatColor.YELLOW + "Creating backup of all player data...");
        
        // Save all current data first
        if (plugin.getStatsManager() != null) {
            plugin.getStatsManager().saveAll();
        }
        if (plugin.getPlayerSettings() != null) {
            plugin.getPlayerSettings().saveSettings();
        }
        
        // Create backup folder
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String timestamp = dateFormat.format(new Date());
        String backupName = "backup_" + timestamp;
        
        File backupFolder = new File(plugin.getDataFolder().getParentFile(), "FarmCrops-Backups");
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }
        
        File backupDir = new File(backupFolder, backupName);
        backupDir.mkdirs();
        
        try {
            // Copy entire FarmCrops data folder
            copyFolder(plugin.getDataFolder().toPath(), backupDir.toPath());
            
            sender.sendMessage(ChatColor.GREEN + "✓ Backup created successfully!");
            sender.sendMessage(ChatColor.GRAY + "Location: plugins/FarmCrops-Backups/" + backupName);
            
            // Clean old backups (keep last 10)
            cleanOldBackups(backupFolder, 10);
            
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "✗ Backup failed: " + e.getMessage());
            plugin.getLogger().severe("Backup creation failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }
    
    private void copyFolder(Path source, Path target) throws IOException {
        Files.walk(source).forEach(sourcePath -> {
            try {
                Path targetPath = target.resolve(source.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                    if (!Files.exists(targetPath)) {
                        Files.createDirectories(targetPath);
                    }
                } else {
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    private void cleanOldBackups(File backupFolder, int keepCount) {
        File[] backups = backupFolder.listFiles((dir, name) -> name.startsWith("backup_"));
        if (backups == null || backups.length <= keepCount) {
            return;
        }
        
        // Sort by last modified date
        java.util.Arrays.sort(backups, (a, b) -> 
            Long.compare(b.lastModified(), a.lastModified())
        );
        
        // Delete old backups beyond keepCount
        for (int i = keepCount; i < backups.length; i++) {
            deleteFolder(backups[i]);
            plugin.getLogger().info("Cleaned old backup: " + backups[i].getName());
        }
    }
    
    private void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file);
                } else {
                    file.delete();
                }
            }
        }
        folder.delete();
    }
}
