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

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import wgextender.config.ConfigurationProvider;
import wgextender.config.message.MKey;
import wgextender.config.message.Messages;
import wgextender.utils.command.CommandWrapper;

public final class WEWandCommandWrapper extends CommandWrapper {
	private final Messages msg;
	private final WEWand weWand;
	private ConfigurationProvider.Misc misc;

	public WEWandCommandWrapper(@NotNull ConfigurationProvider config, @NotNull WEWand weWand) {
		super("/wand");
		this.msg = config.messages();
		this.weWand = weWand;
		this.misc = config.misc();
		config.register(section -> this.misc = section, ConfigurationProvider.Misc.SECTION);
	}

	@Override
	public boolean execute(@NotNull CommandSender sender, @NotNull String label, String @NotNull [] args, @NotNull Command originalCmd) {
		if (!misc.extendedWeWand()) {
			return originalCmd.execute(sender, label, args);
		}
		if (sender instanceof Player player) {
			player.getInventory().addItem(weWand.getWand(msg.rich(MKey.WAND__ITEM_NAME)));
			msg.sendMessage(player, MKey.WAND__GIVEN);
		}
		return true;
	}
}