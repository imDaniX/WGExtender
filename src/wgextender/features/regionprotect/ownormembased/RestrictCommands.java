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

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import wgextender.Config;
import wgextender.WGExtender;
import wgextender.utils.CommandUtils;
import wgextender.utils.WGRegionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RestrictCommands implements Listener {
	private static final long MS_PER_TICK = 50;

	protected final Config config;
	protected volatile List<String> restrictedCommands;

	public RestrictCommands(Config config) {
		this.config = config;
		restrictedCommands = config.restrictedCommandsInRegion;
		startCommandRecheckTask(config);
	}

	private void startCommandRecheckTask(Config config) {
		Bukkit.getAsyncScheduler().runAtFixedRate(WGExtender.getInstance(), (task) -> {
			if (!config.restrictCommandsInRegionEnabled) {
				return;
			}
			List<String> computedRestrictedCommands = new ArrayList<>();
			for (String restrictedCommand : config.restrictedCommandsInRegion) {
				String[] split = restrictedCommand.split(" ", 2);
				String toAdd = " " + split[1].trim();
				for (String alias : CommandUtils.getCommandAliases(split[0])) {
					computedRestrictedCommands.add(alias + toAdd);
				}
			}
			restrictedCommands = computedRestrictedCommands;
		}, MS_PER_TICK, 100 * MS_PER_TICK, TimeUnit.MICROSECONDS);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (!config.restrictCommandsInRegionEnabled) {
			return;
		}
		Player player = event.getPlayer();
		if (WGRegionUtils.canBypassProtection(player)) {
			return;
		}
		if (WGRegionUtils.isInWGRegion(player.getLocation()) && !WGRegionUtils.canBuild(player, player.getLocation())) {
			String message = event.getMessage().substring(1).toLowerCase();
			for (String rcommand : restrictedCommands) {
				if (message.startsWith(rcommand) && ((message.length() == rcommand.length()) || (message.charAt(rcommand.length()) == ' '))) {
					event.setCancelled(true);
					player.sendMessage(ChatColor.RED + "Вы не можете использовать эту команду на чужом регионе");
					return;
				}
			}
		}
	}
}
