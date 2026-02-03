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
    };

    public SettingsGUI(FarmCrops plugin) {
        this.plugin = plugin;
    }

    public void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_GREEN + "FarmCrops Settings");

        // Fill with glass
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gm = glass.getItemMeta();
        if (gm != null) { gm.setDisplayName(" "); glass.setItemMeta(gm); }
        for (int i = 0; i < 27; i++) gui.setItem(i, glass);

        // Place toggle buttons
        for (int i = 0; i < TOGGLES.length; i++) {
            gui.setItem(i, buildToggleItem(TOGGLES[i][0], TOGGLES[i][1], TOGGLES[i][2]));
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
            lore.add(ChatColor.GREEN + "✓ Admins can toggle features");
            lore.add(ChatColor.GRAY + "without editing config.yml");
            im.setLore(lore);
            infoBtn.setItemMeta(im);
        }
        gui.setItem(4, infoBtn);

        // Close button
        ItemStack closeBtn = new ItemStack(Material.BARRIER);
        ItemMeta cm = closeBtn.getItemMeta();
        if (cm != null) {
            cm.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "CLOSE");
            closeBtn.setItemMeta(cm);
        }
        gui.setItem(8, closeBtn);

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
        if (slot == 8) {
            player.closeInventory();
            playerGUIs.remove(player);
            return;
        }

        // Info button - do nothing
        if (slot == 4) {
            return;
        }

        // Toggle buttons (slots 0 to TOGGLES.length-1)
        if (slot >= 0 && slot < TOGGLES.length) {
            String configPath = TOGGLES[slot][0];
            boolean current = plugin.getConfig().getBoolean(configPath, true);
            boolean newValue = !current;

            // Update in-memory config
            plugin.getConfig().set(configPath, newValue);

            // Save config to disk
            plugin.saveConfig();

            // Refresh the button
            gui.setItem(slot, buildToggleItem(TOGGLES[slot][0], TOGGLES[slot][1], TOGGLES[slot][2]));

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            player.sendMessage(ChatColor.GREEN + TOGGLES[slot][1] + " → " + (newValue ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
            player.sendMessage(ChatColor.GRAY + "(Changed globally for all players)");
        }
    }
}
