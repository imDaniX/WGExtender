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

package wgextender.features.regionprotect.ownormembased;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;
import wgextender.WGExtender;
import wgextender.config.message.MKey;
import wgextender.config.section.Features;
import wgextender.config.section.RestrictCommands;
import wgextender.features.ScopedListenerBase;
import wgextender.utils.WGUtils;
import wgextender.utils.command.CommandsUtils;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

public final class RestrictCommandsHandler extends ScopedListenerBase<RestrictCommands> {
    private final WGExtender plugin;
    private final Server server;

    private static final Predicate<String> NEVER = command -> false;

    private final Map<String, Predicate<String>> predicates = new ConcurrentHashMap<>();
    private ScheduledTask recheckTask;

    public RestrictCommandsHandler(WGExtender plugin) {
        super(plugin.getConfigurationProvider(), RestrictCommands.POINTER);
        this.plugin = plugin;
        this.server = plugin.getServer();
        scheduleRecheckTask();
    }

    @Override
    protected void subReload() {
        if (recheckTask != null && !recheckTask.isCancelled()) {
            recheckTask.cancel();
        }
        predicates.clear();
        scheduleRecheckTask();
    }

    private @NotNull Predicate<String> buildPredicate(@NotNull RestrictCommands config) {
        if (!config.enabled()) {
            return NEVER;
        }
        Function<String, Iterable<String>> aliases = config.aliasedSearch()
                ? base -> CommandsUtils.getCommandAliases(server, base)
                : base -> Set.of(base.split(" ", 2)[0]);
        return config.prefixedSearch()
                ? CommandsUtils.computePrefixedVariants(config.commands(), aliases)
                : CommandsUtils.computeVariants(config.commands(), aliases);
    }

    private void scheduleRecheckTask() {
        int recheckTicks = cfgProvider.section(Features.POINTER).restrictCommandsRecheckTicks();
        if (recheckTicks <= 0) {
            return;
        }
        recheckTask = server.getGlobalRegionScheduler().runAtFixedRate(
                plugin,
                task -> predicates.replaceAll((world, old) -> buildPredicate(config(world))),
                1, recheckTicks
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        Predicate<String> restricted = predicates.computeIfAbsent(player.getWorld().getName(), name -> buildPredicate(config(name)));
        if (restricted == NEVER) {
            return;
        }
        if (WGUtils.canBypassProtection(player)) {
            return;
        }
        Location loc = player.getLocation();
        if (!WGUtils.isInRegion(loc) || WGUtils.canBuild(player, loc)) { // TODO canBuild is not a great check for commands?
            return;
        }

        if (restricted.test(event.getMessage().substring(1).trim())) {
            event.setCancelled(true);
            msg.sendMessage(player, MKey.RESTRICTED_COMMAND);
        }
    }
}
