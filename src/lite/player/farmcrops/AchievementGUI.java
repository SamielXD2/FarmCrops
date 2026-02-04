package player.farmcrops;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

/**
 * LITE EDITION STUB - This class does nothing
 * Upgrade to Premium for full functionality
 */
public class AchievementGUI implements Listener {
    
    public AchievementGUI(FarmCrops plugin) {
        // Lite edition - no functionality
    }
    
    public void openGUI(Player player, int page) {
        player.sendMessage("§c✗ Achievements are a Premium feature!");
        player.sendMessage("§eUpgrade to Premium to unlock achievements, titles, and more!");
    }
}
