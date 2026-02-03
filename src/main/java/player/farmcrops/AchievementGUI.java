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

import java.util.*;

/**
 * GUI for viewing achievements
 */
public class AchievementGUI implements Listener {
    
    private final FarmCrops plugin;
    private final Map<Player, Inventory> playerGUIs = new HashMap<>();
    
    public AchievementGUI(FarmCrops plugin) {
        this.plugin = plugin;
    }
    
    public void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.GOLD + "✦ Achievements ✦");
        
        AchievementManager achMgr = plugin.getAchievementManager();
        if (achMgr == null) {
            player.sendMessage(ChatColor.RED + "Achievements are not enabled!");
            return;
        }
        
        Set<String> unlocked = achMgr.getUnlockedAchievements(player.getUniqueId());
        
        // Achievement slots (0-44)
        int slot = 0;
        
        // First Harvest Achievement
        ItemStack firstHarvest = createAchievementItem(
            unlocked.contains("first_harvest"),
            Material.WHEAT,
            "First Harvest",
            "Harvest your first crop!",
            "$10.0"
        );
        gui.setItem(slot++, firstHarvest);
        
        // Farming Apprentice
        ItemStack apprentice = createAchievementItem(
            unlocked.contains("farming_apprentice"),
            Material.IRON_HOE,
            "Farming Apprentice",
            "Harvest 100 crops",
            "$100.0"
        );
        gui.setItem(slot++, apprentice);
        
        // Farming Expert
        ItemStack expert = createAchievementItem(
            unlocked.contains("farming_expert"),
            Material.DIAMOND_HOE,
            "Farming Expert",
            "Harvest 500 crops",
            "$500.0"
        );
        gui.setItem(slot++, expert);
        
        // Farming Master
        ItemStack master = createAchievementItem(
            unlocked.contains("farming_master"),
            Material.NETHERITE_HOE,
            "Farming Master",
            "Harvest 1,000 crops",
            "$1,000.0"
        );
        gui.setItem(slot++, master);
        
        // Rich Farmer
        ItemStack rich = createAchievementItem(
            unlocked.contains("rich_farmer"),
            Material.GOLD_INGOT,
            "Rich Farmer",
            "Earn $10,000 from crops",
            "Special Perk"
        );
        gui.setItem(slot++, rich);
        
        // Millionaire
        ItemStack millionaire = createAchievementItem(
            unlocked.contains("millionaire"),
            Material.DIAMOND,
            "Millionaire",
            "Earn $100,000 from crops",
            "Exclusive Title"
        );
        gui.setItem(slot++, millionaire);
        
        // Lucky Farmer
        ItemStack lucky = createAchievementItem(
            unlocked.contains("lucky_farmer"),
            Material.EMERALD,
            "Lucky Farmer",
            "Get your first Legendary crop",
            "$250.0"
        );
        gui.setItem(slot++, lucky);
        
        // Rare Collector
        ItemStack collector = createAchievementItem(
            unlocked.contains("rare_collector"),
            Material.ENCHANTED_BOOK,
            "Rare Collector",
            "Collect 10 Legendary crops",
            "$500.0"
        );
        gui.setItem(slot++, collector);
        
        // Bottom bar
        ItemStack grayGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = grayGlass.getItemMeta();
        glassMeta.setDisplayName(" ");
        grayGlass.setItemMeta(glassMeta);
        
        for (int i = 45; i < 54; i++) {
            gui.setItem(i, grayGlass);
        }
        
        // Close button
        ItemStack closeBtn = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeBtn.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "CLOSE");
        closeMeta.setLore(Arrays.asList(ChatColor.GRAY + "Click to close"));
        closeBtn.setItemMeta(closeMeta);
        gui.setItem(53, closeBtn);
        
        // Info button
        ItemStack infoBtn = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoBtn.getItemMeta();
        infoMeta.setDisplayName(ChatColor.AQUA + "Achievement Info");
        infoMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "Complete achievements to earn rewards!",
            "",
            ChatColor.YELLOW + "Unlocked: " + ChatColor.GREEN + unlocked.size(),
            ChatColor.YELLOW + "Total: " + ChatColor.WHITE + "8"
        ));
        infoBtn.setItemMeta(infoMeta);
        gui.setItem(49, infoBtn);
        
        playerGUIs.put(player, gui);
        player.openInventory(gui);
    }
    
    private ItemStack createAchievementItem(boolean unlocked, Material material, 
                                           String name, String description, String reward) {
        ItemStack item = new ItemStack(unlocked ? material : Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        
        if (unlocked) {
            meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "✓ " + name);
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + description,
                "",
                ChatColor.GOLD + "Reward: " + ChatColor.YELLOW + reward,
                "",
                ChatColor.GREEN + "" + ChatColor.BOLD + "UNLOCKED!"
            ));
        } else {
            meta.setDisplayName(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "✗ " + name);
            meta.setLore(Arrays.asList(
                ChatColor.DARK_GRAY + description,
                "",
                ChatColor.GOLD + "Reward: " + ChatColor.YELLOW + reward,
                "",
                ChatColor.RED + "LOCKED"
            ));
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        if (!playerGUIs.containsKey(player)) return;
        
        String title = InventoryUtil.getTitle(event.getView());
        if (!title.equals(ChatColor.GOLD + "✦ Achievements ✦")) return;
        
        event.setCancelled(true);
        
        int slot = event.getSlot();
        
        // Close button
        if (slot == 53) {
            player.closeInventory();
            playerGUIs.remove(player);
        }
    }
}
