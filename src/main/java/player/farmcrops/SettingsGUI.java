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
 * /farmsettings — Admin GUI to toggle plugin features without editing config.
 * Protected by farmcrops.settings permission.
 * 
 * IMPORTANT: These settings are GLOBAL and affect ALL PLAYERS on the server!
 * This is NOT a per-player settings menu.
 *
 * Layout (27-slot inventory):
 *   Slot 0: Holograms toggle
 *   Slot 1: Particles toggle
 *   Slot 2: Growing Cursor toggle
 *   Slot 3: Seed Drops toggle
 *   Slot 4: Info button
 *   Slot 8: Close button
 */
public class SettingsGUI implements Listener {

    private final FarmCrops plugin;
    private final Map<Player, Inventory> playerGUIs = new HashMap<>();

    // Each toggle maps to a config path
    // Format: { config_path, display_name, description }
    private static final String[][] TOGGLES = {
        { "holograms.harvest-flash",  "Harvest Hologram",  "Show hologram on harvest" },
        { "holograms.growing-cursor", "Growing Cursor",    "Hologram when looking at crops" },
        { "holograms.particles",      "Particles",         "Particle effects on harvest" },
        { "seeds.enabled",            "Seed Drops",        "Drop seeds when harvesting" },
        { "auto-save.enabled",        "Auto-Save",         "Automatic data saving" },
        { "scoreboard.enabled",       "Scoreboard",        "Enable scoreboard system" },
        { "action-bar.enabled",       "Action Bar",        "Show action bar messages" },
        { "chat-messages.enabled",    "Chat Messages",     "Harvest messages in chat" },
    };

    public SettingsGUI(FarmCrops plugin) {
        this.plugin = plugin;
    }

    public void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 45, ChatColor.DARK_GREEN + "FarmCrops Settings");

        // Fill with glass
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gm = glass.getItemMeta();
        if (gm != null) { gm.setDisplayName(" "); glass.setItemMeta(gm); }
        for (int i = 0; i < 45; i++) gui.setItem(i, glass);

        // Place toggle buttons in a nice grid layout
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        for (int i = 0; i < TOGGLES.length && i < slots.length; i++) {
            gui.setItem(slots[i], buildToggleItem(TOGGLES[i][0], TOGGLES[i][1], TOGGLES[i][2]));
        }

        // Info button
        ItemStack infoBtn = new ItemStack(Material.BOOK);
        ItemMeta im = infoBtn.getItemMeta();
        if (im != null) {
            im.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "INFO");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "These settings are GLOBAL");
            lore.add(ChatColor.GRAY + "Changes affect ALL players");
            lore.add("");
            lore.add(ChatColor.GREEN + "✓ Toggle features on/off");
            lore.add(ChatColor.GRAY + "without editing config.yml");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Use /farmreload to");
            lore.add(ChatColor.YELLOW + "apply other config changes");
            im.setLore(lore);
            infoBtn.setItemMeta(im);
        }
        gui.setItem(4, infoBtn);
        
        // Reload button
        ItemStack reloadBtn = new ItemStack(Material.RECOVERY_COMPASS);
        ItemMeta rm = reloadBtn.getItemMeta();
        if (rm != null) {
            rm.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "RELOAD");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Reload plugin configuration");
            lore.add(ChatColor.GRAY + "and refresh all systems");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to reload");
            rm.setLore(lore);
            reloadBtn.setItemMeta(rm);
        }
        gui.setItem(40, reloadBtn);

        // Close button
        ItemStack closeBtn = new ItemStack(Material.BARRIER);
        ItemMeta cm = closeBtn.getItemMeta();
        if (cm != null) {
            cm.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "CLOSE");
            closeBtn.setItemMeta(cm);
        }
        gui.setItem(44, closeBtn);

        playerGUIs.put(player, gui);
        player.openInventory(gui);
    }

    private ItemStack buildToggleItem(String configPath, String label, String description) {
        boolean enabled = plugin.getConfig().getBoolean(configPath, true);
        Material mat = enabled ? Material.LIME_STAINED_GLASS : Material.RED_STAINED_GLASS;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName((enabled ? ChatColor.GREEN : ChatColor.RED) + label
                + " — " + (enabled ? "ON" : "OFF"));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + description);
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to toggle");
            lore.add(ChatColor.DARK_GRAY + "Affects all players globally");
            meta.setLore(lore);
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
        if (!title.equals(ChatColor.DARK_GREEN + "FarmCrops Settings")) return;

        event.setCancelled(true);

        Inventory gui = playerGUIs.get(player);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(gui)) return;

        int slot = event.getSlot();

        // Close button
        if (slot == 44) {
            player.closeInventory();
            playerGUIs.remove(player);
            return;
        }

        // Info button - do nothing
        if (slot == 4) {
            return;
        }
        
        // Reload button
        if (slot == 40) {
            player.closeInventory();
            playerGUIs.remove(player);
            player.performCommand("farmreload");
            return;
        }

        // Toggle buttons
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        for (int i = 0; i < slots.length && i < TOGGLES.length; i++) {
            if (slot == slots[i]) {
                String configPath = TOGGLES[i][0];
                boolean current = plugin.getConfig().getBoolean(configPath, true);
                boolean newValue = !current;

                // Update in-memory config
                plugin.getConfig().set(configPath, newValue);

                // Save config to disk
                plugin.saveConfig();

                // Refresh the button
                gui.setItem(slot, buildToggleItem(TOGGLES[i][0], TOGGLES[i][1], TOGGLES[i][2]));

                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                player.sendMessage(ChatColor.GREEN + TOGGLES[i][1] + " → " + (newValue ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
                player.sendMessage(ChatColor.GRAY + "(Changed globally for all players)");
                break;
            }
        }
    }
}
