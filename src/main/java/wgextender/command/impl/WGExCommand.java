package wgextender.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jetbrains.annotations.NotNull;
import wgextender.WGExtender;
import wgextender.command.Brigadierable;
import wgextender.command.SubCommandBase;
import wgextender.config.message.MessagesProvider;

import java.util.List;

import static io.papermc.paper.command.brigadier.Commands.literal;
import static wgextender.command.Brigadierable.cmd;

// TODO Make silent flag for more subcommands
public final class WGExCommand implements Brigadierable {
    private final MessagesProvider msg;
    private final List<SubCommandBase> subCommands;

    public WGExCommand(@NotNull WGExtender plugin) {
        this.msg = plugin.getConfigurationProvider().messageProvider();
        this.subCommands = List.of(
                new HelpSubCommand(plugin),
                new ReloadSubCommand(plugin),
                new SearchSubCommand(plugin),
                // v TODO Refactor as bulk operations: more operations, simple query, confirmation
                new SetFlagSubCommand(plugin),
                new RemoveDomainSubCommands.Owner(plugin),
                new RemoveDomainSubCommands.Member(plugin),
                // ^
                new LimitsSubCommand(plugin),
                new UpdateSubCommand(plugin)
        );
    }

    @Override
    public @NotNull LiteralArgumentBuilder<CommandSourceStack> node() {
        var root = literal("wgex")
                .requires(source -> source.getSender().hasPermission("wgextender.admin"))
                .executes(cmd(ctx -> HelpSubCommand.sendHelp(ctx.getSource().getSender(), msg)));
        subCommands.forEach(subCommand -> root.then(subCommand.node()));
        return root;
    }
}
