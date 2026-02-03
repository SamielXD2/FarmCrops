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
        
        Set<String> unlocked = achMgr.getPlayerAchievements(player.getUniqueId());
        
        // Achievement slots (0-44)
        int slot = 0;
        
        // First Harvest Achievement
        ItemStack firstHarvest = createAchievementItem(
            unlocked.contains("first_harvest"),
            Material.WHEAT,
            "First Harvest",
            "Harvest your first crop!",
            "$10.00"
        );
        gui.setItem(slot++, firstHarvest);
        
        // Century Farmer (100 harvests)
        ItemStack apprentice = createAchievementItem(
            unlocked.contains("hundred_harvest"),
            Material.IRON_HOE,
            "Century Farmer",
            "Harvest 100 crops",
            "$100.00"
        );
        gui.setItem(slot++, apprentice);
        
        // Master Farmer (1000 harvests)
        ItemStack expert = createAchievementItem(
            unlocked.contains("thousand_harvest"),
            Material.DIAMOND_HOE,
            "Master Farmer",
            "Harvest 1,000 crops",
            "$500.00"
        );
        gui.setItem(slot++, expert);
        
        // Legendary Farmer (10000 harvests)
        ItemStack master = createAchievementItem(
            unlocked.contains("ten_thousand_harvest"),
            Material.NETHERITE_HOE,
            "Legendary Farmer",
            "Harvest 10,000 crops",
            "$2,000.00"
        );
        gui.setItem(slot++, master);
        
        // Money Maker ($1,000 earned)
        ItemStack rich = createAchievementItem(
            unlocked.contains("first_thousand"),
            Material.GOLD_INGOT,
            "Money Maker",
            "Earn $1,000 from crops",
            "$50.00"
        );
        gui.setItem(slot++, rich);
        
        // Rich Farmer ($10,000 earned)
        ItemStack millionaire = createAchievementItem(
            unlocked.contains("ten_thousand"),
            Material.DIAMOND,
            "Rich Farmer",
            "Earn $10,000 from crops",
            "$250.00"
        );
        gui.setItem(slot++, millionaire);
        
        // Crop Tycoon ($100,000 earned)
        ItemStack lucky = createAchievementItem(
            unlocked.contains("hundred_thousand"),
            Material.EMERALD,
            "Crop Tycoon",
            "Earn $100,000 from crops",
            "$1,000.00"
        );
        gui.setItem(slot++, lucky);
        
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
            ChatColor.YELLOW + "Total: " + ChatColor.WHITE + "7"
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
