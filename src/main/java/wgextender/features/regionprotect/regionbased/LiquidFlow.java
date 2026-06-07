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

package wgextender.features.regionprotect.regionbased;

import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.jetbrains.annotations.NotNull;
import wgextender.config.ConfigurationProvider;
import wgextender.features.ConfigurableListenerBase;
import wgextender.utils.WGUtils;

public final class LiquidFlow extends ConfigurableListenerBase<ConfigurationProvider.Flow> {
	public LiquidFlow(@NotNull ConfigurationProvider cfgProvider) {
		super(cfgProvider, ConfigurationProvider.Flow.SECTION);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onLiquidFlow(BlockFromToEvent event) {
		if (event.getBlock().isLiquid()) {
			check(event.getBlock(), event.getToBlock(), event, true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onDispense(BlockDispenseEvent event) {
		Block block = event.getBlock();
		BlockData blockData = block.getBlockData();
		if (blockData instanceof Directional directional) {
			Block relative = block.getRelative(directional.getFacing());
			if (relative.isLiquid()) {
				check(block, relative, event, false);
			}
		}
	}

	private void check(Block source, Block to, Cancellable event, boolean checkSource) {
		if (switch (checkSource ? source.getType() : to.getType()) {
			case LAVA -> config.lava();
			case WATER -> config.water();
			default -> config.other();
		} && !WGUtils.isInTheSameRegionOrWild(source.getLocation(), to.getLocation())) {
			event.setCancelled(true);
		}
	}
}
