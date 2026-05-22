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

package wgextender.commands;

import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.*;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
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
import wgextender.Config;
import wgextender.features.claimcommand.AutoFlags;
import wgextender.utils.Transform;
import wgextender.utils.WEUtils;
import wgextender.utils.WGRegionUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.bukkit.ChatColor.BLUE;
import static org.bukkit.ChatColor.RED;
import static org.bukkit.util.StringUtil.copyPartialMatches;

//TODO: refactor
public class Commands implements CommandExecutor, TabCompleter {

	protected final Server server;
	protected final Config config;

	public Commands(Server server, Config config) {
		this.server = server;
		this.config = config;
	}

	private static List<String> getRegionsInPlayerSelection(Player player) throws IncompleteRegionException {
		Region psel = WEUtils.getSelection(player);
		ProtectedRegion fakerg = new ProtectedCuboidRegion("wgexfakerg", psel.getMaximumPoint(), psel.getMinimumPoint());
		ApplicableRegionSet ars = WGRegionUtils.getRegionManager(player.getWorld()).getApplicableRegions(fakerg);
		return StreamSupport.stream(ars.spliterator(), false)
			.map(ProtectedRegion::getId)
			.collect(Collectors.toList());
	}

	@Override
	public boolean onCommand(CommandSender sender, @NotNull Command arg1, @NotNull String label, String @NotNull [] args) {
		if (!sender.hasPermission("wgextender.admin")) {
			sender.sendMessage(RED + "Недостаточно прав");
			return true;
		}
		if (args.length >= 1) {
			switch (args[0].toLowerCase()) {
				case "help" -> {
					sender.sendMessage(BLUE + "wgex reload - перезагрузить конфиг");
					sender.sendMessage(BLUE + "wgex search - ищет регионы в выделенной области");
					sender.sendMessage(BLUE + "wgex setflag {world} {flag} {value}  - устанавливает флаг {flag} со значением {value} на все регионы в мире {world}");
					sender.sendMessage(BLUE + "wgex removeowner {name} - удаляет игрока из списков владельцев всех регионов");
					sender.sendMessage(BLUE + "wgex removemember {name} - удаляет игрока из списков членов всех регионов");
					return true;
				}
				case "reload" -> {
					config.loadConfig();
					sender.sendMessage(BLUE + "Конфиг перезагружен");
					return true;
				}
				case "search" -> {
					if (sender instanceof Player player) {
						try {
							List<String> regions = getRegionsInPlayerSelection(player);
							if (regions.isEmpty()) {
								sender.sendMessage(BLUE + "Регионов пересекающихся с выделенной зоной не найдено");
							} else {
								sender.sendMessage(BLUE + "Найдены регионы пересекающиеся с выделенной зоной: " + regions);
							}
						} catch (IncompleteRegionException e) {
							sender.sendMessage(BLUE + "Сначала выделите зону поиска");
						}
						return true;
					}
					return false;
				}
				case "setflag" -> {
					if (args.length < 4) {
						return false;
					}
					World world = server.getWorld(args[1]);
					if (world == null) {
						sender.sendMessage(BLUE + "Мир не найден");
						return true;
					}
					Flag<?> flag = Flags.fuzzyMatchFlag(WorldGuard.getInstance().getFlagRegistry(), args[2]);
					if (flag == null) {
						sender.sendMessage(BLUE + "Флаг не найден");
						return true;
					}
					try {
						String value = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
						for (ProtectedRegion region : WGRegionUtils.getRegionManager(world).getRegions().values()) {
							if (region instanceof GlobalProtectedRegion) {
								continue;
							}
							AutoFlags.setFlag(WGRegionUtils.wrapAsPrivileged(sender, false), world, region, flag, value);
						}
						sender.sendMessage(BLUE + "Флаги установлены");
					} catch (CommandException e) {
						sender.sendMessage(BLUE + "Неправильный формат флага " + flag.getName() + ": " + e.getMessage());
					}
					return true;
				}
				case "removeowner", "removemember" -> {
					if (args.length != 2) {
						return false;
					}
					boolean owner = args[0].equals("removeowner");
					OfflinePlayer offPlayer = server.getOfflinePlayer(args[1]);
					String name = (offPlayer.getName() == null ? args[1] : offPlayer.getName()).toLowerCase();
					UUID uuid = offPlayer.getUniqueId();
					for (RegionManager manager : WGRegionUtils.getRegionContainer().getLoaded()) {
						for (ProtectedRegion region : manager.getRegions().values()) {
							DefaultDomain members = owner ? region.getOwners() : region.getMembers();
							members.removePlayer(uuid);
							members.removePlayer(name);
							region.setMembers(members);
						}
					}
					sender.sendMessage(BLUE + "Игрок удалён из списков " + (owner ? "владельцев" : "участников") + " всех регионов");
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
		if (args.length == 0 || !sender.hasPermission("wgextender.admin")) {
			return Collections.emptyList();
		}
		if (args.length == 1) {
			return copyPartialMatches(
					args[0],
					sender instanceof Player
							? List.of("help", "reload", "search", "setflag", "removeowner", "removemember")
							: List.of("help", "reload", "setflag", "removeowner", "removemember"),
					new ArrayList<>()
			);
		}
		if (!"setflag".equalsIgnoreCase(args[0])) return Collections.emptyList();
		switch (args.length) {
			case 2 -> {
				return copyPartialMatches(args[1], Transform.toList(server.getWorlds(), World::getName), new ArrayList<>());
			}
			case 3 -> {
				return copyPartialMatches(args[2], Transform.toList(WorldGuard.getInstance().getFlagRegistry(), Flag::getName), new ArrayList<>());
			}
			case 4 -> {
				Flag<?> flag = Flags.fuzzyMatchFlag(WorldGuard.getInstance().getFlagRegistry(), args[2]);
				if (flag instanceof StateFlag) {
					return copyPartialMatches(args[3], Transform.toList(State.values(), State::toString), new ArrayList<>());
				}
				if (flag instanceof BooleanFlag) {
					return copyPartialMatches(args[3], List.of("true", "false"), new ArrayList<>());
				}
				if (flag instanceof EnumFlag<?> enumFlag) {
					try {
						return copyPartialMatches(args[3], Transform.toList(enumFlag.getEnumClass().getEnumConstants(), Enum::toString), new ArrayList<>());
					} catch (Exception ignored) { }
				}
			}
		}
		return Collections.emptyList();
	}

}
