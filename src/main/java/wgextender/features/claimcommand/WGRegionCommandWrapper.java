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
import com.sk89q.minecraft.util.commands.CommandPermissionsException;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.BukkitWorldConfiguration;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.commands.task.RegionAdder;
import com.sk89q.worldguard.internal.permission.RegionPermissionModel;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.util.DomainInputResolver;
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

	private ConfigurationProvider.Claim claim;
	private ConfigurationProvider.AutoFlags autoFlags;

	public WGRegionCommandWrapper(@NotNull WGExtender plugin) {
		super("region");
		ConfigurationProvider config = plugin.getConfigurationProvider();
		this.msg = config.messages();
		this.limits = plugin.getBlockLimitsHandler();
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
				claim(regionName, sender);
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
		BlockLimitsHandler.EvaluationResult info = limits.evaluateResult(player);
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

	private void claim(String id, CommandSender sender) throws CommandException {
		if (!(sender instanceof Player player)) {
			throw new CommandException(msg.get(MKey.COMMON__ERROR__PLAYER_ONLY));
		}
		if (id.equalsIgnoreCase("__global__")) {
			throw new CommandException(msg.get(MKey.CLAIM__ERROR__GLOBAL));
		}
		if (!ProtectedRegion.isValidId(id) || id.startsWith("-")) {
			throw new CommandException(msg.get(MKey.CLAIM__ERROR__RESTRICTED_SYMBOLS, id));
		}

		BukkitWorldConfiguration wcfg = WGUtils.getWorldConfig(player);

		if (wcfg.maxClaimVolume == Integer.MAX_VALUE) {
			throw new CommandException(msg.get(MKey.CLAIM__ERROR__CONFIG_TOO_BIG));
		}

		LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
		RegionPermissionModel permModel = new RegionPermissionModel(localPlayer);

		if (!permModel.mayClaim()) {
			throw new CommandPermissionsException();
		}

		RegionManager manager = WGUtils.getRegionManager(player.getWorld());

		if (manager.hasRegion(id)) {
			throw new CommandException(msg.get(MKey.CLAIM__ERROR__ALREADY_EXISTS));
		}

		ProtectedRegion region = createProtectedRegionFromSelection(player, id);

		if (!permModel.mayClaimRegionsUnbounded()) {
			int maxRegionCount = wcfg.getMaxRegionCount(localPlayer);
			if ((maxRegionCount >= 0) && (manager.getRegionCountOfPlayer(localPlayer) >= maxRegionCount)) {
				throw new CommandException(msg.get(MKey.CLAIM__ERROR__TOO_MANY));
			}
			if (region.volume() > wcfg.maxClaimVolume) {
				throw new CommandException(msg.get(MKey.CLAIM__ERROR__TOO_BIG));
			}
		}

		ApplicableRegionSet regions = manager.getApplicableRegions(region);

		if (regions.size() > 0) {
			if (!regions.isOwnerOfAll(localPlayer)) {
				throw new CommandException(msg.get(MKey.CLAIM__ERROR__OVERLAP));
			}
		} else if (wcfg.claimOnlyInsideExistingRegions) {
			throw new CommandException(msg.get(MKey.CLAIM__ERROR__ONLY_INSIDE_OWN));
		}

		RegionAdder task = new RegionAdder(manager, region);
		task.setLocatorPolicy(DomainInputResolver.UserLocatorPolicy.UUID_ONLY);
		task.setOwnersInput(new String[] { player.getName() });
		try {
			task.call();
			msg.sendMessage(sender, MKey.CLAIM__SUCCESS, id);
		} catch (Exception e) {
			msg.sendMessage(sender, MKey.CLAIM__ERROR__EXCEPTION);
			e.printStackTrace(); // TODO Log properly
		}
	}

	private ProtectedRegion createProtectedRegionFromSelection(Player player, String id) throws CommandException {
		try {
			Region selection = WEUtils.getSelection(player);
			if (selection instanceof CuboidRegion) {
				return new ProtectedCuboidRegion(id, selection.getMinimumPoint(), selection.getMaximumPoint());
			} else {
				throw new CommandException(msg.get(MKey.CLAIM__ERROR__ONLY_CUBOID)); // TODO We still can perform some stuff
			}
		} catch (IncompleteRegionException e) {
			throw new CommandException(msg.get(MKey.CLAIM__ERROR__INCOMPLETE));
		}
	}
}