package wgextender.features.flags;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import wgextender.utils.WGRegionUtils;

public class ChorusFruitFlagHandler implements Listener {

	@EventHandler(ignoreCancelled = true)
	public void onItemUse(PlayerItemConsumeEvent event) {
		if (event.getItem().getType() == Material.CHORUS_FRUIT) {
			Player player = event.getPlayer();
			if (
				!WGRegionUtils.canBypassProtection(event.getPlayer()) &&
				!WGRegionUtils.isFlagAllows(player, player.getLocation(), WGExtenderFlags.CHORUS_FRUIT_USE_FLAG)
			) {
				player.sendMessage(ChatColor.RED + "Вы не можете использовать хорус в этом регионе");
				event.setCancelled(true);
			}
		}
	}

}
