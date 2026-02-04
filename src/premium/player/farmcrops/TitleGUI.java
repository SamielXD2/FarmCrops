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
 * GUI for managing player titles earned from achievements
 */
public class TitleGUI implements Listener {
    
    private final FarmCrops plugin;
    private final Map<Player, Inventory> playerGUIs = new HashMap<>();
    
    public TitleGUI(FarmCrops plugin) {
        this.plugin = plugin;
    }
    
    public void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.LIGHT_PURPLE + "👑 My Titles 👑");
        
        UUID uuid = player.getUniqueId();
        List<String> unlockedTitles = plugin.getTitleManager().getUnlockedTitles(uuid);
        String equippedTitle = plugin.getTitleManager().getEquippedTitle(uuid);
        
        // Fill background
        ItemStack bgGlass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, bgGlass);
        }
        
        // Display titles
        int[] displaySlots = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        };
        
        // Get all achievement data to map titles to achievements
        Collection<AchievementManager.AchievementData> allAchievements = plugin.getAchievementManager().getAllAchievements();
        List<TitleDisplay> titleDisplays = new ArrayList<>();
        
        for (AchievementManager.AchievementData ach : allAchievements) {
            String title = plugin.getTitleManager().getTitleForAchievement(ach.id);
            if (title != null) {
                boolean unlocked = unlockedTitles.contains(title);
                boolean equipped = title.equals(equippedTitle);
                titleDisplays.add(new TitleDisplay(title, ach.id, ach.name, unlocked, equipped));
            }
        }
        
        int slotIndex = 0;
        for (TitleDisplay td : titleDisplays) {
            if (slotIndex >= displaySlots.length) break;
            
            ItemStack titleItem = createTitleItem(td);
            gui.setItem(displaySlots[slotIndex], titleItem);
            slotIndex++;
        }
        
        // Info button
        gui.setItem(49, createItem(Material.PAPER,
            ChatColor.AQUA + "" + ChatColor.BOLD + "ℹ Title Info",
            "",
            ChatColor.GRAY + "Titles are earned by completing",
            ChatColor.GRAY + "achievements. Equip one to show",
            ChatColor.GRAY + "it off to other players!",
            "",
            ChatColor.YELLOW + "Unlocked: " + ChatColor.GREEN + unlockedTitles.size(),
            ChatColor.YELLOW + "Equipped: " + (equippedTitle != null ? colorize(equippedTitle) : ChatColor.GRAY + "None")
        ));
        
        // Unequip button (if title is equipped)
        if (equippedTitle != null) {
            gui.setItem(45, createItem(Material.BARRIER,
                ChatColor.RED + "" + ChatColor.BOLD + "Unequip Title",
                ChatColor.GRAY + "Remove your current title",
                "",
                ChatColor.YELLOW + "Click to unequip"
            ));
        }
        
        // Back button
        gui.setItem(53, createItem(Material.ARROW,
            ChatColor.RED + "← Back to Achievements",
            ChatColor.GRAY + "Return to achievements menu"
        ));
        
        playerGUIs.put(player, gui);
        player.openInventory(gui);
    }
    
    private ItemStack createTitleItem(TitleDisplay td) {
        Material mat;
        ChatColor nameColor;
        List<String> lore = new ArrayList<>();
        
        if (td.equipped) {
            mat = Material.ENCHANTED_BOOK;
            nameColor = ChatColor.GOLD;
            lore.add("");
            lore.add(ChatColor.GRAY + "From: " + ChatColor.YELLOW + td.achievementName);
            lore.add("");
            lore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "✓ CURRENTLY EQUIPPED");
            lore.add("");
            lore.add(ChatColor.RED + "Click to unequip");
            
        } else if (td.unlocked) {
            mat = Material.NAME_TAG;
            nameColor = ChatColor.GREEN;
            lore.add("");
            lore.add(ChatColor.GRAY + "From: " + ChatColor.YELLOW + td.achievementName);
            lore.add("");
            lore.add(ChatColor.GREEN + "✓ Unlocked");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to equip");
            
        } else {
            mat = Material.GRAY_DYE;
            nameColor = ChatColor.DARK_GRAY;
            lore.add("");
            lore.add(ChatColor.GRAY + "From: " + ChatColor.DARK_GRAY + td.achievementName);
            lore.add("");
            lore.add(ChatColor.RED + "✗ Locked");
            lore.add(ChatColor.DARK_GRAY + "Complete the achievement to unlock");
        }
        
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(colorize(td.title));
            meta.setLore(lore);
            
            // Enchant effect for equipped
            if (td.equipped) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
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
        
        String title = InventoryUtil.getTitle(event.getView());
        if (!title.contains("My Titles")) return;
        
        event.setCancelled(true);
        
        Inventory gui = playerGUIs.get(player);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(gui)) return;
        
        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        
        // Title slots
        int[] displaySlots = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        };
        
        for (int displaySlot : displaySlots) {
            if (slot == displaySlot) {
                ItemMeta meta = clicked.getItemMeta();
                if (meta != null && meta.hasLore()) {
                    List<String> lore = meta.getLore();
                    for (String line : lore) {
                        if (line.contains("Click to equip")) {
                            // Find achievement ID for this title
                            String titleText = meta.getDisplayName();
                            String achId = findAchievementIdForTitle(titleText);
                            if (achId != null) {
                                plugin.getTitleManager().equipTitle(player, achId);
                                player.closeInventory();
                                playerGUIs.remove(player);
                                openGUI(player); // Refresh
                            }
                            return;
                        } else if (line.contains("Click to unequip")) {
                            plugin.getTitleManager().unequipTitle(player);
                            player.closeInventory();
                            playerGUIs.remove(player);
                            openGUI(player); // Refresh
                            return;
                        }
                    }
                }
                return;
            }
        }
        
        // Unequip button
        if (slot == 45) {
            plugin.getTitleManager().unequipTitle(player);
            player.closeInventory();
            playerGUIs.remove(player);
            openGUI(player);
        }
        // Back button
        else if (slot == 53) {
            player.closeInventory();
            playerGUIs.remove(player);
            plugin.getAchievementGUI().openGUI(player, 1);
        }
    }
    
    private String findAchievementIdForTitle(String titleText) {
        for (AchievementManager.AchievementData ach : plugin.getAchievementManager().getAllAchievements()) {
            String achTitle = plugin.getTitleManager().getTitleForAchievement(ach.id);
            if (achTitle != null && colorize(achTitle).equals(titleText)) {
                return ach.id;
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
    
    private static class TitleDisplay {
        String title;
        String achievementId;
        String achievementName;
        boolean unlocked;
        boolean equipped;
        
        TitleDisplay(String title, String achievementId, String achievementName, boolean unlocked, boolean equipped) {
            this.title = title;
            this.achievementId = achievementId;
            this.achievementName = achievementName;
            this.unlocked = unlocked;
            this.equipped = equipped;
        }
    }
}
