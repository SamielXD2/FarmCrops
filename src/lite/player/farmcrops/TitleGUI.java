package player.farmcrops;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

/**
 * LITE EDITION STUB
 */
public class TitleGUI implements Listener {
    
    public TitleGUI(FarmCrops plugin) {
        // Lite edition - no functionality
    }
    
    public void openGUI(Player player) {
        player.sendMessage("§c✗ Titles are a Premium feature!");
        player.sendMessage("§eUpgrade to Premium to unlock titles!");
    }
}
