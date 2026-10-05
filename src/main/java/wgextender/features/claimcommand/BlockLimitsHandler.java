/**
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

package wgextender.features.claimcommand;

import com.sk89q.wepif.PermissionsResolverManager;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wgextender.config.ConfigurationProvider;
import wgextender.config.section.BlockLimits;
import wgextender.features.ScopedListenerBase;
import wgextender.utils.Comparison;
import wgextender.utils.WEUtils;

import java.math.BigInteger;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static wgextender.utils.Comparison.is;

public final class BlockLimitsHandler extends ScopedListenerBase<BlockLimits> {
    private static final BigInteger MAX_VALUE = BigInteger.valueOf(Integer.MAX_VALUE);

    // Player -> lowercase world name (empty for the general config) -> limit
    private final Map<UUID, Map<String, BigInteger>> cache;

    public BlockLimitsHandler(@NotNull ConfigurationProvider cfgProvider) {
        super(cfgProvider, BlockLimits.POINTER);
        this.cache = new ConcurrentHashMap<>();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        cache.remove(e.getPlayer().getUniqueId());
    }

    @Override
    protected void subReload() {
        clearCache();
    }

    public void clearCache() {
        cache.clear();
    }

    public @NotNull BigInteger groupBlockLimit(@NotNull String group) {
        return groupBlockLimit(group, null);
    }

    /**
     * @param world world name, {@code null} for the general config
     */
    public @NotNull BigInteger groupBlockLimit(@NotNull String group, @Nullable String world) {
        var config = config(world);
        return config.limits().getOrDefault(group, config.defaultLimit());
    }

    /**
     * Returns cached blocks limit for the player's current world or creates one
     * @param player player to check the limit for
     * @return cached or calculated limit
     * @see #cachedBlockLimit(Player, String)
     * @see #refreshBlockLimit(Player)
     */
    public @NotNull BigInteger cachedBlockLimit(@NotNull Player player) {
        return cachedBlockLimit(player, player.getWorld().getName());
    }

    /**
     * Returns cached blocks limit for the world or creates one
     * @param player player to check the limit for
     * @param world world name to use the config of, {@code null} for the general config
     * @return cached or calculated limit
     * @see #refreshBlockLimit(Player, String)
     * @see #calculateBlockLimit(OfflinePlayer, String)
     */
    public @NotNull BigInteger cachedBlockLimit(@NotNull Player player, @Nullable String world) {
        return worldCache(player).computeIfAbsent(worldKey(world), key -> calculateBlockLimit(player, world));
    }

    /**
     * Recalculates and caches blocks limit for the player's current world
     * @param player player to recalculate the limit for
     * @return calculated limit
     * @see #refreshBlockLimit(Player, String)
     * @see #cachedBlockLimit(Player)
     */
    public @NotNull BigInteger refreshBlockLimit(@NotNull Player player) {
        return refreshBlockLimit(player, player.getWorld().getName());
    }

    /**
     * Recalculates and caches blocks limit for the world
     * @param player player to recalculate the limit for
     * @param world world name to use the config of, {@code null} for the general config
     * @return calculated limit
     * @see #cachedBlockLimit(Player, String)
     * @see #calculateBlockLimit(OfflinePlayer, String)
     */
    public @NotNull BigInteger refreshBlockLimit(@NotNull Player player, @Nullable String world) {
        return worldCache(player).compute(worldKey(world), (key, old) -> calculateBlockLimit(player, world));
    }

    /**
     * Recalculates blocks limit using the general config, without caching it
     * @param player player to recalculate the limit for
     * @return calculated limit
     * @see #calculateBlockLimit(OfflinePlayer, String)
     */
    public @NotNull BigInteger calculateBlockLimit(@NotNull OfflinePlayer player) {
        return calculateBlockLimit(player, null);
    }

    /**
     * Recalculates blocks limit without caching it
     * @param player player to recalculate the limit for
     * @param world world name to use the config of, {@code null} for the general config
     * @return calculated limit
     * @see #cachedBlockLimit(Player, String)
     * @see #refreshBlockLimit(Player, String)
     */
    public @NotNull BigInteger calculateBlockLimit(@NotNull OfflinePlayer player, @Nullable String world) {
        var config = config(world);
        String[] groups = PermissionsResolverManager.getInstance().getGroups(player);
        if (groups.length == 0) {
            return config.defaultLimit();
        }
        BigInteger maxBlocks = BigInteger.ZERO;
        for (String group : groups) {
            maxBlocks = maxBlocks.max(config.limits().getOrDefault(group, BigInteger.ZERO));
        }
        return maxBlocks;
    }

    public @NotNull BlockLimitsHandler.EvaluationResult evaluateResult(@NotNull Player player) {
        var config = config(player.getWorld());
        Region selection;
        try {
            selection = WEUtils.getSelection(player);
        } catch (IncompleteRegionException e) {
            return EvaluationResult.EMPTY_ALLOW;
        }

        BigInteger volume = BigInteger.valueOf(selection.getVolume());
        if (is(volume, Comparison.ABOVE, MAX_VALUE)) {
            return new EvaluationResult(
                    ResultType.DENY_MAX_VOLUME,
                    volume,
                    MAX_VALUE
            );
        }
        if (config.enabled()) {
            if (player.hasPermission("worldguard.region.unlimited")) {
                return EvaluationResult.EMPTY_ALLOW;
            }

            BlockVector3 min = selection.getMinimumPoint();
            BlockVector3 max = selection.getMaximumPoint();

            BigInteger yDistance = distance(min.y(), max.y());
            BigInteger xDistance = distance(min.x(), max.x());
            BigInteger zDistance = distance(min.z(), max.z());
            BigInteger minHorizontal = xDistance.min(zDistance);

            if (is(volume, Comparison.BELOW, config.minimalVolume())) {
                return new EvaluationResult(
                        ResultType.DENY_MIN_VOLUME,
                        volume,
                        config.minimalVolume()
                );
            }
            if (is(minHorizontal, Comparison.BELOW, config.minimalHorizontal())) {
                return new EvaluationResult(
                        ResultType.DENY_HORIZONTAL,
                        minHorizontal,
                        config.minimalHorizontal()
                );
            }
            if (is(yDistance, Comparison.BELOW, config.minimalVertical())) {
                return new EvaluationResult(
                        ResultType.DENY_VERTICAL,
                        yDistance,
                        config.minimalVertical()
                );
            }
            BigInteger maxBlocks = refreshBlockLimit(player);
            if (is(volume, Comparison.ABOVE, maxBlocks)) {
                return new EvaluationResult(
                        ResultType.DENY_MAX_VOLUME,
                        volume,
                        maxBlocks
                );
            }
        }
        return EvaluationResult.EMPTY_ALLOW;
    }

    public record EvaluationResult(@NotNull BlockLimitsHandler.ResultType type, @NotNull BigInteger assignedSize, @NotNull BigInteger assignedLimit) {
        public static final EvaluationResult EMPTY_ALLOW = new EvaluationResult(ResultType.ALLOW, MAX_VALUE, MAX_VALUE);
    }

    public enum ResultType {
        ALLOW, DENY_MAX_VOLUME, DENY_MIN_VOLUME, DENY_HORIZONTAL, DENY_VERTICAL
    }

    private @NotNull Map<String, BigInteger> worldCache(@NotNull Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), id -> new ConcurrentHashMap<>());
    }

    private static @NotNull String worldKey(@Nullable String world) {
        return world == null ? "" : world.toLowerCase(Locale.ROOT);
    }

    private static @NotNull BigInteger distance(long min, long max) {
        return BigInteger.valueOf(max - min + 1L);
    }
}
