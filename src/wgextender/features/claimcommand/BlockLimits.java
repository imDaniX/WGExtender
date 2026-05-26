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

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import wgextender.VaultIntegration;
import wgextender.config.Config;
import wgextender.utils.WEUtils;

import java.math.BigInteger;

public final class BlockLimits {
	private static final BigInteger MAX_VALUE = BigInteger.valueOf(Integer.MAX_VALUE);

	private BlockLimits() { }

	public static @NotNull ProcessedClaimInfo processClaimInfo(Config config, Player player) {
		Region selection;
		try {
			selection = WEUtils.getSelection(player);
		} catch (IncompleteRegionException e) {
			return ProcessedClaimInfo.EMPTY_ALLOW;
		}
		BlockVector3 min = selection.getMinimumPoint();
		BlockVector3 max = selection.getMaximumPoint();

		BigInteger yDistance = distance(min.y(), max.y());
		BigInteger xDistance = distance(min.x(), max.x());
		BigInteger zDistance = distance(min.z(), max.z());
		BigInteger minHorizontal = xDistance.min(zDistance);

		BigInteger volume = BigInteger.ONE
				.multiply(xDistance)
				.multiply(zDistance)
				.multiply(yDistance);
		if (volume.compareTo(MAX_VALUE) > 0) {
			return new ProcessedClaimInfo(
					Result.DENY_MAX_VOLUME,
					volume,
					MAX_VALUE
			);
		}
		if (config.claimBlockLimitsEnabled) {
			if (player.hasPermission("worldguard.region.unlimited")) {
				return ProcessedClaimInfo.EMPTY_ALLOW;
			}
			if (volume.compareTo(config.claimBlockMinimalVolume) < 0) {
				return new ProcessedClaimInfo(
						Result.DENY_MIN_VOLUME,
						volume,
						config.claimBlockMinimalVolume
				);
			}
			if (minHorizontal.compareTo(config.claimBlockMinimalHorizontal) < 0) {
				return new ProcessedClaimInfo(
						Result.DENY_MIN_VOLUME,
						minHorizontal,
						config.claimBlockMinimalHorizontal
				);
			}
			if (yDistance.compareTo(config.claimBlockMinimalVertical) < 0) {
				return new ProcessedClaimInfo(
						Result.DENY_VERTICAL,
						yDistance,
						config.claimBlockMinimalVertical
				);
			}
			String[] groups = VaultIntegration.getInstance().getPermissions().getPlayerGroups(player);
			if (groups.length == 0) {
				return ProcessedClaimInfo.EMPTY_ALLOW;
			}
			BigInteger maxBlocks = config.claimBlockLimitDefault;
			for (String group : groups) {
				maxBlocks = maxBlocks.max(config.claimBlockLimits.getOrDefault(group.toLowerCase(), BigInteger.ZERO));
			}
			if (volume.compareTo(maxBlocks) > 0) {
				return new ProcessedClaimInfo(
						Result.DENY_MAX_VOLUME,
						volume,
						maxBlocks
				);
			}
		}
		return ProcessedClaimInfo.EMPTY_ALLOW;
	}

	public record ProcessedClaimInfo(Result result, BigInteger assignedSize, BigInteger assignedLimit) {
		public static final ProcessedClaimInfo EMPTY_ALLOW = new ProcessedClaimInfo(Result.ALLOW, BigInteger.ZERO, BigInteger.ZERO);
	}

	public enum Result {
		ALLOW, DENY_MAX_VOLUME, DENY_MIN_VOLUME, DENY_HORIZONTAL, DENY_VERTICAL
	}

	private static BigInteger distance(long min, long max) {
		return BigInteger.valueOf(max - min + 1L);
	}
}
