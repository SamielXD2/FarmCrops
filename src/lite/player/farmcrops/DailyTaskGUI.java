package player.farmcrops;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

/**
 * LITE EDITION STUB - Daily Task GUI
 */
public class DailyTaskGUI implements Listener {
    
    private final FarmCrops plugin;
    
    public DailyTaskGUI(FarmCrops plugin) {
        this.plugin = plugin;
    }
    
    public void openGUI(Player player) {
        player.sendMessage("§c✗ Daily Tasks is a Premium feature!");
    }
    
    public void openDailyTasksGUI(Player player) {
        player.sendMessage("§c✗ Daily Tasks is a Premium feature!");
        player.sendMessage("§eUpgrade to Premium to unlock daily tasks!");
    }
}
