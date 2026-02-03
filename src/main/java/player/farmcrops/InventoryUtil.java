package player.farmcrops;

import org.bukkit.inventory.InventoryView;

/**
 * Compatibility utility for handling InventoryView title access across different Minecraft versions.
 * 
 * - Spigot/Bukkit 1.20.x and earlier: Uses getTitle()
 * - Paper 1.21+: Uses title() which returns Adventure Component
 * 
 * This class provides a unified method that works on both.
 */
public class InventoryUtil {
    
    /**
     * Gets the inventory title as a plain string, compatible with both old and new APIs.
     * 
     * @param view The InventoryView to get the title from
     * @return The title as a plain string
     */
    public static String getTitle(InventoryView view) {
        // Try the new Paper 1.21+ method first (title() returning Component)
        try {
            Object titleComponent = view.getClass().getMethod("title").invoke(view);
            
            // Check if net.kyori.adventure is available (Paper)
            try {
                Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
                Class<?> serializerClass = Class.forName("net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer");
                
                Object serializer = serializerClass.getMethod("plainText").invoke(null);
                String title = (String) serializerClass.getMethod("serialize", componentClass).invoke(serializer, titleComponent);
                return title;
                
            } catch (ClassNotFoundException e) {
                // Adventure not available, fall through to legacy method
            }
            
        } catch (NoSuchMethodException e) {
            // title() method doesn't exist, fall through to legacy method
        } catch (Exception e) {
            // Any other reflection error, fall through to legacy method
            e.printStackTrace();
        }
        
        // Fallback to legacy getTitle() method (Spigot/Bukkit)
        try {
            @SuppressWarnings("deprecation")
            String title = view.getTitle();
            return title;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
