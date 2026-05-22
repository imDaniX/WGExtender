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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CommandUtils {
	public static Map<String, Command> getCommands(Server server) {
		return server.getCommandMap().getKnownCommands();
	}

	public static List<String> getCommandAliases(Server server, String commandName) {
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

	public static void replaceCommand(Server server, Command oldCommand, Command newCommand) {
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
