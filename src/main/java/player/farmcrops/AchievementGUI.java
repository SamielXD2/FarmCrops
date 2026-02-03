package player.farmcrops;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI for viewing and claiming achievements with title rewards
 */
public class AchievementGUI implements Listener {
    
    private final FarmCrops plugin;
    private final Map<Player, Inventory> playerGUIs = new HashMap<>();
    private final Map<Player, Integer> playerPages = new HashMap<>();
    
    public AchievementGUI(FarmCrops plugin) {
        this.plugin = plugin;
    }
    
    public void openGUI(Player player, int page) {
        AchievementManager achMgr = plugin.getAchievementManager();
        if (achMgr == null) {
            player.sendMessage(ChatColor.RED + "Achievements are not enabled!");
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.GOLD + "✦ Achievements ✦ Page " + page);
        
        UUID uuid = player.getUniqueId();
        Set<String> claimed = achMgr.getPlayerAchievements(uuid);
        Set<String> unclaimed = achMgr.getUnclaimedAchievements(uuid);
        
        // Get all achievements
        List<AchievementManager.AchievementData> allAchievements = new ArrayList<>(achMgr.getAllAchievements());
        
        // Calculate pagination
        int itemsPerPage = 28; // 4 rows of 7
        int totalPages = (int) Math.ceil((double) allAchievements.size() / itemsPerPage);
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allAchievements.size());
        
