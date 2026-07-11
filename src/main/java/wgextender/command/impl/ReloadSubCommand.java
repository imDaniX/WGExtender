package wgextender.command.impl;

import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jetbrains.annotations.NotNull;
import wgextender.WGExtender;
import wgextender.command.SubCommandBase;
import wgextender.config.message.MKey;

final class ReloadSubCommand extends SubCommandBase.Simple {
    ReloadSubCommand(@NotNull WGExtender plugin) {
        super(plugin, "reload");
    }

    @Override
    protected void execute(@NotNull CommandContext<CommandSourceStack> ctx) {
        cfgProvider.reload();
        cfgProvider.reloadSubscribers();
        msg.sendMessage(ctx.getSource().getSender(), MKey.WGEX_COMMAND__RELOAD__SUCCESS);
    }
}
