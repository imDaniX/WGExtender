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

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.jetbrains.annotations.NotNull;
import wgextender.config.ConfigurationProvider;
import wgextender.features.ConfigurableListenerBase;
import wgextender.utils.WGUtils;

public final class FireBurn extends ConfigurableListenerBase<ConfigurationProvider.Fire> {
    public FireBurn(@NotNull ConfigurationProvider cfgProvider) {
        super(cfgProvider, ConfigurationProvider.Fire.SECTION);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockIgniteBySpread(BlockSpreadEvent event) {
        if (event.getNewState().getType() == Material.FIRE) {
            if (config.spreadToRegion()) {
                if (!WGUtils.isInTheSameRegionOrWild(event.getSource().getLocation(), event.getBlock().getLocation())) {
                    event.setCancelled(true);
                    return;
                }
            }
            if (config.spreadInRegion()) {
                if (WGUtils.isInTheSameRegion(event.getSource().getLocation(), event.getBlock().getLocation())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (config.burn() && WGUtils.isInRegion(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }
}
