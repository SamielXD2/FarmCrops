package player.farmcrops;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * v0.9.6 - FIXED: Melons now work properly!
 * - Added special handling for melons (they're blocks, not Ageable crops)
 * - Auto-sell permission + halal-safe sounds
 */
public class CropListener implements Listener {

    private final FarmCrops plugin;

    // PDC keys
    public static final NamespacedKey WEIGHT_KEY = new NamespacedKey("farmcrops", "weight");
    public static final NamespacedKey TIER_KEY   = new NamespacedKey("farmcrops", "tier");
    public static final NamespacedKey CROP_KEY   = new NamespacedKey("farmcrops", "crop");

    private static final Material[] TRACKED_CROPS = {
            Material.WHEAT, Material.CARROTS, Material.POTATOES,
            Material.BEETROOTS, Material.MELON
    };

    public CropListener(FarmCrops plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (!isTrackedCrop(block.getType())) return;

        Player player = event.getPlayer();
        
        // Check if custom crops are enabled
        if (!plugin.getConfig().getBoolean("custom-crops.enabled", true)) {
            return;
        }
        
        if (!player.hasPermission("farmcrops.harvest")) return;

        // ========================================
        // MELON HANDLING (Special case - melons are blocks, not Ageable!)
        // ========================================
        if (block.getType() == Material.MELON) {
            try {
                // Melons don't have age - they're just blocks that spawn from stems
                // Process them directly
                processCropHarvest(player, block, event);
            } catch (Exception e) {
                plugin.getLogger().severe("Error processing melon harvest for " + player.getName() + ": " + e.getMessage());
                player.sendMessage(ChatColor.RED + "✗ Error harvesting melon!");
                player.sendMessage(ChatColor.GRAY + "Make sure you have the 'farmcrops.harvest' permission.");
                e.printStackTrace();
            }
            return;
        }

        // ========================================
        // REGULAR CROPS (Wheat, Carrots, Potatoes, Beetroots)
        // ========================================
        if (!(block.getBlockData() instanceof Ageable)) return;
        Ageable ageable = (Ageable) block.getBlockData();
        if (ageable.getAge() < ageable.getMaximumAge()) {
            return;
        }

        processCropHarvest(player, block, event);
    }

    /**
     * Process the harvest for any tracked crop (including melons!)
     */
    private void processCropHarvest(Player player, Block block, BlockBreakEvent event) {
        try {
            // Get player preferences
            PlayerSettings.PlayerPreferences prefs = plugin.getPlayerSettings()
                .getPreferences(player.getUniqueId());

            // ========================================
            // CROP STATS CALCULATION
            // ========================================
            String tier;
            double weight;
            double price;

            // Roll fresh stats for this crop
            tier  = rollTier();

            double minWeight = plugin.getConfig().getDouble("weight.min", 0.5);
            double maxWeight = plugin.getConfig().getDouble("weight.max", 10.0);
            weight = ThreadLocalRandom.current().nextDouble(minWeight, maxWeight);
            weight = Math.round(weight * 100.0) / 100.0;

            double basePrice      = getCropPrice(block.getType());
            double tierMultiplier = plugin.getConfig().getDouble("tiers." + tier + ".multiplier", 1.0);
            price  = basePrice * tierMultiplier * weight;

            String color = plugin.getConfig().getString("tiers." + tier + ".color", "&7");

            // Cancel vanilla drops
            event.setDropItems(false);
            Location dropLoc = block.getLocation().add(0.5, 0.5, 0.5);

            // ========================================
            // v0.9.5 NEW: AUTO-SELL WITH PERMISSION
            // ========================================
            if (prefs.autoSell && player.hasPermission("farmcrops.autosell.use")) {
                try {
                    // Give money directly
                    plugin.getEconomy().depositPlayer(player, price);
                    
                    // Play sound if enabled
                    if (prefs.playSounds) {
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
                    }
                    
                    // Show message if enabled
                    if (prefs.showHarvestMessages) {
                        player.sendMessage(colorize(color) + "+" + weight + "kg " + formatName(block.getType()) + 
                            ChatColor.GREEN + " → " + ChatColor.GOLD + "+$" + String.format("%.2f", price));
                    }
                    
                    // NEW v1.0.0: Action Bar notification
                    if (plugin.getActionBarManager() != null) {
                        plugin.getActionBarManager().sendHarvestNotification(
                            player, formatName(block.getType()), tier, weight, price
                        );
                    }
                    
                    // Record stats
                    plugin.getStatsManager().recordHarvest(player, block.getType(), tier, weight, price);
                    
                    // NEW v1.0.0: Update scoreboard
                    if (plugin.getScoreboardManager() != null) {
                        plugin.getScoreboardManager().recordHarvest(player, price);
                    }
                    
                } catch (Exception e) {
                    plugin.getLogger().warning("Error during auto-sell for " + player.getName() + ": " + e.getMessage());
                    player.sendMessage(ChatColor.RED + "✗ Auto-sell failed! Check console for details.");
                }
                
            } else {
                try {
                    // Drop item normally
                    Material dropMat = getDropMaterial(block.getType());
                    ItemStack item = new ItemStack(dropMat, 1);
                    ItemMeta meta = item.getItemMeta();

                    if (meta != null) {
                        PersistentDataContainer pdc = meta.getPersistentDataContainer();
                        pdc.set(WEIGHT_KEY, PersistentDataType.DOUBLE, weight);
                        pdc.set(TIER_KEY, PersistentDataType.STRING, tier);
                        pdc.set(CROP_KEY, PersistentDataType.STRING, block.getType().name());

                        List<String> lore = new ArrayList<>();
                        lore.add(colorize(color) + "Tier: " + capitalize(tier));
                        lore.add(colorize("&7Weight: &f" + weight + " kg"));
                        lore.add(colorize("&7Price: &a$" + String.format("%.2f", price)));
                        meta.setLore(lore);

                        // Apply tier color to the ENTIRE name (tier + crop name)
                        String fullName = capitalize(tier) + " " + formatName(block.getType());
                        meta.setDisplayName(colorize(color) + fullName);
                        item.setItemMeta(meta);
                    }

                    player.getWorld().dropItemNaturally(dropLoc, item);
                    
                    // NEW v1.0.0: Action Bar notification
                    if (plugin.getActionBarManager() != null) {
                        plugin.getActionBarManager().sendHarvestNotification(
                            player, formatName(block.getType()), tier, weight, price
                        );
                    }
                    
                    // Record stats
                    plugin.getStatsManager().recordHarvest(player, block.getType(), tier, weight, price);
                    
                    // NEW v1.0.0: Update scoreboard
                    if (plugin.getScoreboardManager() != null) {
                        plugin.getScoreboardManager().recordHarvest(player, price);
                    }
                    
                } catch (Exception e) {
                    plugin.getLogger().warning("Error creating crop item for " + player.getName() + ": " + e.getMessage());
                    player.sendMessage(ChatColor.RED + "✗ Failed to create crop item! Check console.");
                }
            }

            // v1.0.0 Features - Daily Tasks, Collections, Achievements (always run regardless of autosell)
            try {
                if (plugin.getDailyTaskManager() != null) {
                    plugin.getDailyTaskManager().onCropHarvest(player, block.getType());
                }
                
                if (plugin.getCollectionManager() != null) {
                    plugin.getCollectionManager().addCropToCollection(player, block.getType());
                }
                
                if (plugin.getAchievementManager() != null) {
                    plugin.getAchievementManager().checkAchievements(player);
                }
            } catch (Exception e) {
                // Log but don't interrupt harvest
                plugin.getLogger().warning("Error updating premium features: " + e.getMessage());
            }

            // Drop seeds (melons don't drop seeds)
            if (block.getType() != Material.MELON) {
                try {
                    dropSeeds(block.getType(), dropLoc, player.getWorld());
                } catch (Exception e) {
                    plugin.getLogger().warning("Error dropping seeds: " + e.getMessage());
                }
            }

            // Harvest hologram flash
            try {
                if (prefs.showHolograms && 
                    plugin.isHoloEnabled() && 
                    plugin.getConfig().getBoolean("holograms.harvest-flash", true)) {
                    
                    plugin.getHoloManager().flashHarvest(
                        dropLoc, player.getName(), tier, weight, price, formatName(block.getType())
                    );
                }
            } catch (Exception e) {
                // Holograms are optional, just log the error
                plugin.getLogger().fine("Hologram error: " + e.getMessage());
            }

            // Particles (check player + server settings)
            try {
                if (prefs.showParticles && 
                    plugin.getConfig().getBoolean("holograms.particles", true)) {
                    
                    spawnHarvestParticles(dropLoc, tier);
                }
            } catch (Exception e) {
                // Particles are optional, just log the error
                plugin.getLogger().fine("Particle error: " + e.getMessage());
            }
            
            // Sound (if not auto-sell or no permission, and player has sounds enabled)
            try {
                if ((!prefs.autoSell || !player.hasPermission("farmcrops.autosell.use")) && prefs.playSounds) {
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
                }
            } catch (Exception e) {
                // Sounds are optional
                plugin.getLogger().fine("Sound error: " + e.getMessage());
            }
            
        } catch (Exception e) {
            // Critical error in harvest processing
            plugin.getLogger().severe("═══════════════════════════════════════");
            plugin.getLogger().severe("CRITICAL ERROR in processCropHarvest!");
            plugin.getLogger().severe("Player: " + player.getName());
            plugin.getLogger().severe("Crop: " + block.getType());
            plugin.getLogger().severe("Error: " + e.getMessage());
            plugin.getLogger().severe("═══════════════════════════════════════");
            e.printStackTrace();
            
            player.sendMessage(ChatColor.RED + "✗ An error occurred while harvesting!");
            player.sendMessage(ChatColor.GRAY + "Please contact an administrator.");
        }
    }

    private void dropSeeds(Material cropType, Location location, World world) {
        if (!plugin.getConfig().getBoolean("seeds.enabled", true)) return;

        String cropKey = null;
        Material seedMaterial = null;

        switch (cropType) {
            case WHEAT:
                cropKey = "wheat";
                seedMaterial = Material.WHEAT_SEEDS;
                break;
            case CARROTS:
                cropKey = "carrot";
                seedMaterial = Material.CARROT;
                break;
            case POTATOES:
                cropKey = "potato";
                seedMaterial = Material.POTATO;
                break;
            case BEETROOTS:
                cropKey = "beetroot";
                seedMaterial = Material.BEETROOT_SEEDS;
                break;
            case MELON:
                // Melons don't drop seeds from the melon block itself
                return;
            default:
                return;
        }

        if (cropKey == null || seedMaterial == null) return;

        String prefix = "seeds." + cropKey + ".";
        if (!plugin.getConfig().contains(prefix + "chance")) return;

        int chance = plugin.getConfig().getInt(prefix + "chance", 100);
        int minSeeds = plugin.getConfig().getInt(prefix + "min", 1);
        int maxSeeds = plugin.getConfig().getInt(prefix + "max", 4);

        if (ThreadLocalRandom.current().nextInt(1, 101) > chance) return;

        int amount = ThreadLocalRandom.current().nextInt(minSeeds, maxSeeds + 1);
        ItemStack seeds = new ItemStack(seedMaterial, amount);
        world.dropItemNaturally(location, seeds);
    }

    public double getCropPrice(Material cropType) {
        String cropKey = null;
        switch (cropType) {
            case WHEAT:      cropKey = "wheat";    break;
            case CARROTS:    cropKey = "carrot";   break;
            case POTATOES:   cropKey = "potato";   break;
            case BEETROOTS:  cropKey = "beetroot"; break;
            case MELON:      cropKey = "melon";    break;
            default: break;
        }

        if (cropKey != null && plugin.getConfig().contains("prices." + cropKey)) {
            return plugin.getConfig().getDouble("prices." + cropKey);
        }
        return plugin.getConfig().getDouble("prices.default", 10.0);
    }

    private void spawnHarvestParticles(Location location, String tier) {
        World world = location.getWorld();
        if (world == null) return;

        int particleAmount = plugin.getConfig().getInt("holograms.particle-amount", 10);
        Particle particleType;
        Object particleData = null;

        switch (tier.toLowerCase()) {
            case "common":
                particleType = Particle.SMOKE_NORMAL;
                break;
            case "rare":
                particleType = Particle.REDSTONE;
                particleData = new Particle.DustOptions(Color.AQUA, 1.5f);
                break;
            case "epic":
                particleType = Particle.REDSTONE;
                particleData = new Particle.DustOptions(Color.PURPLE, 1.5f);
                break;
            case "legendary":
                particleType = Particle.REDSTONE;
                particleData = new Particle.DustOptions(Color.ORANGE, 2.0f);
                break;
            case "mythic":
                particleType = Particle.REDSTONE;
                particleData = new Particle.DustOptions(Color.RED, 2.5f);
                break;
            default:
                particleType = Particle.SMOKE_NORMAL;
                break;
        }

        Location particleLoc = location.clone().add(0.5, 0.5, 0.5);
        if (particleData != null) {
            world.spawnParticle(particleType, particleLoc, particleAmount, 0.3, 0.3, 0.3, 0.1, particleData);
        } else {
            world.spawnParticle(particleType, particleLoc, particleAmount, 0.3, 0.3, 0.3, 0.1);
        }
    }

    private String rollTier() {
        int roll = ThreadLocalRandom.current().nextInt(1, 101);
        int common = plugin.getConfig().getInt("tiers.common.chance", 70);
        int rare   = plugin.getConfig().getInt("tiers.rare.chance", 19);
        int epic   = plugin.getConfig().getInt("tiers.epic.chance", 7);
        int legendary = plugin.getConfig().getInt("tiers.legendary.chance", 3);

        if (roll <= common)                                        return "common";
        else if (roll <= common + rare)                            return "rare";
        else if (roll <= common + rare + epic)                     return "epic";
        else if (roll <= common + rare + epic + legendary)         return "legendary";
        else                                                       return "mythic";
    }

    private boolean isTrackedCrop(Material m) {
        for (Material t : TRACKED_CROPS) { if (t == m) return true; }
        return false;
    }

    private Material getDropMaterial(Material crop) {
        switch (crop) {
            case WHEAT:     return Material.WHEAT;
            case CARROTS:   return Material.CARROT;
            case POTATOES:  return Material.POTATO;
            case BEETROOTS: return Material.BEETROOT;
            case MELON:     return Material.MELON_SLICE;
            default:        return crop;
        }
    }

    private String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    static String formatName(Material m) {
        String name = m.name();
        if (name.endsWith("S")) {
            name = name.substring(0, name.length() - 1);
        }
        return name.charAt(0) + name.substring(1).toLowerCase().replace("_", " ");
    }

    private String colorize(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