        // Fill background
        ItemStack bgGlass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, bgGlass);
        }
        
        // Display achievements (slots 10-43, skipping 17, 18, 26, 27, 35, 36)
        int[] displaySlots = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        };
        
        int slotIndex = 0;
        for (int i = startIndex; i < endIndex && slotIndex < displaySlots.length; i++, slotIndex++) {
            AchievementManager.AchievementData ach = allAchievements.get(i);
            boolean isClaimed = claimed.contains(ach.id);
            boolean isUnclaimed = unclaimed.contains(ach.id);
            
            ItemStack achItem = createAchievementItem(ach, isClaimed, isUnclaimed, player);
            gui.setItem(displaySlots[slotIndex], achItem);
        }
        
        // Navigation and controls
        // Info button
        gui.setItem(49, createItem(Material.BOOK,
            ChatColor.AQUA + "" + ChatColor.BOLD + "📊 Progress",
            "",
            ChatColor.YELLOW + "Claimed: " + ChatColor.GREEN + claimed.size() + ChatColor.GRAY + "/" + ChatColor.WHITE + achMgr.getTotalAchievements(),
            ChatColor.YELLOW + "Unclaimed: " + ChatColor.GOLD + unclaimed.size(),
            "",
            ChatColor.GRAY + "Click unclaimed achievements",
            ChatColor.GRAY + "to claim rewards and titles!"
        ));
        
        // Previous page
        if (page > 1) {
            gui.setItem(48, createItem(Material.ARROW,
                ChatColor.YELLOW + "← Previous Page",
                ChatColor.GRAY + "Go to page " + (page - 1)
            ));
        }
        
        // Next page
        if (page < totalPages) {
            gui.setItem(50, createItem(Material.ARROW,
                ChatColor.YELLOW + "Next Page →",
                ChatColor.GRAY + "Go to page " + (page + 1)
            ));
        }
        
        // Titles button
        gui.setItem(45, createItem(Material.NAME_TAG,
            ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "👑 My Titles",
            ChatColor.GRAY + "Manage your earned titles",
            "",
            ChatColor.YELLOW + "Click to open"
        ));
        
        // Back to main menu
        gui.setItem(53, createItem(Material.BARRIER,
            ChatColor.RED + "✗ Close",
            ChatColor.GRAY + "Exit menu"
        ));
        
        playerGUIs.put(player, gui);
        playerPages.put(player, page);
        player.openInventory(gui);
    }
    
    private ItemStack createAchievementItem(AchievementManager.AchievementData ach, boolean claimed, boolean unclaimed, Player player) {
        Material mat;
        ChatColor nameColor;
        List<String> lore = new ArrayList<>();
        
        if (claimed) {
            mat = getMaterialForAchievement(ach.id);
            nameColor = ChatColor.GREEN;
            lore.add(ChatColor.GRAY + ach.description);
            lore.add("");
            lore.add(ChatColor.GOLD + "Reward: " + ChatColor.YELLOW + "$" + ach.reward);
            
            // Show title if available
            String title = plugin.getTitleManager().getTitleForAchievement(ach.id);
            if (title != null) {
                lore.add(ChatColor.LIGHT_PURPLE + "Title: " + colorize(title));
            }
            
            lore.add("");
            lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "✓ CLAIMED!");
            
        } else if (unclaimed) {
            mat = getMaterialForAchievement(ach.id);
            nameColor = ChatColor.GOLD;
            lore.add(ChatColor.GRAY + ach.description);
            lore.add("");
            lore.add(ChatColor.GOLD + "Reward: " + ChatColor.YELLOW + "$" + ach.reward);
            
            // Show title if available
            String title = plugin.getTitleManager().getTitleForAchievement(ach.id);
            if (title != null) {
                lore.add(ChatColor.LIGHT_PURPLE + "Title: " + colorize(title));
            }
            
            lore.add("");
            lore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "⚡ READY TO CLAIM!");
            lore.add(ChatColor.YELLOW + "Click to claim rewards!");
            
        } else {
            mat = Material.GRAY_STAINED_GLASS_PANE;
            nameColor = ChatColor.DARK_GRAY;
            lore.add(ChatColor.DARK_GRAY + ach.description);
            lore.add("");
            lore.add(ChatColor.GRAY + "Reward: $" + ach.reward);
            lore.add("");
            lore.add(ChatColor.RED + "✗ LOCKED");
            lore.add(ChatColor.DARK_GRAY + "Progress: " + getProgress(player, ach));
        }
        
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(nameColor + "" + ChatColor.BOLD + ach.name);
            meta.setLore(lore);
            
            // Enchant effect for unclaimed
            if (unclaimed) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private String getProgress(Player player, AchievementManager.AchievementData ach) {
        StatsManager stats = plugin.getStatsManager();
        UUID uuid = player.getUniqueId();
        
        switch (ach.type) {
            case HARVEST:
                return stats.getTotalHarvests(uuid) + "/" + ach.requirement;
            case EARNINGS:
                return "$" + String.format("%.0f", stats.getTotalEarnings(uuid)) + "/$" + ach.requirement;
            case TIER_EPIC:
                return stats.getEpicHarvests(uuid) + "/" + ach.requirement;
            case TIER_LEGENDARY:
                return stats.getLegendaryHarvests(uuid) + "/" + ach.requirement;
            case COLLECTION_WHEAT:
                return stats.getCropHarvests(uuid, "WHEAT") + "/" + ach.requirement;
            case COLLECTION_CARROT:
                return stats.getCropHarvests(uuid, "CARROTS") + "/" + ach.requirement;
            case COLLECTION_POTATO:
                return stats.getCropHarvests(uuid, "POTATOES") + "/" + ach.requirement;
            default:
                return "0/" + ach.requirement;
        }
    }
    
    private Material getMaterialForAchievement(String id) {
        switch (id) {
            case "first_harvest": return Material.WHEAT;
            case "hundred_harvest": return Material.IRON_HOE;
            case "thousand_harvest": return Material.DIAMOND_HOE;
            case "ten_thousand_harvest": return Material.NETHERITE_HOE;
            case "fifty_thousand_harvest": return Material.BEACON;
            case "first_thousand": return Material.GOLD_INGOT;
            case "ten_thousand": return Material.DIAMOND;
            case "hundred_thousand": return Material.EMERALD;
            case "million_earned": return Material.NETHER_STAR;
            case "first_epic": return Material.AMETHYST_SHARD;
            case "first_legendary": return Material.DRAGON_EGG;
            case "collection_wheat_1000": return Material.HAY_BLOCK;
            case "collection_carrot_1000": return Material.CARROT;
            case "collection_potato_1000": return Material.POTATO;
            default: return Material.PAPER;
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        if (!playerGUIs.containsKey(player)) return;
        
        String title = InventoryUtil.getTitle(event.getView());
        if (!title.contains("✦ Achievements ✦")) return;
        
        event.setCancelled(true);
        
        Inventory gui = playerGUIs.get(player);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(gui)) return;
        
        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        
        int page = playerPages.getOrDefault(player, 1);
        
        // Check if it's an achievement slot that can be claimed
        int[] displaySlots = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        };
        
        for (int i = 0; i < displaySlots.length; i++) {
            if (slot == displaySlots[i]) {
                // Check if this achievement is unclaimed
                ItemMeta meta = clicked.getItemMeta();
                if (meta != null && meta.hasLore()) {
                    List<String> lore = meta.getLore();
                    for (String line : lore) {
                        if (line.contains("READY TO CLAIM")) {
                            // Find which achievement this is
                            String achName = ChatColor.stripColor(meta.getDisplayName());
                            AchievementManager.AchievementData achData = findAchievementByName(achName);
                            if (achData != null) {
                                if (plugin.getAchievementManager().claimAchievement(player, achData.id)) {
                                    player.closeInventory();
                                    playerGUIs.remove(player);
                                    openGUI(player, page); // Refresh
                                }
                            }
                            return;
                        }
                    }
                }
                return;
            }
        }
        
        // Navigation buttons
        if (slot == 48) { // Previous page
            player.closeInventory();
            playerGUIs.remove(player);
            openGUI(player, page - 1);
        } else if (slot == 50) { // Next page
            player.closeInventory();
            playerGUIs.remove(player);
            openGUI(player, page + 1);
        } else if (slot == 45) { // Titles
            player.closeInventory();
            playerGUIs.remove(player);
            plugin.getTitleGUI().openGUI(player);
        } else if (slot == 53) { // Close
            player.closeInventory();
            playerGUIs.remove(player);
        }
    }
    
    private AchievementManager.AchievementData findAchievementByName(String name) {
        for (AchievementManager.AchievementData ach : plugin.getAchievementManager().getAllAchievements()) {
            if (ach.name.equals(name)) {
                return ach;
            }
        }
        return null;
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
    
    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
