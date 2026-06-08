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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wgextender.config.ConfigurationProvider;
import wgextender.features.ConfigurableListenerBase;
import wgextender.utils.WGUtils;

import java.util.function.Predicate;

public final class Explode extends ConfigurableListenerBase<ConfigurationProvider.Explosion> {
	public Explode(@NotNull ConfigurationProvider cfgProvider) {
		super(cfgProvider, ConfigurationProvider.Explosion.SECTION);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event) {
		if (!config.block()) {
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
		if (!config.block()) {
			return;
		}
		event.blockList().removeIf(block -> WGUtils.isInRegion(block.getLocation()));
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onEntityDamageByExplosion(EntityDamageEvent event) {
		if (!config.entity()) {
			return;
		}
		if ((event.getCause() == DamageCause.BLOCK_EXPLOSION) || (event.getCause() == DamageCause.ENTITY_EXPLOSION)) {
			Location location = event.getEntity().getLocation();
            if (!WGUtils.isInRegion(location)) {
                return;
            }
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

	private @Nullable Player findExplosionSource(@Nullable Entity exploded) {
		Entity source = null;
		if (exploded instanceof TNTPrimed primed) {
			if (config.tntPrime()) {
				source = primed.getSource();
			}
		} else if (exploded instanceof Creeper creeper) {// TODO Creepers can be ignited using flint and steel
			if (config.creeperTarget()) {
				source = creeper.getTarget();
			}
		} else { // TODO End crystals, beds, anchors
			return null;
		}
		return source instanceof Player player ? player : null;
	}
}
