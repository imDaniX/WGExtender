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

package wgextender.features.claimcommand;

import com.sk89q.minecraft.util.commands.CommandException;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import wgextender.config.Config;
import wgextender.config.message.MKey;
import wgextender.config.message.Messages;
import wgextender.utils.CommandUtils;
import wgextender.utils.WEUtils;
import wgextender.utils.WGRegionUtils;

public class WGRegionCommandWrapper extends Command {

	public static void inject(Server server, Config config) {
		WGRegionCommandWrapper wrapper = new WGRegionCommandWrapper(config, CommandUtils.getCommands(server).get("region"));
		CommandUtils.replaceCommand(server, wrapper.originalCmd, wrapper);
	}

	public static void uninject(Server server) {
		WGRegionCommandWrapper wrapper = (WGRegionCommandWrapper) CommandUtils.getCommands(server).get("region");
		CommandUtils.replaceCommand(server, wrapper, wrapper.originalCmd);
	}

	protected final Config config;
	protected final Messages msg;
	protected final Command originalCmd;
	protected final WGClaimCommand claimHandler;

	protected WGRegionCommandWrapper(Config config, Command originalCmd) {
		super(originalCmd.getName(), originalCmd.getDescription(), originalCmd.getUsage(), originalCmd.getAliases());
		this.config = config;
		this.msg = config.getMessages();
		this.originalCmd = originalCmd;
		this.claimHandler = new WGClaimCommand(config);
	}

	private final BlockLimits blockLimits = new BlockLimits();

	@Override
	public boolean execute(CommandSender sender, String label, String[] args) {
		if ((sender instanceof Player player) && (args.length >= 2) && args[0].equalsIgnoreCase("claim")) {
            String regionName = args[1];
			if (config.claimExpandSelectionVertical) {
				boolean result = WEUtils.expandVert(player);
				if (result) {
					msg.sendMessage(player, MKey.CLAIM__AUTO_VERT);
				}
			}
			if (!process(player)) {
				return true;
			}
			boolean hasRegion = AutoFlags.hasRegion(player.getWorld(), regionName);
			try {
				claimHandler.claim(regionName, sender);
				if (!hasRegion && config.claimAutoFlagsEnabled) {
					AutoFlags.setFlagsForRegion(WGRegionUtils.wrapAsPrivileged(player, config.showAutoFlagMessages), player.getWorld(), config, regionName);
				}
			} catch (CommandException ex) {
				msg.sendMessage(sender, MKey.CLAIM__ERROR__FORMAT, ex.getMessage());
			}
			return true;
		} else {
			return originalCmd.execute(sender, label, args);
		}
	}

	private boolean process(Player player) {
		BlockLimits.ProcessedClaimInfo info = blockLimits.processClaimInfo(config, player);
		return switch (info.result()) {
			case ALLOW -> true;
			case DENY_MAX_VOLUME -> {
				msg.sendMessage(player, MKey.CLAIM__ERROR__DENY_MAX_VOLUME, info.assignedLimit(), info.assignedSize());
				yield false;
			}
			case DENY_MIN_VOLUME -> {
				msg.sendMessage(player, MKey.CLAIM__ERROR__DENY_MIN_VOLUME, info.assignedLimit(), info.assignedSize());
				yield false;
			}
			case DENY_HORIZONTAL -> {
				msg.sendMessage(player, MKey.CLAIM__ERROR__DENY_HORIZONTAL, info.assignedLimit(), info.assignedSize());
				yield false;
			}
			case DENY_VERTICAL -> {
				msg.sendMessage(player, MKey.CLAIM__ERROR__DENY_VERTICAL, info.assignedLimit(), info.assignedSize());
				yield false;
			}
		};
	}
}
