package wgextender.features.flags;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.jetbrains.annotations.NotNull;
import wgextender.config.Config;
import wgextender.config.message.MKey;
import wgextender.features.ConfigurableListenerBase;
import wgextender.utils.WGUtils;

public final class ChorusFruitFlagHandler extends ConfigurableListenerBase {
	public ChorusFruitFlagHandler(@NotNull Config config) {
		super(config);
	}

	@EventHandler(ignoreCancelled = true)
	public void onItemUse(PlayerItemConsumeEvent event) {
		if (event.getItem().getType() == Material.CHORUS_FRUIT) {
			Player player = event.getPlayer();
			if (
				!WGUtils.canBypassProtection(event.getPlayer()) &&
				!WGUtils.isFlagAllows(player, player.getLocation(), WGExtenderFlags.CHORUS_FRUIT_USE_FLAG)
			) {
				msg.sendMessage(player, MKey.FLAGS__CHORUS_RESTRICTED);
				event.setCancelled(true);
			}
		}
	}

}
