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

package wgextender.utils;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.BukkitWorldConfiguration;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.commands.region.RegionCommands;
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.BooleanFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class WGUtils {
	public static final RegionQuery REGION_QUERY = getRegionContainer().createQuery();

	private static final RegionCommands REGION_COMMANDS = new RegionCommands(getWorldGuard());
	private static final Set<Character> FLAG_COMMAND_FLAGS = getFlagCommandFlags();

	private WGUtils() { }

	public static LocalPlayer wgPlayer(Player player) {
		return WorldGuardPlugin.inst().wrapPlayer(player);
	}

	public static WorldGuard getWorldGuard() {
		return WorldGuard.getInstance();
	}

	public static WorldGuardPlatform getPlatform() {
		return getWorldGuard().getPlatform();
	}

	public static RegionContainer getRegionContainer() {
		return getPlatform().getRegionContainer();
	}

	public static RegionManager getRegionManager(World world) {
		return getRegionContainer().get(BukkitAdapter.adapt(world));
	}

	public static BukkitWorldConfiguration getWorldConfig(World world) {
		return (BukkitWorldConfiguration) getPlatform().getGlobalStateManager().get(BukkitAdapter.adapt(world));
	}

	public static BukkitWorldConfiguration getWorldConfig(Player player) {
		return getWorldConfig(player.getWorld());
	}

	public static boolean canBypassProtection(Player player) {
		return getPlatform().getSessionManager().hasBypass(wgPlayer(player), BukkitAdapter.adapt(player.getWorld()));
	}

	public static boolean isInRegion(Location location) {
		return getRegionsAt(location).size() > 0;
	}

	public static boolean isInTheSameRegionOrWild(Location location1, Location location2) {
		return getRegionsAt(location1).getRegions().equals(getRegionsAt(location2).getRegions());
	}

	public static boolean isInTheSameRegion(Location location1, Location location2) {
		ApplicableRegionSet ars1 = getRegionsAt(location1);
		ApplicableRegionSet ars2 = getRegionsAt(location2);
		return (ars1.size() > 0) && ars1.getRegions().equals(ars2.getRegions());
	}

	public static boolean canBuild(Player player, Location location) {
		return isFlagAllows(player, location, Flags.BUILD);
	}

	public static boolean isFlagAllows(Player player, Location location, StateFlag flag) {
		return REGION_QUERY.testState(BukkitAdapter.adapt(location), WorldGuardPlugin.inst().wrapPlayer(player), flag);
	}

	public static boolean isFlagTrue(Location location, BooleanFlag flag) {
		Boolean bool = REGION_QUERY.queryValue(BukkitAdapter.adapt(location), null, flag);
		return (bool != null) && bool;
	}

	public static ApplicableRegionSet getRegionsAt(Location location) {
		return REGION_QUERY.getApplicableRegions(BukkitAdapter.adapt(location));
	}

	public static boolean hasRegion(World world, String regionName) {
		return getRegion(world, regionName) != null;
	}

	public static ProtectedRegion getRegion(World world, String regionName) {
		final RegionManager rm = getRegionManager(world);
		if (rm == null) {
			return null;
		}
		return rm.getRegion(regionName);
	}

	public static <T> void setFlagNaturally(Actor actor, World world, ProtectedRegion region, Flag<T> flag, String value) throws CommandException {
		CommandContext context = new CommandContext(String.format("flag %s -w %s %s %s", region.getId(), world.getName(), flag.getName(), value), FLAG_COMMAND_FLAGS);
		REGION_COMMANDS.flag(context, actor);
	}

	private static Set<Character> getFlagCommandFlags() {
		try {
			Method method = RegionCommands.class.getMethod("flag", CommandContext.class, Actor.class);
			Command annotation = method.getAnnotation(Command.class);
			char[] flags = annotation.flags().toCharArray();
			Set<Character> valueFlags = new HashSet<>();
			for (int i = 0; i < flags.length; ++i) {
				if ((flags.length > (i + 1)) && (flags[i + 1] == ':')) {
					valueFlags.add(flags[i]);
					++i;
				}
			}
			return valueFlags;
		} catch (Throwable t) {
			t.printStackTrace();
			return Collections.emptySet();
		}
	}

	public static Flag<?> matchFlag(String flagStr) {
		return Flags.fuzzyMatchFlag(getWorldGuard().getFlagRegistry(), flagStr);
	}
}
