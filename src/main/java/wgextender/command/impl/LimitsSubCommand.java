package wgextender.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import wgextender.WGExtender;
import wgextender.command.SubCommandBase;
import wgextender.config.message.MKey;

import java.math.BigInteger;

import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;
import static wgextender.command.Brigadierable.cmd;

final class LimitsSubCommand extends SubCommandBase {
    LimitsSubCommand(@NotNull WGExtender plugin) {
        super(plugin, "limits");
    }

    @Override
    public @NotNull LiteralArgumentBuilder<CommandSourceStack> node() {
        return root()
                .executes(cmd(ctx -> sendHelp(ctx.getSource().getSender())))
                .then(literal("help").executes(cmd(ctx -> sendHelp(ctx.getSource().getSender()))))
                .then(literal("refresh")
                        .then(argument("player", StringArgumentType.word())
                                .executes(cmd(ctx -> refresh(ctx, false)))
                                .then(literal("-s")
                                        .executes(cmd(ctx -> refresh(ctx, true)))
                                )
                        )
                )
                .then(literal("clear")
                        .executes(cmd(ctx -> clear(ctx, false)))
                        .then(literal("-s")
                                .executes(cmd(ctx -> clear(ctx, true)))
                        )
                );
    }

    private void sendHelp(@NotNull CommandSender sender) {
        msg.sendMessage(sender, MKey.WGEX_COMMAND__LIMITS__REFRESH__HELP);
        msg.sendMessage(sender, MKey.WGEX_COMMAND__LIMITS__CLEAR__HELP);
    }

    private void refresh(@NotNull CommandContext<CommandSourceStack> ctx, boolean silent) {
        CommandSender sender = ctx.getSource().getSender();
        String playerName = StringArgumentType.getString(ctx, "player");
        Player target = server.getPlayer(playerName);
        if (target == null) {
            if (!silent) {
                msg.sendMessage(sender, MKey.COMMON__ERROR__PLAYER_NOT_FOUND);
            }
            return;
        }
        BigInteger limit = plugin.getBlockLimitsHandler().refreshBlockLimit(target);
        if (!silent) {
            msg.sendMessage(sender, MKey.WGEX_COMMAND__LIMITS__REFRESH__SUCCESS, target.getName(), limit);
        }
    }

    private void clear(@NotNull CommandContext<CommandSourceStack> ctx, boolean silent) {
        plugin.getBlockLimitsHandler().clearCache();
        if (!silent) {
            msg.sendMessage(ctx.getSource().getSender(), MKey.WGEX_COMMAND__LIMITS__CLEAR__SUCCESS);
        }
    }
}
