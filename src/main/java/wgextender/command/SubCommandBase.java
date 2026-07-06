package wgextender.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Server;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import wgextender.WGExtender;
import wgextender.config.ConfigurationProvider;
import wgextender.config.message.MessagesProvider;

import static wgextender.command.Brigadierable.cmd;

@ApiStatus.Internal
public abstract class SubCommandBase implements Brigadierable {
    protected final WGExtender plugin;
    protected final Server server;
    protected final ConfigurationProvider cfgProvider;
    protected final MessagesProvider msg;
    private final String name;

    protected SubCommandBase(@NotNull WGExtender plugin, @NotNull String name) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.cfgProvider = plugin.getConfigurationProvider();
        this.msg = cfgProvider.messageProvider();
        this.name = name;
    }

    protected @NotNull String name() {
        return name;
    }

    protected @NotNull String permission() {
        return "wgextender.admin." + name;
    }

    protected @NotNull LiteralArgumentBuilder<CommandSourceStack> root() {
        return Commands.literal(name).requires(source -> source.getSender().hasPermission(permission()));
    }

    public abstract static class Simple extends SubCommandBase {
        protected Simple(@NotNull WGExtender plugin, @NotNull String name) {
            super(plugin, name);
        }

        @Override
        public final @NotNull LiteralArgumentBuilder<CommandSourceStack> node() {
            return root().executes(cmd(this::execute));
        }

        protected abstract void execute(@NotNull CommandContext<CommandSourceStack> ctx);
    }
}
