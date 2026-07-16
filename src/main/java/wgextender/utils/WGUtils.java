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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static wgextender.utils.WEUtils.weLocation;
import static wgextender.utils.WEUtils.weWorld;

public final class WGUtils {
    public static final RegionQuery REGION_QUERY = getRegionContainer().createQuery();

    public static final RegionCommands REGION_COMMANDS = new RegionCommands(getWorldGuard());
    private static final Set<Character> FLAG_COMMAND_FLAGS = getFlagCommandFlags();

    private WGUtils() { }

    // This is, in fact, weSender... but whatever
    public static @NotNull Actor wgSender(@NotNull CommandSender sender) {
        return WorldGuardPlugin.inst().wrapCommandSender(sender);
    }

    public static @NotNull LocalPlayer wgPlayer(@NotNull Player player) {
        return WorldGuardPlugin.inst().wrapPlayer(player);
    }

    public static @NotNull Actor privilegedActor(@NotNull CommandSender sender, boolean showMessages) {
        Actor actor;
        if (sender instanceof Player player) {
            actor = wgPlayer(player);
        } else {
            // While WG's wrapCommandSender does check for player, we don't really expect non-player senders,
            // so it's generally faster for our case
            actor = wgSender(sender);
        }
        return (Actor) Proxy.newProxyInstance(
                actor.getClass().getClassLoader(),
                actor.getClass().getInterfaces(),
                (proxy, method, args) -> switch (method.getName()) {
                    case "print", "printRaw", "printDebug", "printError", "printInfo" ->
                            showMessages ? method.invoke(actor, args) : null;
                    case "hasPermission" -> true;
                    case "checkPermission" -> null;
                    default -> method.invoke(actor, args);
                }
        );
    }

    public static @NotNull WorldGuard getWorldGuard() {
        return WorldGuard.getInstance();
    }

    public static @NotNull WorldGuardPlatform getPlatform() {
        return getWorldGuard().getPlatform();
    }

    public static @NotNull RegionContainer getRegionContainer() {
        return getPlatform().getRegionContainer();
    }

    public static @Nullable RegionManager getRegionManager(@NotNull World world) {
        return getRegionContainer().get(weWorld(world));
    }

    public static @NotNull BukkitWorldConfiguration getWorldConfig(@NotNull World world) {
        return (BukkitWorldConfiguration) getPlatform().getGlobalStateManager().get(weWorld(world));
    }

    public static @NotNull BukkitWorldConfiguration getWorldConfig(@NotNull Player player) {
        return getWorldConfig(player.getWorld());
    }

    public static boolean canBypassProtection(@NotNull Player player) {
        return getPlatform().getSessionManager().hasBypass(wgPlayer(player), weWorld(player.getWorld()));
    }

    public static boolean isInRegion(@NotNull Location location) {
        return getRegionsAt(location).size() > 0;
    }

    public static boolean isInTheSameRegionOrWild(@NotNull Location location1, @NotNull Location location2) {
        return getRegionsAt(location1).getRegions().equals(getRegionsAt(location2).getRegions());
    }

    public static boolean isInTheSameRegion(@NotNull Location location1, @NotNull Location location2) {
        ApplicableRegionSet ars1 = getRegionsAt(location1);
        ApplicableRegionSet ars2 = getRegionsAt(location2);
        return (ars1.size() > 0) && ars1.getRegions().equals(ars2.getRegions());
    }

    public static boolean canBuild(@NotNull Player player, @NotNull Location at) {
        return isFlagAllows(player, at, Flags.BUILD);
    }

    public static <T> @Nullable T getFlagValue(@NotNull Player player, @NotNull Location location, @NotNull Flag<T> flag) {
        return REGION_QUERY.queryValue(weLocation(location), WorldGuardPlugin.inst().wrapPlayer(player), flag);
    }

    public static <T> @Nullable T getFlagValue(@NotNull Location location, @NotNull Flag<T> flag) {
        return REGION_QUERY.queryValue(weLocation(location), null, flag);
    }

    public static boolean isFlagAllows(@NotNull Player player, @NotNull Location location, @NotNull StateFlag flag) {
        return REGION_QUERY.testState(weLocation(location), WorldGuardPlugin.inst().wrapPlayer(player), flag);
    }

    public static boolean isFlagTrue(@NotNull Location location, @NotNull BooleanFlag flag) {
        Boolean bool = REGION_QUERY.queryValue(weLocation(location), null, flag);
        return (bool != null) && bool;
    }

    public static @NotNull ApplicableRegionSet getRegionsAt(@NotNull Location location) {
        return REGION_QUERY.getApplicableRegions(weLocation(location));
    }

    public static boolean hasRegion(@NotNull World world, @NotNull String regionName) {
        return getRegion(world, regionName) != null;
    }

    public static @Nullable ProtectedRegion getRegion(@NotNull World world, @NotNull String regionName) {
        RegionManager rm = getRegionManager(world);
        return rm == null ? null : rm.getRegion(regionName);
    }

    @SuppressWarnings("deprecation")
    public static <T> void setFlagNaturally(
            @NotNull Actor actor,
            @NotNull World world,
            @NotNull ProtectedRegion region,
            @NotNull Flag<T> flag,
            @NotNull String value
    ) throws CommandException {
        CommandContext context = new CommandContext(
                "flag " + region.getId() + " -w " + world.getName() + " " + flag.getName() + " " + value,
                FLAG_COMMAND_FLAGS
        );
        REGION_COMMANDS.flag(context, actor);
    }

    @SuppressWarnings("deprecation")
    private static @NotNull Set<Character> getFlagCommandFlags() {
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

    public static @Nullable Flag<?> matchFlag(String flagStr) {
        return Flags.fuzzyMatchFlag(getWorldGuard().getFlagRegistry(), flagStr);
    }
}
