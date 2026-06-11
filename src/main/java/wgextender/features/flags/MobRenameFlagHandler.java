package wgextender.features.flags;

import com.sk89q.worldguard.protection.flags.StateFlag;
import io.papermc.paper.event.player.PlayerNameEntityEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import wgextender.config.ConfigurationProvider;
import wgextender.config.message.MKey;
import wgextender.config.message.Messages;
import wgextender.utils.WGUtils;

public final class MobRenameFlagHandler implements Listener {
    private final Messages msg;

    public MobRenameFlagHandler(@NotNull ConfigurationProvider provider) {
        this.msg = provider.messages();
    }

    @EventHandler(ignoreCancelled = true)
    public void onRename(PlayerNameEntityEvent event) {
        StateFlag.State state = WGUtils.getFlagValue(event.getPlayer(), event.getEntity().getLocation(), WGExtenderFlags.MOB_RENAME_FLAG);
        if (state == StateFlag.State.DENY) {
            msg.sendMessage(event.getPlayer(), MKey.FLAGS__RENAME_RESTRICTED);
            event.setCancelled(true);
        }
    }
}
