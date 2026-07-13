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

import com.google.common.base.Strings;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissionsException;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.BukkitWorldConfiguration;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.internal.permission.RegionPermissionModel;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
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
import wgextender.config.message.MessagesProvider;
import wgextender.utils.WEUtils;
import wgextender.utils.WGUtils;
import wgextender.utils.command.CommandWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("deprecation") // WE's Command API, which is used by WG all over the place, is deprecated
public final class WGRegionCommandWrapper extends CommandWrapper {
    private final MessagesProvider msg;
    private final BlockLimitsHandler limits;

    private ConfigurationProvider.Claim claimCfg;
    private ConfigurationProvider.AutoFlags autoFlagsCfg;

    public WGRegionCommandWrapper(@NotNull WGExtender plugin) {
        super("region");
        ConfigurationProvider config = plugin.getConfigurationProvider();
        this.msg = config.messageProvider();
        this.limits = plugin.getBlockLimitsHandler();
        this.claimCfg = config.claimCfg();
        this.autoFlagsCfg = config.autoFlagsCfg();
        config.register(section -> this.claimCfg = section, ConfigurationProvider.Claim.SECTION);
        config.register(section -> this.autoFlagsCfg = section, ConfigurationProvider.AutoFlags.SECTION);
    }

    @Override
    public boolean execute(@NonNull CommandSender sender, @NonNull String label, @NotNull String @NonNull [] args, @NotNull Command originalCmd) {
        if (sender instanceof Player player && args.length >= 2 && args[0].equalsIgnoreCase("claim")) {
            String regionName = args[1];
            if (claimCfg.expandSelectionVertical() && WEUtils.expandVert(player)) {
                msg.sendMessage(player, MKey.CLAIM__AUTO_VERT);
            }
            if (!processLimits(player)) {
                return true;
            }
            boolean hasRegion = WGUtils.hasRegion(player.getWorld(), regionName);
            try {
                if (claimCfg.hijackHandler()) {
                    hijackClaim(regionName, sender);
                } else {
                    if (regionName.startsWith("-")) { // Not handled by WG
                        throw new CommandException(msg.get(MKey.CLAIM__ERROR__RESTRICTED_SYMBOLS, regionName));
                    }
                    // Not using `originalCmd` because we still want to catch the exceptions
                    WGUtils.REGION_COMMANDS.claim(new CommandContext(new String[] {regionName}), WGUtils.wgSender(sender));
                }

                if (!hasRegion && autoFlagsCfg.enabled()) {
                    Actor actor = WEUtils.privilegedActor(player, autoFlagsCfg.showMessages());
                    World world = player.getWorld();
                    ProtectedRegion rg = WGUtils.getRegion(world, regionName);
                    if (rg == null) return true;
                    List<CommandException> exceptions = new ArrayList<>(0);
                    for (Map.Entry<Flag<?>, String> entry : autoFlagsCfg.flags().entrySet()) {
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
                        CommandException ex = new CommandException();
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

    private boolean processLimits(@NotNull Player player) {
        BlockLimitsHandler.EvaluationResult info = limits.evaluateResult(player);
        if (info.type() == BlockLimitsHandler.ResultType.ALLOW) return true;
        msg.sendMessage(player, switch (info.type()) {
            case DENY_MAX_VOLUME -> MKey.CLAIM__ERROR__DENY_MAX_VOLUME;
            case DENY_MIN_VOLUME -> MKey.CLAIM__ERROR__DENY_MIN_VOLUME;
            case DENY_HORIZONTAL -> MKey.CLAIM__ERROR__DENY_HORIZONTAL;
            case DENY_VERTICAL -> MKey.CLAIM__ERROR__DENY_VERTICAL;
            default -> throw new IllegalStateException("Unexpected value: " + info.type());
        }, info.assignedLimit(), info.assignedSize());
        return false;
    }

    /**
     * <a href="https://github.com/EngineHub/WorldGuard/blob/d1a3193754280a633d901901313fd326905dbcd9/worldguard-core/src/main/java/com/sk89q/worldguard/commands/region/RegionCommands.java#L248">Original code</a>
     */
    private void hijackClaim(String id, CommandSender sender) throws CommandException {
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

        RegionManager manager = checkRegionManager(player.getWorld());

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

        if (!Strings.isNullOrEmpty(wcfg.setParentOnClaim)) {
            ProtectedRegion templateRegion = manager.getRegion(wcfg.setParentOnClaim);
            if (templateRegion != null) {
                try {
                    region.setParent(templateRegion);
                } catch (ProtectedRegion.CircularInheritanceException e) {
                    throw new CommandException(e.getMessage());
                }
            }
        }

        region.getOwners().addPlayer(localPlayer);
        manager.addRegion(region);

        msg.sendMessage(sender, MKey.CLAIM__SUCCESS, id);
    }

    private @NotNull ProtectedRegion createProtectedRegionFromSelection(@NotNull Player player, @NotNull String id) throws CommandException {
        try {
            Region selection = WEUtils.getSelection(player);
            if (selection instanceof CuboidRegion) {
                return new ProtectedCuboidRegion(id, selection.getMinimumPoint(), selection.getMaximumPoint());
            } else if (selection instanceof Polygonal2DRegion polySel) {
                return new ProtectedPolygonalRegion(id, polySel.getPoints(), polySel.getMinimumY(), polySel.getMaximumY());
            } else {
                throw new CommandException(msg.get(MKey.CLAIM__ERROR__WRONG_SHAPE));
            }
        } catch (IncompleteRegionException e) {
            throw new CommandException(msg.get(MKey.CLAIM__ERROR__INCOMPLETE));
        }
    }

    private @NotNull RegionManager checkRegionManager(World bukkitWorld) throws CommandException {
        var world = WEUtils.weWorld(bukkitWorld);
        if (!WGUtils.getPlatform().getGlobalStateManager().get(world).useRegions) {
            throw new CommandException(msg.get(MKey.COMMON__ERROR__WORLD_DISABLED, world.getName()));
        }

        RegionManager manager = WGUtils.getPlatform().getRegionContainer().get(world);
        if (manager == null) {
            throw new CommandException(msg.get(MKey.CLAIM__ERROR__REGION_DATA_FAIL));
        }
        return manager;
    }
}