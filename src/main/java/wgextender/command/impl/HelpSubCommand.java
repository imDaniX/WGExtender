package wgextender.command.impl;

import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import wgextender.WGExtender;
import wgextender.command.SubCommandBase;
import wgextender.config.message.MKey;
import wgextender.config.message.Messages;

final class HelpSubCommand extends SubCommandBase.Simple {
    HelpSubCommand(@NotNull WGExtender plugin) {
        super(plugin, "help");
    }

    @Override
    protected void execute(@NotNull CommandContext<CommandSourceStack> ctx) {
        sendHelp(ctx.getSource().getSender(), msg);
    }

    static void sendHelp(@NotNull CommandSender sender, @NotNull Messages msg) {
        msg.sendMessage(sender, MKey.WGEX_COMMAND__RELOAD__HELP);
        msg.sendMessage(sender, MKey.WGEX_COMMAND__SEARCH__HELP);
        msg.sendMessage(sender, MKey.WGEX_COMMAND__SETFLAG__HELP);
        msg.sendMessage(sender, MKey.WGEX_COMMAND__REMOVEOWNER__HELP);
        msg.sendMessage(sender, MKey.WGEX_COMMAND__REMOVEMEMBER__HELP);
        msg.sendMessage(sender, MKey.WGEX_COMMAND__LIMITS__HELP);
        msg.sendMessage(sender, MKey.WGEX_COMMAND__UPDATE__HELP);
    }
}
