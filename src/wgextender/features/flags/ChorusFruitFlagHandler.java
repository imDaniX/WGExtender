package wgextender.features.flags;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.jetbrains.annotations.NotNull;
import wgextender.config.message.MKey;
import wgextender.config.message.Messages;
import wgextender.utils.WGRegionUtils;

public class ChorusFruitFlagHandler implements Listener {
	private final Messages msg;

	public ChorusFruitFlagHandler(@NotNull Messages msg) {
		this.msg = msg;
	}

	@EventHandler(ignoreCancelled = true)
	public void onItemUse(PlayerItemConsumeEvent event) {
		if (event.getItem().getType() == Material.CHORUS_FRUIT) {
			Player player = event.getPlayer();
			if (
				!WGRegionUtils.canBypassProtection(event.getPlayer()) &&
				!WGRegionUtils.isFlagAllows(player, player.getLocation(), WGExtenderFlags.CHORUS_FRUIT_USE_FLAG)
			) {
				msg.sendMessage(player, MKey.FLAGS__CHORUS_RESTRICTED);
				event.setCancelled(true);
			}
		}
	}

}
