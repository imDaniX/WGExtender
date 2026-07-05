package wgextender.command.impl;

import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import wgextender.WGExtender;
import wgextender.command.SubCommandBase;
import wgextender.config.message.MKey;
import wgextender.external.updater.ModrinthUpdater;

final class UpdateSubCommand extends SubCommandBase.Simple {
    UpdateSubCommand(@NotNull WGExtender plugin) {
        super(plugin, "update");
    }

    @Override
    protected void execute(@NotNull CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        msg.sendMessage(sender, MKey.WGEX_COMMAND__UPDATE__START);
        server.getAsyncScheduler().runNow(plugin, task -> {
            ModrinthUpdater.Result result = plugin.getUpdater().checkForUpdate(cfgProvider.updater().allowStaging());
            switch (result) {
                case ModrinthUpdater.Result.Failure failure -> {
                    msg.sendMessage(sender, MKey.WGEX_COMMAND__UPDATE__FAILURE, failure.cause().getMessage());
                    if (!sender.equals(server.getConsoleSender()) && plugin.getConfigurationProvider().updater().logFailures()) {
                        plugin.logger().error(
                                msg.rich(MKey.WGEX_COMMAND__UPDATE__FAILURE, failure.cause().getMessage()),
                                failure.cause()
                        );
                    }
                }
                case ModrinthUpdater.Result.Success success -> { switch (success.status()) {
                        case AHEAD -> msg.sendMessage(
                                sender,
                                MKey.WGEX_COMMAND__UPDATE__AHEAD,
                                success.currentRaw(), success.latestRaw()
                        );
                        case UP_TO_DATE -> msg.sendMessage(
                                sender,
                                MKey.WGEX_COMMAND__UPDATE__UP_TO_DATE,
                                success.currentRaw()
                        );
                        case AVAILABLE -> msg.sendMessage(
                                sender,
                                MKey.WGEX_COMMAND__UPDATE__AVAILABLE,
                                success.currentRaw(), success.latestRaw(), success.latestFile().versionType()
                        );
                }}
            }
        });
    }
}
