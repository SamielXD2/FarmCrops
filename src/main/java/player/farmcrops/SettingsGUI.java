package player.farmcrops;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * v0.8.0 Settings GUI — Visual categories with custom crops toggle
 * 
 * Layout (54-slot inventory):
 *   Row 0-1: Visual Settings (holograms, particles)
 *   Row 2-3: Gameplay Settings (custom crops, seeds)
 *   Row 4: Leaderboard Settings
 *   Row 5: Bottom bar with save/close buttons
 */
public class SettingsGUI implements Listener {

    private final FarmCrops plugin;
    private final Map<Player, Inventory> playerGUIs = new HashMap<>();

    // Setting definitions: [configPath, displayName, description, category, material]
    private static final String[][] SETTINGS = {
        // Visual Settings
        {"holograms.harvest-flash", "Harvest Hologram", "Show hologram when crop is harvested", "visual", "BEACON"},
        {"holograms.growing-cursor", "Growing Cursor", "Show hologram when looking at crops", "visual", "SPYGLASS"},
        {"holograms.particles", "Particles", "Particle effects on harvest", "visual", "BLAZE_POWDER"},
        
        // Gameplay Settings
        {"custom-crops.enabled", "Custom Crops", "Enable custom weighted crop drops", "gameplay", "WHEAT"},
        {"seeds.enabled", "Seed Drops", "Drop seeds when harvesting crops", "gameplay", "WHEAT_SEEDS"},
        
        // Leaderboard Settings  
        {"leaderboard.sort-by-earnings", "Sort by Earnings", "Leaderboard sorted by money (off = harvests)", "leaderboard", "EMERALD"},
    };

    public SettingsGUI(FarmCrops plugin) {
        this.plugin = plugin;
    }

