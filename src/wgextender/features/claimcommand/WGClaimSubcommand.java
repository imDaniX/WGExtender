package wgextender.features.claimcommand;

import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissionsException;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.BukkitWorldConfiguration;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.commands.task.RegionAdder;
import com.sk89q.worldguard.internal.permission.RegionPermissionModel;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.util.DomainInputResolver.UserLocatorPolicy;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import wgextender.config.Config;
import wgextender.config.message.MKey;
import wgextender.config.message.Messages;
import wgextender.utils.WEUtils;
import wgextender.utils.WGUtils;

public final class WGClaimSubcommand {
	private final Messages msg;

	public WGClaimSubcommand(Config config) {
		this.msg = config.getMessages();
	}

	public void claim(String id, CommandSender sender) throws CommandException {
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
		task.setLocatorPolicy(UserLocatorPolicy.UUID_ONLY);
		task.setOwnersInput(new String[] { player.getName() });
		try {
			task.call();
			msg.sendMessage(sender, MKey.CLAIM__SUCCESS, id);
		} catch (Exception e) {
			msg.sendMessage(sender, MKey.CLAIM__ERROR__EXCEPTION);
			e.printStackTrace();
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
