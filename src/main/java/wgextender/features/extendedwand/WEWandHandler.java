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

package wgextender.features.extendedwand;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import wgextender.config.ConfigurationProvider;
import wgextender.features.ConfigurableListenerBase;

// TODO We can disable listening
public final class WEWandHandler extends ConfigurableListenerBase<ConfigurationProvider.Misc> {
	private final WEWand weWand;

	public WEWandHandler(@NotNull ConfigurationProvider cfgProvider, @NotNull WEWand weWand) {
		super(cfgProvider, ConfigurationProvider::miscCfg);
		this.weWand = weWand;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityAttack(EntityDamageByEntityEvent event) {
		if (!config.extendedWeWand()) return;
		if (event.getDamager() instanceof Player player) {
			ItemStack item = player.getInventory().getItemInMainHand();
			if (weWand.isWand(item)) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerDeath(PlayerDeathEvent event) {
		if (!config.extendedWeWand()) return;
		event.getDrops().removeIf(weWand::isWand);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onItemDrop(PlayerDropItemEvent event) {
		if (!config.extendedWeWand()) return;
		Item drop = event.getItemDrop();
		if (weWand.isWand(drop.getItemStack())) {
			drop.remove();
		}
	}
}
