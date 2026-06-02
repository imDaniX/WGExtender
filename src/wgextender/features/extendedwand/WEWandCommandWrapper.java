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

package wgextender.features.extendedwand;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import wgextender.config.Config;
import wgextender.config.message.MKey;
import wgextender.config.message.Messages;
import wgextender.utils.CommandUtils;

public final class WEWandCommandWrapper extends Command {
	public static void inject(Server server, Config config) {
		WEWandCommandWrapper wrapper = new WEWandCommandWrapper(config, CommandUtils.getCommands(server).get("/wand"));
		CommandUtils.replaceCommand(server, wrapper.originalCmd, wrapper);
	}

	public static void uninject(Server server) {
		WEWandCommandWrapper wrapper = (WEWandCommandWrapper) CommandUtils.getCommands(server).get("/wand");
		CommandUtils.replaceCommand(server, wrapper, wrapper.originalCmd);
	}

	private final Config config;
	private final Messages msg;
	private final Command originalCmd;

	private WEWandCommandWrapper(Config config, Command originalCmd) {
		super(originalCmd.getName(), originalCmd.getDescription(), originalCmd.getUsage(), originalCmd.getAliases());
		this.config = config;
		this.msg = config.messages();
		this.originalCmd = originalCmd;
	}

	@Override
	public boolean execute(@NonNull CommandSender sender, @NonNull String label, String @NonNull [] args) {
		if (!config.extendedWorldEditWand()) {
			return originalCmd.execute(sender, label, args);
		}
		if (sender instanceof Player player) {
			player.getInventory().addItem(WEWand.getWand(msg.rich(MKey.WAND__ITEM_NAME)));
			msg.sendMessage(player, MKey.WAND__GIVEN);
		}
		return true;
	}
}
