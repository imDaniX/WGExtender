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
import wgextender.WGExtender;
import wgextender.config.ConfigurationProvider;
import wgextender.config.message.MKey;
import wgextender.features.ConfigurableListenerBase;
import wgextender.utils.WGUtils;
import wgextender.utils.command.CommandsUtils;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public final class RestrictCommandsHandler extends ConfigurableListenerBase<ConfigurationProvider.RestrictCommands> {

	private final WGExtender plugin;
	private final Server server;

	private Predicate<String> restrictedCommands = command -> false;
	private ScheduledTask recheckTask;

	public RestrictCommandsHandler(WGExtender plugin) {
		super(plugin.getConfigurationProvider(), ConfigurationProvider.RestrictCommands.SECTION);
		this.plugin = plugin;
		this.server = plugin.getServer();
		scheduleRecheckTask();
	}

	@Override
	protected void subReload(ConfigurationProvider.RestrictCommands oldConfig) {
		if (recheckTask != null && !recheckTask.isCancelled()) {
			recheckTask.cancel();
		}
		if (!config.enabled()) {
			return;
		}
		rebuildPredicate();
		scheduleRecheckTask();
	}

	private void rebuildPredicate() {
		if (!config.enabled()) {
			restrictedCommands = command -> false;
			return;
		}
		Function<String, Iterable<String>> aliases = config.aliasedSearch()
				? base -> CommandsUtils.getCommandAliases(server, base)
				: base -> Set.of(base.split(" ", 2)[0]);
		restrictedCommands = config.prefixedSearch()
				? CommandsUtils.computePrefixedVariants(config.commands(), aliases)
				: CommandsUtils.computeVariants(config.commands(), aliases);
	}

	private void scheduleRecheckTask() {
		if (config.recheckTicks() <= 0) {
			return;
		}
		recheckTask = server.getGlobalRegionScheduler().runAtFixedRate(
				plugin,
				task -> rebuildPredicate(),
				1, config.recheckTicks()
		);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (!config.enabled()) {
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