    public void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.DARK_GREEN + "⚙ FarmCrops Settings");

        // Fill background with black stained glass
        ItemStack bgGlass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, bgGlass);
        }

        // Category headers
        gui.setItem(0, createCategoryHeader(Material.ENDER_EYE, "&e&l⚡ Visual Settings", 
            "Holograms and particle effects"));
        gui.setItem(18, createCategoryHeader(Material.GOLDEN_APPLE, "&a&l🎮 Gameplay Settings", 
            "Core gameplay features"));
        gui.setItem(36, createCategoryHeader(Material.GOLD_INGOT, "&6&l🏆 Leaderboard Settings", 
            "Ranking and sorting options"));

        // Visual Settings (Row 1)
        gui.setItem(10, buildToggleButton("holograms.harvest-flash", "Harvest Hologram", 
            "Show hologram when crop is harvested", Material.BEACON));
        gui.setItem(11, buildToggleButton("holograms.growing-cursor", "Growing Cursor", 
            "Show hologram when looking at crops", Material.SPYGLASS));
        gui.setItem(12, buildToggleButton("holograms.particles", "Particles", 
            "Particle effects on harvest", Material.BLAZE_POWDER));

        // Gameplay Settings (Row 3)
        gui.setItem(28, buildToggleButton("custom-crops.enabled", "Custom Crops", 
            "Enable custom weighted crop drops", Material.WHEAT));
        gui.setItem(29, buildToggleButton("seeds.enabled", "Seed Drops", 
            "Drop seeds when harvesting crops", Material.WHEAT_SEEDS));

        // Leaderboard Settings (Row 5) - special toggle
        gui.setItem(46, buildLeaderboardSortButton());

        // Bottom bar
        ItemStack grayGlass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            gui.setItem(i, grayGlass);
        }

        // Save button
        gui.setItem(48, createItem(Material.LIME_STAINED_GLASS, 
            ChatColor.GREEN + "" + ChatColor.BOLD + "✓ SAVE & CLOSE",
            ChatColor.GRAY + "Save all changes and exit"));

        // Close button (no save)
        gui.setItem(50, createItem(Material.BARRIER,
            ChatColor.RED + "" + ChatColor.BOLD + "✗ CANCEL",
            ChatColor.GRAY + "Exit without saving"));

        playerGUIs.put(player, gui);
        player.openInventory(gui);
    }

    private ItemStack buildToggleButton(String configPath, String name, String description, Material mat) {
        boolean enabled = plugin.getConfig().getBoolean(configPath, true);
        
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String status = enabled ? ChatColor.GREEN + "✓ ON" : ChatColor.RED + "✗ OFF";
            meta.setDisplayName(ChatColor.YELLOW + name + " " + ChatColor.GRAY + "— " + status);
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + description);
            lore.add("");
            if (enabled) {
                lore.add(ChatColor.GREEN + "● Currently ENABLED");
            } else {
                lore.add(ChatColor.RED + "○ Currently DISABLED");
            }
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to toggle");
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    private ItemStack buildLeaderboardSortButton() {
        String sortBy = plugin.getConfig().getString("leaderboard.sort-by", "earnings");
        boolean byEarnings = "earnings".equalsIgnoreCase(sortBy);
        
        Material mat = byEarnings ? Material.EMERALD : Material.WHEAT;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String currentSort = byEarnings ? "Earnings" : "Harvests";
            meta.setDisplayName(ChatColor.GOLD + "Leaderboard Sort " + ChatColor.GRAY + "— " + 
                ChatColor.YELLOW + currentSort);
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "How the /farmtop leaderboard is sorted");
            lore.add("");
            
            if (byEarnings) {
                lore.add(ChatColor.GREEN + "● Earnings (Money)");
                lore.add(ChatColor.GRAY + "○ Harvests (Count)");
            } else {
                lore.add(ChatColor.GRAY + "○ Earnings (Money)");
                lore.add(ChatColor.GREEN + "● Harvests (Count)");
            }
            
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to toggle");
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    private ItemStack createCategoryHeader(Material mat, String name, String description) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + description);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        if (!playerGUIs.containsKey(player)) return;
        
        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
            .serialize(event.getView().title());
        if (!title.contains("FarmCrops Settings")) return;

        event.setCancelled(true);
        
        Inventory gui = playerGUIs.get(player);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(gui)) return;

        int slot = event.getSlot();
        
        // Save & Close button
        if (slot == 48) {
            plugin.saveConfig();
            player.closeInventory();
            playerGUIs.remove(player);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            player.sendMessage(ChatColor.GREEN + "✓ Settings saved successfully!");
            return;
        }

        // Cancel button
        if (slot == 50) {
            plugin.reloadConfig();
            player.closeInventory();
            playerGUIs.remove(player);
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1.0f, 1.0f);
            player.sendMessage(ChatColor.YELLOW + "Settings not saved.");
            return;
        }

        // Toggle buttons
        String configPath = getConfigPathForSlot(slot);
        if (configPath != null) {
            if (configPath.equals("leaderboard.sort-by-earnings")) {
                // Special case: toggle between "earnings" and "harvests"
                String current = plugin.getConfig().getString("leaderboard.sort-by", "earnings");
                String newValue = "earnings".equalsIgnoreCase(current) ? "harvests" : "earnings";
                plugin.getConfig().set("leaderboard.sort-by", newValue);
                gui.setItem(slot, buildLeaderboardSortButton());
            } else {
                // Regular boolean toggle
                boolean current = plugin.getConfig().getBoolean(configPath, true);
                plugin.getConfig().set(configPath, !current);
                
                // Refresh the button
                SettingInfo info = getSettingInfo(configPath);
                if (info != null) {
                    gui.setItem(slot, buildToggleButton(configPath, info.name, info.description, info.material));
                }
            }
            
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        }
    }

    private String getConfigPathForSlot(int slot) {
        switch (slot) {
            case 10: return "holograms.harvest-flash";
            case 11: return "holograms.growing-cursor";
            case 12: return "holograms.particles";
            case 28: return "custom-crops.enabled";
            case 29: return "seeds.enabled";
            case 46: return "leaderboard.sort-by-earnings";
            default: return null;
        }
    }

    private SettingInfo getSettingInfo(String configPath) {
        switch (configPath) {
            case "holograms.harvest-flash":
                return new SettingInfo("Harvest Hologram", "Show hologram when crop is harvested", Material.BEACON);
            case "holograms.growing-cursor":
                return new SettingInfo("Growing Cursor", "Show hologram when looking at crops", Material.SPYGLASS);
            case "holograms.particles":
                return new SettingInfo("Particles", "Particle effects on harvest", Material.BLAZE_POWDER);
            case "custom-crops.enabled":
                return new SettingInfo("Custom Crops", "Enable custom weighted crop drops", Material.WHEAT);
            case "seeds.enabled":
                return new SettingInfo("Seed Drops", "Drop seeds when harvesting crops", Material.WHEAT_SEEDS);
            default:
                return null;
        }
    }

    private static class SettingInfo {
        final String name;
        final String description;
        final Material material;

        SettingInfo(String name, String description, Material material) {
            this.name = name;
            this.description = description;
            this.material = material;
        }
    }
}
