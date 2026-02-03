package player.farmcrops;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.player.Action;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * v0.10.0 — Right-click crop preview with caching.
 *
 * When a player right-clicks a fully-grown crop:
 *   1. A tier + weight is rolled and stored in the cache keyed to that block location.
 *   2. The player sees the preview (hologram if available, otherwise chat).
 *   3. When the block is actually broken, CropListener pulls from the cache
 *      instead of re-rolling — so the harvest matches the preview exactly.
 *   4. Cache entries are removed on harvest OR after a configurable TTL
 *      (default 60 s) so memory never leaks.
 *
 * Permission: farmcrops.preview   (default: true, already in plugin.yml)
 * Config keys already present in config.yml:
 *   holograms.right-click-preview   – master toggle
 *   holograms.preview-duration      – seconds before cache entry expires (default 3 … we treat it as 60 for TTL)
 */
public class CropPreviewManager implements Listener {

    private final FarmCrops plugin;

    // ── cached previews, keyed by block location ──
    // Accessible package-private so CropListener can pull & remove entries.
    final Map<Location, PreviewData> cache = new ConcurrentHashMap<>();

    // How long (ms) a preview stays cached before auto-expiry.
    // We read "holograms.preview-duration" from config as seconds.
    private long ttlMillis;

    public CropPreviewManager(FarmCrops plugin) {
        this.plugin = plugin;
        reloadTTL();
        startCleanupTask();
    }

    /** Re-read TTL from live config (called on /farmreload). */
    public void reloadTTL() {
        int seconds = plugin.getConfig().getInt("holograms.preview-cache-ttl", 60);
        ttlMillis = (long) seconds * 1000L;
    }

    // ─────────────────────────────────────────────
    // Event: right-click on a crop block
    // ─────────────────────────────────────────────

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only care about right-click on a block
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getBlockFace() == null) return;

        Block block = event.getBlock();
        if (block == null) return;

        // Master toggle
        if (!plugin.getConfig().getBoolean("holograms.right-click-preview", true)) return;

        // Must be a tracked crop
        if (!isTrackedCrop(block.getType())) return;

        Player player = event.getPlayer();

        // Permission gate
        if (!player.hasPermission("farmcrops.preview")) return;

        // Must be fully grown
        if (!(block.getBlockData() instanceof Ageable)) return;
        Ageable ageable = (Ageable) block.getBlockData();
        if (ageable.getAge() < ageable.getMaximumAge()) return;

        // Prevent the interact from also placing a block / eating food
        event.setCancelled(true);

        Location loc = block.getLocation();

        // If there is already a cached preview for this block, just show it again (no re-roll)
        PreviewData existing = cache.get(loc);
        if (existing != null) {
            showPreview(player, block, existing);
            return;
        }

        // Roll a new preview and cache it
        PreviewData preview = rollPreview(block.getType());
        cache.put(loc, preview);

        showPreview(player, block, preview);
    }

    // ─────────────────────────────────────────────
    // Roll
    // ─────────────────────────────────────────────

    private PreviewData rollPreview(Material cropType) {
        String tier = rollTier();
        double minW = plugin.getConfig().getDouble("weight.min", 0.5);
        double maxW = plugin.getConfig().getDouble("weight.max", 10.0);
        double weight = Math.round(ThreadLocalRandom.current().nextDouble(minW, maxW) * 100.0) / 100.0;

        double basePrice   = plugin.getCropPrice(cropType);
        double tierMult    = plugin.getConfig().getDouble("tiers." + tier + ".multiplier", 1.0);
        double price       = basePrice * tierMult * weight;

        return new PreviewData(tier, weight, price, System.currentTimeMillis());
    }

    // ─────────────────────────────────────────────
    // Show preview to the player
    // ─────────────────────────────────────────────

    private void showPreview(Player player, Block cropBlock, PreviewData preview) {
        String color  = plugin.getConfig().getString("tiers." + preview.tier + ".color", "&7");
        String colored = ChatColor.translateAlternateColorCodes('&', color);
        String cropName = CropListener.formatName(cropBlock.getType());
        String tierName = preview.tier.substring(0, 1).toUpperCase() + preview.tier.substring(1);

        // ── hologram (if DecentHolograms is live) ──
        if (plugin.isHoloEnabled()) {
            plugin.getHoloManager().flashPreview(
                cropBlock.getLocation(), player, preview.tier, preview.weight, preview.price, cropName
            );
        }

        // ── chat message (always, nice on mobile / when holos are off) ──
        PlayerSettings.PlayerPreferences prefs = plugin.getPlayerSettings()
            .getPreferences(player.getUniqueId());
        if (prefs.showHarvestMessages) {
            player.sendMessage(
                colored + "🌾 " + tierName + " " + cropName +
                ChatColor.GRAY + " — " +
                ChatColor.WHITE + preview.weight + " kg" +
                ChatColor.GRAY + " | " +
                ChatColor.GOLD + "$" + String.format("%.2f", preview.price) +
                ChatColor.GRAY + " (cached)"
            );
        }

        // ── sound ──
        if (prefs.playSounds) {
            player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.6f, 1.2f);
        }
    }

    // ─────────────────────────────────────────────
    // Public API for CropListener
    // ─────────────────────────────────────────────

    /**
     * Pull a cached preview for this location and remove it from the cache.
     * Returns null if nothing is cached (caller should roll normally).
     */
    public PreviewData consumePreview(Location loc) {
        return cache.remove(loc);
    }

    // ─────────────────────────────────────────────
    // Tier roller (duplicated from CropListener so this class is self-contained)
    // ─────────────────────────────────────────────

    private String rollTier() {
        int roll = ThreadLocalRandom.current().nextInt(1, 101);
        int common      = plugin.getConfig().getInt("tiers.common.chance", 70);
        int rare        = plugin.getConfig().getInt("tiers.rare.chance", 19);
        int epic        = plugin.getConfig().getInt("tiers.epic.chance", 7);
        int legendary   = plugin.getConfig().getInt("tiers.legendary.chance", 3);

        if (roll <= common)                                      return "common";
        else if (roll <= common + rare)                          return "rare";
        else if (roll <= common + rare + epic)                   return "epic";
        else if (roll <= common + rare + epic + legendary)       return "legendary";
        else                                                     return "mythic";
    }

    // ─────────────────────────────────────────────
    // Cleanup task — expire old cache entries
    // ─────────────────────────────────────────────

    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                cache.entrySet().removeIf(e -> now - e.getValue().rolledAt > ttlMillis);
            }
        }.runTaskTimer(plugin, 0L, 200L); // every 10 seconds
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private boolean isTrackedCrop(Material m) {
        switch (m) {
            case WHEAT: case CARROTS: case POTATOES: case BEETROOTS: case MELON:
                return true;
            default:
                return false;
        }
    }

    // ─────────────────────────────────────────────
    // Data class
    // ─────────────────────────────────────────────

    /**
     * Immutable snapshot of a single preview roll.
     */
    static class PreviewData {
        final String tier;
        final double weight;
        final double price;
        final long   rolledAt; // epoch ms — used for TTL expiry

        PreviewData(String tier, double weight, double price, long rolledAt) {
            this.tier      = tier;
            this.weight    = weight;
            this.price     = price;
            this.rolledAt  = rolledAt;
        }
    }
}
