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

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Proxy;

public final class WEUtils {
    private WEUtils() { }

    public static @NotNull Location weLocation(@NotNull org.bukkit.Location location) {
        return BukkitAdapter.adapt(location);
    }

    public static @NotNull World weWorld(@NotNull org.bukkit.World world) {
        return BukkitAdapter.adapt(world);
    }

    public static WorldEditPlugin getWorldEditPlugin() {
        return JavaPlugin.getPlugin(WorldEditPlugin.class);
    }

    public static @NotNull Region getSelection(@NotNull Player player) throws IncompleteRegionException {
        return getWorldEditPlugin().getSession(player).getSelection(weWorld(player.getWorld()));
    }

    public static boolean expandVert(@NotNull Player player) {
        LocalSession session = getWorldEditPlugin().getSession(player);
        com.sk89q.worldedit.world.World weWorld = weWorld(player.getWorld());
        try {
            Region region = session.getSelection(weWorld);
            region.expand(
                    BlockVector3.at(0, (weWorld.getMaxY() + 1), 0),
                    BlockVector3.at(0, -(weWorld.getMaxY() + 1), 0)
            );
            session.getRegionSelector(weWorld).learnChanges();
            return true;
        } catch (Throwable ignored) { }
        return false;
    }

    public static @NotNull Actor privilegedActor(@NotNull CommandSender sender, boolean showMessages) {
        Actor actor;
        if (sender instanceof Player player) {
            actor = WGUtils.wgPlayer(player);
        } else {
            actor = WorldGuardPlugin.inst().wrapCommandSender(sender);
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
}
