package player.farmcrops;

import org.bukkit.inventory.InventoryView;

/**
 * Utility class for inventory operations
 */
public class InventoryUtil {
    
    /**
     * Gets the title from an InventoryView in a version-compatible way
     * @param view The InventoryView to get the title from
     * @return The inventory title as a String
     */
    public static String getTitle(InventoryView view) {
        // Use getTitle() which works across different Bukkit versions
        return view.getTitle();
    }
}
