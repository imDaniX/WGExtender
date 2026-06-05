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

package wgextender.utils;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.regex.Pattern;

public final class CommandUtils {
	private static final Pattern SPACE_PATTERN = Pattern.compile("\\s+");

	private CommandUtils() { }

	public static @NotNull Map<String, Command> getCommands(@NotNull Server server) {
		return server.getCommandMap().getKnownCommands();
	}

	public static @NotNull List<String> getCommandAliases(@NotNull Server server, @NotNull String commandName) {
		Command command = getCommands(server).get(commandName);
		if (command == null) {
			return Collections.singletonList(commandName);
		} else {
			List<String> aliases = new ArrayList<>();
			aliases.add(commandName);
			for (Entry<String, Command> entry : getCommands(server).entrySet()) {
				if (entry.getValue().equals(command)) {
					aliases.add(entry.getKey());
				}
			}
			return aliases;
		}
	}

	public static @NotNull Collection<String> computeAliasedVariants(
			@NotNull Collection<String> commands,
			@NotNull Function<String, Iterable<String>> aliases
	) {
		Set<String> computedCommands = new HashSet<>();
		for (String commandBase : commands) {
			String[] split = SPACE_PATTERN.split(commandBase, 2);
			String toAdd = split.length > 1 ? split[1] : "";
			for (String alias : aliases.apply(split[0].toLowerCase(Locale.ROOT))) {
				computedCommands.add(alias + toAdd);
			}
		}
		return computedCommands;
	}

	public static void replaceCommand(@NotNull Server server, @NotNull Command oldCommand, @NotNull Command newCommand) {
		String cmdName = oldCommand.getName();
		var commandMap = getCommands(server);
		if (commandMap.get(cmdName).equals(oldCommand)) {
			commandMap.put(cmdName, newCommand);
		}
		for (String alias : oldCommand.getAliases()) {
			if (commandMap.get(alias).equals(oldCommand)) {
				commandMap.put(alias, newCommand);
			}
		}
	}
}
