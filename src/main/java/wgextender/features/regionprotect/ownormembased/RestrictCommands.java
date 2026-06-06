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

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import wgextender.WGExtender;
import wgextender.config.Config;
import wgextender.config.message.MKey;
import wgextender.features.ConfigurableListenerBase;
import wgextender.utils.CommandsUtils;
import wgextender.utils.WGUtils;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public final class RestrictCommands extends ConfigurableListenerBase {
	private final Server server;
	private Predicate<String> restrictedCommands;

	public RestrictCommands(WGExtender plugin) {
		super(plugin.getPluginConfig());
		this.server = plugin.getServer();
		restrictedCommands = command -> false;
        server.getGlobalRegionScheduler().runAtFixedRate( // TODO Reschedule on reload
                plugin,
                (task) -> commandRecheckTask(config),
                1, Math.max(1, config.restrictCommandsRecheckTicks)
        );
	}

	private void commandRecheckTask(Config config) { // TODO React to reload
		if (!config.restrictCommandsInRegionEnabled) {
			return;
		}

		Function<String, Iterable<String>> aliases = config.restrictCommandsAliasedSearch
				? base -> CommandsUtils.getCommandAliases(server, base)
				: base -> Set.of(base.split(" ", 2)[0]);

		restrictedCommands = config.restrictCommandsPrefixedSearch
				? CommandsUtils.computePrefixedVariants(config.restrictedCommandsInRegion, aliases)
				: CommandsUtils.computeVariants(config.restrictedCommandsInRegion, aliases);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (!config.restrictCommandsInRegionEnabled) {
			return;
		}
		Player player = event.getPlayer();
		if (WGUtils.canBypassProtection(player)) {
			return;
		}
		Location loc = player.getLocation();
        if (!WGUtils.isInRegion(loc) || WGUtils.canBuild(player, loc)) { // TODO canBuild is not a great check for commands?
            return;
        }

		if (restrictedCommands.test(event.getMessage().substring(1).trim())) {
			event.setCancelled(true);
			msg.sendMessage(player, MKey.RESTRICTED_COMMAND);
		}
    }
}
