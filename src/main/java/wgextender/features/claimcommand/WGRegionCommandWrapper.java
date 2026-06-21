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
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import wgextender.WGExtender;
import wgextender.config.ConfigurationProvider;
import wgextender.config.message.MKey;
import wgextender.config.message.Messages;
import wgextender.utils.WEUtils;
import wgextender.utils.WGUtils;
import wgextender.utils.command.CommandWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class WGRegionCommandWrapper extends CommandWrapper {
	private final Messages msg;
	private final BlockLimitsHandler limits;
	private final WGClaimSubcommand claimSubcommand;

	private ConfigurationProvider.Claim claim;
	private ConfigurationProvider.AutoFlags autoFlags;

	public WGRegionCommandWrapper(@NotNull WGExtender plugin) {
		super("region");
		ConfigurationProvider config = plugin.getConfigurationProvider();
		this.msg = config.messages();
		this.limits = plugin.getBlockLimitsHandler();
		this.claimSubcommand = new WGClaimSubcommand(config);
		this.claim = config.claim();
		this.autoFlags = config.autoFlags();
		config.register(section -> this.claim = section, ConfigurationProvider.Claim.SECTION);
		config.register(section -> this.autoFlags = section, ConfigurationProvider.AutoFlags.SECTION);
	}

	@Override
	public boolean execute(@NonNull CommandSender sender, @NonNull String label, @NotNull String @NonNull [] args, @NotNull Command originalCmd) {
		if (sender instanceof Player player && args.length >= 2 && args[0].equalsIgnoreCase("claim")) {
			String regionName = args[1];
			if (claim.expandSelectionVertical()) {
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
				if (!hasRegion && autoFlags.enabled()) {
					Actor actor = WEUtils.privilegedActor(player, autoFlags.showMessages());
					World world = player.getWorld();
					final ProtectedRegion rg = WGUtils.getRegion(world, regionName);
                    if (rg == null) return true;
					List<CommandException> exceptions = new ArrayList<>(0);
                    for (Map.Entry<Flag<?>, String> entry : autoFlags.flags().entrySet()) {
                        try {
                            WGUtils.setFlagNaturally(actor, world, rg, entry.getKey(), entry.getValue());
                        } catch (CommandException ex) {
							exceptions.add(ex);
                        }
                    }
					if (!exceptions.isEmpty()) {
						if (exceptions.size() == 1) {
							throw exceptions.getFirst();
						}
						CommandException ex = new CommandException(); // TODO Handle it better?
						exceptions.forEach(ex::addSuppressed);
						throw ex;
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