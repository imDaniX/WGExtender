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

package wgextender;

import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.BooleanFlag;
import com.sk89q.worldguard.protection.flags.EnumFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import wgextender.config.ConfigurationProvider;
import wgextender.config.message.MKey;
import wgextender.config.message.Messages;
import wgextender.utils.Transform;
import wgextender.utils.WEUtils;
import wgextender.utils.WGUtils;

import java.math.BigInteger;
import java.util.*;

import static org.bukkit.util.StringUtil.copyPartialMatches;
import static wgextender.utils.WGUtils.getWorldGuard;

// TODO Brigadier?
public final class WGExCommand implements CommandExecutor, TabCompleter {
    private final WGExtender plugin;
    private final Server server;
    private final ConfigurationProvider cfgProvider;
    private final Messages msg;

    public WGExCommand(@NotNull WGExtender plugin) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.cfgProvider = plugin.getConfigurationProvider();
        this.msg = cfgProvider.messages();
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command arg1, @NotNull String label, String @NotNull [] args) {
        if (!sender.hasPermission("wgextender.admin")) {
            msg.sendMessage(sender, MKey.COMMON__ERROR__NO_PERMISSION);
            return true;
        }
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help" -> {
                showHelp(sender);
                return true;
            }
            case "reload" -> {
                cfgProvider.reload();
                msg.sendMessage(sender, MKey.WGEX_COMMAND__RELOAD__SUCCESS);
                return true;
            }
            case "search" -> {
                if (!(sender instanceof Player player)) {
                    msg.sendMessage(sender, MKey.COMMON__ERROR__PLAYER_ONLY);
                    return true;
                }
                try {
                    Region psel = WEUtils.getSelection(player);
                    ProtectedRegion fakeRg = new ProtectedCuboidRegion("wgexfakerg", psel.getMaximumPoint(), psel.getMinimumPoint());
                    ApplicableRegionSet ars = WGUtils.getRegionManager(player.getWorld()).getApplicableRegions(fakeRg);
                    List<String> regions = new ArrayList<>();
                    for (ProtectedRegion ar : ars) {
                        String id = ar.getId();
                        regions.add(id);
                    }
                    if (regions.isEmpty()) {
                        msg.sendMessage(sender, MKey.WGEX_COMMAND__SEARCH__NOT_FOUND);
                    } else {
                        msg.sendMessage(sender, MKey.WGEX_COMMAND__SEARCH__FOUND, regions);
                    }
                } catch (IncompleteRegionException e) {
                    msg.sendMessage(sender, MKey.WGEX_COMMAND__SEARCH__INCOMPLETE_SELECTION);
                }
                return true;
            }
            case "setflag" -> {
                if (args.length < 4) {
                    msg.sendMessage(sender, MKey.WGEX_COMMAND__SETFLAG__HELP);
                    return true;
                }
                World world = server.getWorld(args[1]);
                if (world == null) {
                    msg.sendMessage(sender, MKey.WGEX_COMMAND__SETFLAG__WORLD_NOT_FOUND);
                    return true;
                }
                Flag<?> flag = WGUtils.matchFlag(args[2]);
                if (flag == null) {
                    msg.sendMessage(sender, MKey.WGEX_COMMAND__SETFLAG__FLAG_NOT_FOUND);
                    return true;
                }
                try {
                    String value = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                    for (ProtectedRegion region : WGUtils.getRegionManager(world).getRegions().values()) {
                        if (region instanceof GlobalProtectedRegion) {
                            continue;
                        }
                        WGUtils.setFlagNaturally(WEUtils.privilegedActor(sender, false), world, region, flag, value);
                    }
                    msg.sendMessage(sender, MKey.WGEX_COMMAND__SETFLAG__SUCCESS);
                } catch (CommandException e) {
                    msg.sendMessage(sender, MKey.WGEX_COMMAND__SETFLAG__INVALID_FORMAT, flag.getName(), e.getMessage());
                }
                return true;
            }
            case "removeowner", "removemember" -> {
                boolean owner = args[0].equalsIgnoreCase("removeowner");
                if (args.length != 2) {
                    msg.sendMessage(sender, owner
                            ? MKey.WGEX_COMMAND__REMOVEOWNER__HELP
                            : MKey.WGEX_COMMAND__REMOVEMEMBER__HELP
                    );
                    return true;
                }
                OfflinePlayer offPlayer = server.getOfflinePlayer(args[1]);
                String name = (offPlayer.getName() == null ? args[1] : offPlayer.getName()).toLowerCase(Locale.ROOT);
                UUID uuid = offPlayer.getUniqueId();
                for (RegionManager manager : WGUtils.getRegionContainer().getLoaded()) {
                    for (ProtectedRegion region : manager.getRegions().values()) {
                        DefaultDomain domain = owner ? region.getOwners() : region.getMembers();
                        domain.removePlayer(uuid);
                        domain.removePlayer(name);
                        if (owner) {
                            region.setOwners(domain);
                        } else {
                            region.setMembers(domain);
                        }
                    }
                }
                msg.sendMessage(sender, owner
                        ? MKey.WGEX_COMMAND__REMOVEOWNER__SUCCESS
                        : MKey.WGEX_COMMAND__REMOVEMEMBER__SUCCESS
                );
                return true;
            }
            case "limits" -> {
                if (args.length < 2 || args[1].equalsIgnoreCase("help")) {
                    msg.sendMessage(sender, MKey.WGEX_COMMAND__LIMITS__REFRESH__HELP);
                    msg.sendMessage(sender, MKey.WGEX_COMMAND__LIMITS__CLEAR__HELP);
                    return true;
                }
                switch (args[1].toLowerCase(Locale.ROOT)) {
                    case "refresh" -> {
                        if (args.length < 3) {
                            msg.sendMessage(sender, MKey.WGEX_COMMAND__LIMITS__REFRESH__HELP);
                            return true;
                        }
                        boolean silent = args[args.length - 1].equalsIgnoreCase("-s");
                        String playerName = silent ? args[2] : args[args.length - 1];
                        if (silent && args.length == 3) {
                            msg.sendMessage(sender, MKey.WGEX_COMMAND__LIMITS__REFRESH__HELP);
                            return true;
                        }
                        Player target = server.getPlayer(playerName);
                        if (target == null) {
                            if (!silent) {
                                msg.sendMessage(sender, MKey.COMMON__ERROR__PLAYER_NOT_FOUND);
                            }
                            return true;
                        }
                        BigInteger limit = plugin.getBlockLimitsHandler().refreshBlockLimit(target);
                        if (!silent) {
                            msg.sendMessage(sender, MKey.WGEX_COMMAND__LIMITS__REFRESH__SUCCESS, target.getName(), limit);
                        }
                        return true;
                    }
                    case "clear" -> {
                        boolean silent = args.length > 2 && args[args.length - 1].equalsIgnoreCase("-s");
                        plugin.getBlockLimitsHandler().clearCache();
                        if (!silent) {
                            msg.sendMessage(sender, MKey.WGEX_COMMAND__LIMITS__CLEAR__SUCCESS);
                        }
                        return true;
                    }
                    default -> {
                        msg.sendMessage(sender, MKey.WGEX_COMMAND__UNKNOWN_SUBCOMMAND, args[0] + " " + args[1]);
                        msg.sendMessage(sender, MKey.WGEX_COMMAND__LIMITS__REFRESH__HELP);
                        msg.sendMessage(sender, MKey.WGEX_COMMAND__LIMITS__CLEAR__HELP);
                        return true;
                    }
                }
            }
            default -> {
                msg.sendMessage(sender, MKey.WGEX_COMMAND__UNKNOWN_SUBCOMMAND, args[0]);
                showHelp(sender);
                return true;
            }
        }
    }

    private void showHelp(CommandSender sender) {
        msg.sendMessage(sender, MKey.WGEX_COMMAND__RELOAD__HELP);
        msg.sendMessage(sender, MKey.WGEX_COMMAND__SEARCH__HELP);
        msg.sendMessage(sender, MKey.WGEX_COMMAND__SETFLAG__HELP);
        msg.sendMessage(sender, MKey.WGEX_COMMAND__REMOVEOWNER__HELP);
        msg.sendMessage(sender, MKey.WGEX_COMMAND__REMOVEMEMBER__HELP);
        msg.sendMessage(sender, MKey.WGEX_COMMAND__LIMITS__HELP);
    }

    private static final List<String> PLAYER_ARGS = List.of("help", "reload", "search", "setflag", "removeowner", "removemember", "limits");
    private static final List<String> CONSOLE_ARGS = List.of("help", "reload", "setflag", "removeowner", "removemember", "limits");

    private static final List<String> STATE_ARGS = List.of("ALLOW", "DENY");
    private static final List<String> BOOLEAN_ARGS = List.of("true", "false");
    private static final List<String> LIMITS_ARGS = List.of("refresh", "clear");

    private static final List<String> SILENT_ARG = List.of("-s");

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length == 0 || !sender.hasPermission("wgextender.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return copyPartialMatches(
                    args[0],
                    sender instanceof Player ? PLAYER_ARGS : CONSOLE_ARGS,
                    new ArrayList<>()
            );
        }
        if (args[0].equalsIgnoreCase("limits")) {
            if (args.length == 2) {
                return copyPartialMatches(args[1], LIMITS_ARGS, new ArrayList<>());
            }
            if (args[1].equalsIgnoreCase("refresh")) {
                if (args.length == 3) {
                    return null;
                }
                if (args.length == 4) {
                    return copyPartialMatches(args[3], SILENT_ARG, new ArrayList<>());
                }
            }
            if (args[1].equalsIgnoreCase("clear")) {
                if (args.length == 3) {
                    return copyPartialMatches(args[2], SILENT_ARG, new ArrayList<>());
                }
            }
            return List.of();
        }
        if (!args[0].equalsIgnoreCase("setflag")) return List.of();
        return switch (args.length) {
            case 2 -> copyPartialMatches(args[1], Transform.toList(server.getWorlds(), World::getName), new ArrayList<>());
            case 3 -> copyPartialMatches(args[2], Transform.toList(getWorldGuard().getFlagRegistry(), Flag::getName), new ArrayList<>());
            case 4 -> {
                Flag<?> flag = WGUtils.matchFlag(args[2]);
                if (flag instanceof StateFlag) {
                    yield copyPartialMatches(args[3], STATE_ARGS, new ArrayList<>());
                }
                if (flag instanceof BooleanFlag) {
                    yield copyPartialMatches(args[3], BOOLEAN_ARGS, new ArrayList<>());
                }
                if (flag instanceof EnumFlag<?> enumFlag) {
                    try {
                        yield copyPartialMatches(args[3], Transform.toList(enumFlag.getEnumClass().getEnumConstants(), Enum::toString), new ArrayList<>());
                    } catch (Exception ignored) { }
                }
                yield List.of();
            }
            default -> List.of();
        };
    }
}