package wgextender.command.impl;

import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import wgextender.WGExtender;
import wgextender.command.SubCommandBase;
import wgextender.config.message.MKey;
import wgextender.utils.ModrinthUpdater.CheckResult;

final class UpdateSubCommand extends SubCommandBase.Simple {
    UpdateSubCommand(@NotNull WGExtender plugin) {
        super(plugin, "update");
    }

    @Override
    protected void execute(@NotNull CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        msg.sendMessage(sender, MKey.WGEX_COMMAND__UPDATE__START);
        server.getAsyncScheduler().runNow(plugin, task -> {
            CheckResult result = plugin.getUpdater().checkForUpdate(cfgProvider.updater().allowStaging());
            switch (result) {
                case CheckResult.Failure failure -> {
                    msg.sendMessage(sender, MKey.WGEX_COMMAND__UPDATE__FAILURE, failure.cause().getMessage());
                    plugin.logger().error("Failed to fetch latest version", failure.cause());
                }
                case CheckResult.Unknown ignored -> msg.sendMessage(
                        sender,
                        MKey.WGEX_COMMAND__UPDATE__UNKNOWN
                );
                case CheckResult.Ahead ahead -> msg.sendMessage(
                        sender,
                        MKey.WGEX_COMMAND__UPDATE__AHEAD,
                        ahead.currentRaw(), ahead.latestRaw()
                );
                case CheckResult.UpToDate upToDate -> msg.sendMessage(
                        sender,
                        MKey.WGEX_COMMAND__UPDATE__UP_TO_DATE,
                        upToDate.currentRaw()
                );
                case CheckResult.Available available -> msg.sendMessage(
                        sender,
                        MKey.WGEX_COMMAND__UPDATE__AVAILABLE,
                        available.currentRaw(), available.latestRaw(), available.latestFile().versionType()
                );
            }
        });
    }
}
