package wgextender.features.flags;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.jetbrains.annotations.NotNull;
import wgextender.config.ConfigurationProvider;
import wgextender.config.message.MKey;
import wgextender.config.message.MessagesProvider;
import wgextender.utils.WGUtils;

public final class ConsumeFlagsHandler implements Listener {
    private final MessagesProvider msg;

    public ConsumeFlagsHandler(@NotNull ConfigurationProvider provider) {
        this.msg = provider.messageProvider();
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();

        if (WGUtils.canBypassProtection(event.getPlayer())) {
            return;
        }

        if (!WGUtils.isFlagAllows(player, player.getLocation(), WGExtenderFlags.ITEM_CONSUME_FLAG)) {
            msg.sendMessage(player, MKey.FLAGS__CONSUME_RESTRICTED);
            event.setCancelled(true);
            return;
        }

        if (event.getItem().getType() == Material.CHORUS_FRUIT && !WGUtils.isFlagAllows(player, player.getLocation(), WGExtenderFlags.CHORUS_FRUIT_USE_FLAG)) {
            msg.sendMessage(player, MKey.FLAGS__CHORUS_RESTRICTED);
            event.setCancelled(true);
        }
    }
}
