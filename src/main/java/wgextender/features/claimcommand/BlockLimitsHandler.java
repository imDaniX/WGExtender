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
import wgextender.config.ConfigurationProvider;
import wgextender.features.ConfigurableListenerBase;
import wgextender.utils.Comparison;
import wgextender.utils.WEUtils;

import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static wgextender.utils.Comparison.is;

public final class BlockLimitsHandler extends ConfigurableListenerBase<ConfigurationProvider.BlockLimits> {
	private static final BigInteger MAX_VALUE = BigInteger.valueOf(Integer.MAX_VALUE);

	private final Map<UUID, BigInteger> cache;

	public BlockLimitsHandler(@NotNull ConfigurationProvider cfgProvider) {
		super(cfgProvider, ConfigurationProvider.BlockLimits.SECTION);
		this.cache = new ConcurrentHashMap<>();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onQuit(PlayerQuitEvent e) {
		cache.remove(e.getPlayer().getUniqueId());
	}

	public void clearCache() {
		cache.clear();
	}

	public @NotNull BigInteger groupBlockLimit(@NotNull String group) {
		return config.limits().getOrDefault(group, config.defaultLimit());
	}

	/**
	 * Returns cached blocks limit or creates one
	 * @param player player to check the limit for
	 * @return cached or calculated limit
	 * @see #refreshBlockLimit(Player)
	 * @see #calculateBlockLimit(OfflinePlayer)
	 */
	public @NotNull BigInteger cachedBlockLimit(@NotNull Player player) {
		return cache.computeIfAbsent(player.getUniqueId(), id -> calculateBlockLimit(player));
	}

	/**
	 * Recalculates and caches blocks limit
	 * @param player player to recalculate the limit for
	 * @return calculated limit
	 * @see #cachedBlockLimit(Player)
	 * @see #calculateBlockLimit(OfflinePlayer)
	 */
	public @NotNull BigInteger refreshBlockLimit(@NotNull Player player) {
		return cache.compute(player.getUniqueId(), (id, old) -> calculateBlockLimit(player));
	}

	/**
	 * Recalculates blocks limit without caching it
	 * @param player player to recalculate the limit for
	 * @return calculated limit
	 * @see #cachedBlockLimit(Player)
	 * @see #refreshBlockLimit(Player)
	 */
	public @NotNull BigInteger calculateBlockLimit(@NotNull OfflinePlayer player) {
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

	private static @NotNull BigInteger distance(long min, long max) {
		return BigInteger.valueOf(max - min + 1L);
	}
}
