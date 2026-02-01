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

    // PDC keys — only WEIGHT and TIER. No more STACK_ID_KEY (it did nothing).
    public static final NamespacedKey WEIGHT_KEY = new NamespacedKey("farmcrops", "weight");
    public static final NamespacedKey TIER_KEY   = new NamespacedKey("farmcrops", "tier");
    public static final NamespacedKey CROP_KEY   = new NamespacedKey("farmcrops", "crop");

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

        // Permission check — if player doesn't have farmcrops.harvest, let vanilla handle it
        Player player = event.getPlayer();
        if (!player.hasPermission("farmcrops.harvest")) return;

        if (!(block.getBlockData() instanceof Ageable)) return;
        Ageable ageable = (Ageable) block.getBlockData();
        if (ageable.getAge() < ageable.getMaximumAge()) {
            return; // Not fully grown, let vanilla handle
        }

        String tier  = rollTier();
        String color = plugin.getConfig().getString("tiers." + tier + ".color", "&7");

        double minWeight = plugin.getConfig().getDouble("weight.min", 0.5);
        double maxWeight = plugin.getConfig().getDouble("weight.max", 10.0);
        double weight = ThreadLocalRandom.current().nextDouble(minWeight, maxWeight);
        weight = Math.round(weight * 100.0) / 100.0;

        // Per-crop pricing with fallback to default
        double basePrice = getCropPrice(block.getType());
        double tierMultiplier = plugin.getConfig().getDouble("tiers." + tier + ".multiplier", 1.0);
        double price = basePrice * tierMultiplier * weight;

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

            meta.setDisplayName(colorize(color) + capitalize(tier) + " " + formatName(block.getType()));
            item.setItemMeta(meta);
        }

        // Cancel vanilla drops entirely — we handle everything manually
        event.setDropItems(false);
        Location dropLoc = block.getLocation().add(0.5, 0.5, 0.5);

        // Drop the custom crop item
        player.getWorld().dropItemNaturally(dropLoc, item);

        // Drop seeds based on per-crop config
        dropSeeds(block.getType(), dropLoc, player.getWorld());

        // Harvest hologram flash
        if (plugin.isHoloEnabled() && plugin.getConfig().getBoolean("holograms.harvest-flash", true)) {
            plugin.getHoloManager().flashHarvest(
                dropLoc, player.getName(), tier, weight, price, formatName(block.getType())
            );
        }

        // Tier-colored particles
        if (plugin.isHoloEnabled() && plugin.getConfig().getBoolean("holograms.particles", true)) {
            spawnHarvestParticles(dropLoc, tier);
        }

        // Record stats
        plugin.getStatsManager().recordHarvest(player, block.getType(), tier, weight, price);

        plugin.getLogger().info("✓ " + player.getName() + " harvested " + tier.toUpperCase()
            + " " + formatName(block.getType()) + " (" + weight + "kg) - Worth: $" + String.format("%.2f", price));
    }

    /**
     * Drop seeds based on per-crop config settings.
     * Each crop has its own chance, min, and max in config.
     */
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
                // Melons only drop seeds via silk touch on the stem — skip
                return;
            default:
                return;
        }

        if (cropKey == null || seedMaterial == null) return;

        String prefix = "seeds." + cropKey + ".";

        // Check if this crop's seed config exists; if not, skip silently
        if (!plugin.getConfig().contains(prefix + "chance")) return;

        int chance = plugin.getConfig().getInt(prefix + "chance", 100);
        int minSeeds = plugin.getConfig().getInt(prefix + "min", 1);
        int maxSeeds = plugin.getConfig().getInt(prefix + "max", 4);

        // Roll the chance
        if (ThreadLocalRandom.current().nextInt(1, 101) > chance) return;

        int amount = ThreadLocalRandom.current().nextInt(minSeeds, maxSeeds + 1);
        ItemStack seeds = new ItemStack(seedMaterial, amount);
        world.dropItemNaturally(location, seeds);
    }

    /**
     * Get the base price for a specific crop type.
     * Falls back to prices.default if no crop-specific price is configured.
     */
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
        if (!plugin.isHoloEnabled() || !plugin.getConfig().getBoolean("holograms.growing-cursor", true)) {
            return;
        }

        Player player = event.getPlayer();

        // Throttle to every 10 ticks (0.5 seconds)
        if (player.getWorld().getGameTime() % 10 != 0) return;

        RayTraceResult result = player.rayTraceBlocks(5.0);

        if (result == null || result.getHitBlock() == null) {
            Block previousBlock = playerLookingAt.remove(player.getUniqueId());
            if (previousBlock != null) {
                plugin.getHoloManager().removeGrowingCropHologram(previousBlock);
            }
            return;
        }

        Block targetBlock = result.getHitBlock();

        if (!isTrackedCrop(targetBlock.getType())) {
            Block previousBlock = playerLookingAt.remove(player.getUniqueId());
            if (previousBlock != null) {
                plugin.getHoloManager().removeGrowingCropHologram(previousBlock);
            }
            return;
        }

        Block previousBlock = playerLookingAt.get(player.getUniqueId());
        if (previousBlock != null && previousBlock.equals(targetBlock)) {
            return; // Same block, no update needed
        }

        if (previousBlock != null) {
            plugin.getHoloManager().removeGrowingCropHologram(previousBlock);
        }

        if (targetBlock.getBlockData() instanceof Ageable) {
            Ageable ageable = (Ageable) targetBlock.getBlockData();
            playerLookingAt.put(player.getUniqueId(), targetBlock);
            plugin.getHoloManager().showGrowingCropHologram(
                targetBlock, targetBlock.getType(), ageable.getAge(), ageable.getMaximumAge()
            );
        }
    }

    // ─────────────────────────────────────────────
    // Utility methods
    // ─────────────────────────────────────────────

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
