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

import org.bukkit.Location;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import wgextender.config.Config;
import wgextender.features.ConfigurableListenerBase;
import wgextender.utils.WGUtils;

import java.util.function.Predicate;

public final class Explode extends ConfigurableListenerBase {
	public Explode(Config config) {
		super(config);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event) {
		if (!config.checkExplosionBlockDamage) {
			return;
		}
		Player source = findExplosionSource(event.getEntity());
		Predicate<Location> shouldProtectBlockPredicate;
		if (source != null) {
			boolean canBypass = WGUtils.canBypassProtection(source);
			shouldProtectBlockPredicate = location -> !canBypass && !WGUtils.canBuild(source, location);
		} else {
			shouldProtectBlockPredicate = WGUtils::isInRegion;
		}
		event.blockList().removeIf(block -> shouldProtectBlockPredicate.test(block.getLocation()));
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBlockExplode(BlockExplodeEvent event) {
		if (!config.checkExplosionBlockDamage) {
			return;
		}
		event.blockList().removeIf(block -> WGUtils.isInRegion(block.getLocation()));
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onEntityDamageByExplosion(EntityDamageEvent event) {
		if (!config.checkExplosionEntityDamage) {
			return;
		}
		if ((event.getCause() == DamageCause.BLOCK_EXPLOSION) || (event.getCause() == DamageCause.ENTITY_EXPLOSION)) {
			Location location = event.getEntity().getLocation();
			if (WGUtils.isInRegion(location)) {
				if (event instanceof EntityDamageByEntityEvent entityEvent) {
					Player source = findExplosionSource(entityEvent.getDamager());
					if (source == null || (!WGUtils.canBypassProtection(source) && !WGUtils.canBuild(source, location))) {
						event.setCancelled(true);
					}
				} else {
					event.setCancelled(true);
				}
			}

		}
	}

	private static Player findExplosionSource(Entity exploded) {
		Entity source;
		if (exploded instanceof TNTPrimed primed) {
			source = primed.getSource();
		} else if (exploded instanceof Creeper creeper) {
			source = creeper.getTarget(); // TODO Creeper can be ignited using flint and steel
		} else {
			return null;
		}
		return source instanceof Player player ? player : null;
	}
}
