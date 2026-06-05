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
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import wgextender.WGExtender;
import wgextender.config.Config;
import wgextender.config.message.MKey;
import wgextender.config.message.Messages;
import wgextender.utils.CommandsUtils;
import wgextender.utils.WEUtils;
import wgextender.utils.WGUtils;

import java.util.Map;

public final class WGRegionCommandWrapper extends Command {
	public static void inject(WGExtender plugin) {
		WGRegionCommandWrapper wrapper = new WGRegionCommandWrapper(plugin, CommandsUtils.getCommands(plugin.getServer()).get("region"));
		CommandsUtils.replaceCommand(plugin.getServer(), wrapper.originalCmd, wrapper);
	}

	public static void uninject(Server server) {
		WGRegionCommandWrapper wrapper = (WGRegionCommandWrapper) CommandsUtils.getCommands(server).get("region");
		CommandsUtils.replaceCommand(server, wrapper, wrapper.originalCmd);
	}

	private final Config config;
	private final Messages msg;
	private final BlockLimitsHandler limits;

	private final Command originalCmd;
	private final WGClaimSubcommand claimSubcommand;

	private WGRegionCommandWrapper(WGExtender plugin, Command originalCmd) {
		super(originalCmd.getName(), originalCmd.getDescription(), originalCmd.getUsage(), originalCmd.getAliases());
		this.config = plugin.getPluginConfig();
		this.msg = config.getMessages();
		this.limits = plugin.getBlockLimitsHandler();
		this.originalCmd = originalCmd;
		this.claimSubcommand = new WGClaimSubcommand(config);
	}

	@Override
	public boolean execute(@NonNull CommandSender sender, @NonNull String label, @NotNull String @NonNull [] args) {
		if (sender instanceof Player player && args.length >= 2 && args[0].equalsIgnoreCase("claim")) {
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
			boolean hasRegion = WGUtils.hasRegion(player.getWorld(), regionName);
			try {
				claimSubcommand.claim(regionName, sender);
				if (!hasRegion && config.claimAutoFlagsEnabled) {
					Actor actor = WEUtils.privilegedActor(player, config.showAutoFlagMessages);
					World world = player.getWorld();
					final ProtectedRegion rg = WGUtils.getRegion(world, regionName);
					if (rg != null) {
						for (Map.Entry<Flag<?>, String> entry : config.claimAutoFlags.entrySet()) {
							try {
								WGUtils.setFlagNaturally(actor, world, rg, entry.getKey(), entry.getValue());
							} catch (CommandException e) {
								e.printStackTrace(); // TODO Handle properly
							}
						}
					}

				}
			} catch (CommandException ex) {
				msg.sendMessage(sender, MKey.CLAIM__ERROR__FORMAT, ex.getMessage());
			}
			return true;
		} else {
			return originalCmd.execute(sender, label, args);
		}
	}

	private boolean process(@NotNull Player player) {
		BlockLimitsHandler.EvaluationResult info = limits.evaluateBlocksLimit(player);
		return switch (info.type()) {
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
