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

package wgextender.utils.command;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.jetbrains.annotations.NotNull;
import wgextender.utils.CaseInsensitive;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class CommandsUtils {
	public static final Pattern SPACE_PATTERN = Pattern.compile("\\s+");

	private CommandsUtils() { }

	public static @NotNull Map<String, Command> getCommands(@NotNull Server server) {
		return server.getCommandMap().getKnownCommands();
	}

	public static @NotNull List<String> getCommandAliases(@NotNull Server server, @NotNull String commandName) {
		Command command = getCommands(server).get(commandName.toLowerCase(Locale.ROOT));
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

	public static void replaceCommand(@NotNull Server server, @NotNull Command oldCommand, @NotNull Command newCommand) {
		String cmdName = oldCommand.getName();
		var commandMap = getCommands(server);
		if (oldCommand.equals(commandMap.get(cmdName))) {
			commandMap.put(cmdName, newCommand);
		}
		for (String alias : oldCommand.getAliases()) {
			if (oldCommand.equals(commandMap.get(alias))) {
				commandMap.put(alias, newCommand);
			}
		}
	}

	public static @NotNull Predicate<String> computeVariants(
			@NotNull Collection<String> commands,
			@NotNull Function<String, Iterable<String>> aliases
	) {
		Set<String> variants = CaseInsensitive.newSet();
		processCommands(commands, aliases, variants::add);
		return variants::contains;
	}

	public static @NotNull Predicate<String> computePrefixedVariants(
			@NotNull Collection<String> commands,
			@NotNull Function<String, Iterable<String>> aliases
	) {
		ArgNode root = new ArgNode();
		processCommands(commands, aliases, command -> insertChildren(root, command));
		return input -> {
			ArgNode node = root;
			int index = 0;
			while (index < input.length()) {
				int end = input.indexOf(' ', index);
				if (end == -1) end = input.length();
				node = node.children.get(input.substring(index, end));
				if (node == null) return false;
				if (node.end) return true;
				index = end + 1;
			}
			return false;
		};
	}

	private static void processCommands(
			@NotNull Collection<String> commands,
			@NotNull Function<String, Iterable<String>> aliases,
			@NotNull Consumer<String> addVariant
	) {
		for (String command : commands) {
			String[] split = SPACE_PATTERN.split(command, 2);
			String suffix = split.length > 1 ? " " + split[1] : "";
			addVariant.accept(command);
			for (String alias : aliases.apply(split[0])) {
				addVariant.accept(alias + suffix);
			}
		}
	}

	private static void insertChildren(@NotNull ArgNode node, @NotNull String command) {
		for (String word : SPACE_PATTERN.split(command)) {
			node = node.children.computeIfAbsent(word, key -> new ArgNode());
		}
		node.end = true;
	}

	private static class ArgNode {
		final Map<String, ArgNode> children = CaseInsensitive.newMap();
		boolean end;
	}
}
