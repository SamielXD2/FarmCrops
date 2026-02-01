package player.farmcrops;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class CropListener implements Listener {

    private final FarmCrops plugin;
    
    public static final NamespacedKey WEIGHT_KEY = new NamespacedKey("farmcrops", "weight");
    public static final NamespacedKey TIER_KEY   = new NamespacedKey("farmcrops", "tier");
    public static final NamespacedKey STACK_ID_KEY = new NamespacedKey("farmcrops", "stackid");
    
    private static final Material[] TRACKED_CROPS = {
            Material.WHEAT, Material.CARROTS, Material.POTATOES,
            Material.BEETROOTS, Material.MELON
    };
    
    // Track which block each player is looking at to avoid hologram spam
    private final Map<UUID, Block> playerLookingAt = new HashMap<>();

    public CropListener(FarmCrops plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        
        if (!isTrackedCrop(block.getType())) return;
        
        if (!(block.getBlockData() instanceof Ageable)) return;
        Ageable ageable = (Ageable) block.getBlockData();
        if (ageable.getAge() < ageable.getMaximumAge()) {
            return;
        }

        Player player = event.getPlayer();
        
        double minWeight = plugin.getConfig().getDouble("weight.min", 0.5);
        double maxWeight = plugin.getConfig().getDouble("weight.max", 10.0);
        
        String tier  = rollTier();
        String color = plugin.getConfig().getString("tiers." + tier + ".color", "&7");
        
        double weight = ThreadLocalRandom.current().nextDouble(minWeight, maxWeight);
        weight = Math.round(weight * 100.0) / 100.0;
        
        // Calculate price
        double basePrice = plugin.getConfig().getDouble("prices.default", 10.0);
        double tierMultiplier = plugin.getConfig().getDouble("tiers." + tier + ".multiplier", 1.0);
        double price = basePrice * tierMultiplier * weight;
        
        ItemStack item = new ItemStack(getDropMaterial(block.getType()), 1);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(WEIGHT_KEY, PersistentDataType.DOUBLE, weight);
            pdc.set(TIER_KEY, PersistentDataType.STRING, tier);
            
            // Generate unique stack ID for stacking system
            // Items with same tier will have same stack ID -> they will stack
            boolean combineTimers = plugin.getConfig().getBoolean("stacking.combine-tiers", true);
            if (combineTimers) {
                // Same tier = same stack ID = items stack together
                pdc.set(STACK_ID_KEY, PersistentDataType.STRING, tier);
            } else {
                // Unique stack ID = items don't stack
                pdc.set(STACK_ID_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());
            }
            
            List<String> lore = new ArrayList<>();
            lore.add(colorize(color) + "Tier: " + capitalize(tier));
            lore.add(colorize("&7Weight: &f" + weight + " kg"));
            lore.add(colorize("&7Price: &a$" + String.format("%.2f", price)));
            meta.setLore(lore);
            
            meta.setDisplayName(colorize(color) + capitalize(tier) + " " + formatName(block.getType()));
            item.setItemMeta(meta);
        }
        
        event.setDropItems(false);
        Location dropLoc = block.getLocation().add(0.5, 0.5, 0.5);
        
        // Drop the custom crop
        player.getWorld().dropItemNaturally(dropLoc, item);
        
        // Drop seeds for crops that have seeds
        dropSeeds(block.getType(), dropLoc, player.getWorld());
        
        // Show harvest hologram if enabled
        if (plugin.isHoloEnabled() && plugin.getConfig().getBoolean("holograms.harvest-flash", true)) {
            plugin.getHoloManager().flashHarvest(
                dropLoc, 
                player.getName(), 
                tier, 
                weight,
                price,
                formatName(block.getType())
            );
        }
        
        // Spawn particles if enabled
        if (plugin.isHoloEnabled() && plugin.getConfig().getBoolean("holograms.particles", true)) {
            spawnHarvestParticles(dropLoc, tier);
        }
        
        // Update player stats
        plugin.getStatsManager().recordHarvest(player, tier, weight, price);
        
        plugin.getLogger().info("✓ " + player.getName() + " harvested " + tier.toUpperCase()
            + " " + formatName(block.getType()) + " (" + weight + "kg) - Worth: $" + String.format("%.2f", price));
    }
    
    /**
     * Drop seeds for crops that produce seeds when broken
     */
    private void dropSeeds(Material cropType, Location location, World world) {
        Material seedType = null;
        int minSeeds = 1;
        int maxSeeds = 4;
        
        switch (cropType) {
            case WHEAT:
                seedType = Material.WHEAT_SEEDS;
                break;
            case CARROTS:
                seedType = Material.CARROT;
                minSeeds = 1;
                maxSeeds = 3;
                break;
            case POTATOES:
                seedType = Material.POTATO;
                minSeeds = 1;
                maxSeeds = 4;
                break;
            case BEETROOTS:
                seedType = Material.BEETROOT_SEEDS;
                break;
            case MELON:
                // Melons don't drop seeds naturally, only when using silk touch on stem
                // So we don't drop seeds here
                return;
            default:
                return;
        }
        
        if (seedType != null) {
            int seedAmount = ThreadLocalRandom.current().nextInt(minSeeds, maxSeeds + 1);
            ItemStack seeds = new ItemStack(seedType, seedAmount);
            world.dropItemNaturally(location, seeds);
        }
    }
    
    /**
     * Spawn particle effects matching the tier color
     */
    private void spawnHarvestParticles(Location location, String tier) {
        World world = location.getWorld();
        if (world == null) return;
        
        int particleAmount = plugin.getConfig().getInt("holograms.particle-amount", 10);
        
        Particle particleType;
        Object particleData = null;
        
        switch (tier.toLowerCase()) {
            case "common":
                particleType = Particle.SMOKE;
                break;
            case "rare":
                particleType = Particle.DUST;
                particleData = new Particle.DustOptions(Color.AQUA, 1.5f);
                break;
            case "epic":
                particleType = Particle.DUST;
                particleData = new Particle.DustOptions(Color.PURPLE, 1.5f);
                break;
            case "legendary":
                particleType = Particle.DUST;
                particleData = new Particle.DustOptions(Color.ORANGE, 2.0f);
                break;
            default:
                particleType = Particle.SMOKE;
                break;
        }
        
        Location particleLoc = location.clone().add(0.5, 0.5, 0.5);
        
        if (particleData != null) {
            world.spawnParticle(particleType, particleLoc, particleAmount, 0.3, 0.3, 0.3, 0.1, particleData);
        } else {
            world.spawnParticle(particleType, particleLoc, particleAmount, 0.3, 0.3, 0.3, 0.1);
        }
    }
    
    /**
     * Show hologram when player looks at a growing crop
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only check if hologram feature is enabled
        if (!plugin.isHoloEnabled() || !plugin.getConfig().getBoolean("holograms.growing-cursor", true)) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Only check every 10 ticks (0.5 seconds) to reduce lag
        if (player.getWorld().getGameTime() % 10 != 0) {
            return;
        }
        
        // Ray trace to find block player is looking at
        RayTraceResult result = player.rayTraceBlocks(5.0);
        
        if (result == null || result.getHitBlock() == null) {
            // Player not looking at a block - remove hologram if exists
            Block previousBlock = playerLookingAt.remove(player.getUniqueId());
            if (previousBlock != null && plugin.isHoloEnabled()) {
                plugin.getHoloManager().removeGrowingCropHologram(previousBlock);
            }
            return;
        }
        
        Block targetBlock = result.getHitBlock();
        
        // Check if it's a tracked crop
        if (!isTrackedCrop(targetBlock.getType())) {
            // Not a crop - remove hologram if exists
            Block previousBlock = playerLookingAt.remove(player.getUniqueId());
            if (previousBlock != null && plugin.isHoloEnabled()) {
                plugin.getHoloManager().removeGrowingCropHologram(previousBlock);
            }
            return;
        }
        
        // Check if it's the same block they were looking at
        Block previousBlock = playerLookingAt.get(player.getUniqueId());
        if (previousBlock != null && previousBlock.equals(targetBlock)) {
            // Still looking at same block, don't recreate hologram
            return;
        }
        
        // Remove old hologram if exists
        if (previousBlock != null) {
            plugin.getHoloManager().removeGrowingCropHologram(previousBlock);
        }
        
        // Show hologram for new block
        if (targetBlock.getBlockData() instanceof Ageable) {
            Ageable ageable = (Ageable) targetBlock.getBlockData();
            playerLookingAt.put(player.getUniqueId(), targetBlock);
            plugin.getHoloManager().showGrowingCropHologram(targetBlock, targetBlock.getType(), ageable.getAge(), ageable.getMaximumAge());
        }
    }

    private String rollTier() {
        int roll = ThreadLocalRandom.current().nextInt(1, 101);
        int common = plugin.getConfig().getInt("tiers.common.chance", 70);
        int rare   = plugin.getConfig().getInt("tiers.rare.chance", 20);
        int epic   = plugin.getConfig().getInt("tiers.epic.chance", 7);
        
        if (roll <= common)                    return "common";
        else if (roll <= common + rare)        return "rare";
        else if (roll <= common + rare + epic) return "epic";
        else                                   return "legendary";
    }
    
    private boolean isTrackedCrop(Material m) {
        for (Material t : TRACKED_CROPS) { if (t == m) return true; }
        return false;
    }
    
    private Material getDropMaterial(Material crop) {
        switch (crop) {
            case WHEAT: return Material.WHEAT;
            case CARROTS: return Material.CARROT;
            case POTATOES: return Material.POTATO;
            case BEETROOTS: return Material.BEETROOT;
            case MELON: return Material.MELON_SLICE;
            default: return crop;
        }
    }
    
    private String capitalize(String s) { 
        return s.substring(0,1).toUpperCase() + s.substring(1); 
    }
    
    private String formatName(Material m) { 
        String name = m.name();
        if (name.endsWith("S")) {
            // Remove trailing S for plural crop names (CARROTS -> CARROT)
            name = name.substring(0, name.length() - 1);
        }
        return name.charAt(0) + name.substring(1).toLowerCase().replace("_"," "); 
    }
    
    private String colorize(String s) { 
        return ChatColor.translateAlternateColorCodes('&', s); 
    }
}
