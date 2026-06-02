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

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import wgextender.WGExtender;
import wgextender.config.Config;
import wgextender.config.message.MKey;
import wgextender.features.ConfigurableListenerBase;
import wgextender.utils.CommandUtils;
import wgextender.utils.WGUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class RestrictCommands extends ConfigurableListenerBase {
	private final Pattern SPACE_PATTERN = Pattern.compile("\\s+");

	private final Server server;
	private final Map<String, Collection<String>> restrictedCommandsPerWorld = new ConcurrentHashMap<>();

	public RestrictCommands(WGExtender plugin) {
		super(plugin.getPluginConfig());
		this.server = plugin.getServer();
		server.getGlobalRegionScheduler().runAtFixedRate(
				plugin,
				(task) -> commandRecheckTask(),
				1, config.forWorld("").restrictedCommands().recheckTicks()
		);
	}

	private void commandRecheckTask() {
		for (World world : server.getWorlds()) {
			Config.RestrictedCommands rc = config.forWorld(world).restrictedCommands();
			if (!rc.enabled()) {
				restrictedCommandsPerWorld.remove(world.getName());
				continue;
			}
			Set<String> computed = new HashSet<>();
			for (String restrictedCommand : rc.commands()) {
				String[] split = SPACE_PATTERN.split(restrictedCommand, 2);
				String toAdd = split.length > 1 ? split[1] : "";
				for (String alias : CommandUtils.getCommandAliases(server, split[0].toLowerCase(Locale.ROOT))) {
					computed.add(alias + toAdd);
				}
			}
			restrictedCommandsPerWorld.put(world.getName(), computed);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();
		if (!config.forWorld(player.getWorld()).restrictedCommands().enabled()) {
			return;
		}
		if (WGUtils.canBypassProtection(player)) {
			return;
		}
		if (WGUtils.isInRegion(player.getLocation()) && !WGUtils.canBuild(player, player.getLocation())) { // TODO canBuild is not a great check for commands?
			Collection<String> restrictedCommands = restrictedCommandsPerWorld.getOrDefault(player.getWorld().getName(), List.of());
			String command = event.getMessage().substring(1).toLowerCase(Locale.ROOT);
			for (String rcommand : restrictedCommands) {
				if (command.startsWith(rcommand) && (command.length() == rcommand.length() || command.charAt(rcommand.length()) == ' ')) {
					event.setCancelled(true);
					msg.sendMessage(player, MKey.RESTRICTED_COMMAND);
					return;
				}
			}
		}
	}
}
