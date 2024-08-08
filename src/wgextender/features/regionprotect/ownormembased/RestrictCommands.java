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

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class RestrictCommands implements Listener {
	private final Pattern SPACE_PATTERN = Pattern.compile("\\s+");
	private static final long TICK = 1000 / 20;

	protected final Config config;
	protected volatile Collection<String> restrictedCommands;

	public RestrictCommands(Config config) {
		this.config = config;
		restrictedCommands = config.restrictedCommandsInRegion;
        Bukkit.getAsyncScheduler().runAtFixedRate(
                WGExtender.getInstance(),
                (task) -> commandRecheckTask(config),
                TICK, TICK * 100, TimeUnit.MILLISECONDS
        );
	}

	private void commandRecheckTask(Config config) {
		if (!config.restrictCommandsInRegionEnabled) {
			return;
		}
		Set<String> computedRestrictedCommands = new HashSet<>();
		for (String restrictedCommand : config.restrictedCommandsInRegion) {
			String[] split = SPACE_PATTERN.split(restrictedCommand, 2);
			String toAdd = split.length > 1 ? split[1] : "";
			for (String alias : CommandUtils.getCommandAliases(split[0].toLowerCase(Locale.ROOT))) {
				computedRestrictedCommands.add(alias + toAdd);
			}
		}
		restrictedCommands = computedRestrictedCommands;
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
			String command = event.getMessage().substring(1).toLowerCase(Locale.ROOT);
			for (String rcommand : restrictedCommands) {
				if (command.startsWith(rcommand) && (command.length() == rcommand.length() || command.charAt(rcommand.length()) == ' ')) {
					event.setCancelled(true);
					player.sendMessage(ChatColor.RED + "Вы не можете использовать эту команду в чужом регионе");
					return;
				}
			}
		}
	}
}
