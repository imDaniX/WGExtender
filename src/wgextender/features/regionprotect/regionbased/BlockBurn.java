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

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBurnEvent;
import wgextender.config.Config;
import wgextender.features.ConfigurableListenerBase;
import wgextender.utils.WGUtils;

public final class BlockBurn extends ConfigurableListenerBase {
	public BlockBurn(Config config) {
		super(config);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent event) {
		if (
				config.forWorld(event.getBlock().getWorld()).regionProtection().fire().burn() &&
				WGUtils.isInRegion(event.getBlock().getLocation())
		) {
			event.setCancelled(true);
		}
	}
}
