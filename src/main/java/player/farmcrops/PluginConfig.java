package player.farmcrops;

/**
 * Configuration class that handles edition-specific features.
 * These values are replaced at build time by Maven based on the active profile.
 */
public class PluginConfig {
    
    // Edition information
    public static final String EDITION = "${plugin.edition}";
    public static final String VERSION = "${plugin.version}";
    
    // Feature flags - replaced at build time
    public static final boolean ACHIEVEMENTS_ENABLED = Boolean.parseBoolean("${enable.achievements}");
    public static final boolean DAILY_TASKS_ENABLED = Boolean.parseBoolean("${enable.dailytasks}");
    public static final boolean COLLECTIONS_ENABLED = Boolean.parseBoolean("${enable.collections}");
    public static final boolean AUTOSAVE_ENABLED = Boolean.parseBoolean("${enable.autosave}");
    
    // Check if this is Premium edition
    public static boolean isPremium() {
        return "Premium".equals(EDITION);
    }
    
    // Check if this is Lite edition
    public static boolean isLite() {
        return "Lite".equals(EDITION);
    }
    
    /**
     * Get a feature-locked message for Lite users
     */
    public static String getLockedMessage(String featureName) {
        return "§c§l" + featureName + " is a Premium-only feature! §7Upgrade to unlock.";
    }
}
