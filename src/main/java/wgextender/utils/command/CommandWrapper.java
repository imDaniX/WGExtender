package wgextender.utils.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import wgextender.WGExtender;
import wgextender.utils.Injectable;

public abstract class CommandWrapper implements Injectable {
    protected final String originalCmdLabel;
    protected Forwarding forwardingCmd;

    protected CommandWrapper(@NotNull String originalCmdLabel) {
        this.originalCmdLabel = originalCmdLabel;
    }

    public abstract boolean execute(
            @NotNull CommandSender sender,
            @NotNull String commandLabel,
            @NonNull String @NotNull [] args,
            @NotNull Command originalCmd
    );

    @Override
    public void inject(@NotNull WGExtender plugin) {
        this.forwardingCmd = new Forwarding(CommandsUtils.getCommands(plugin.getServer()).get(originalCmdLabel));
        CommandsUtils.replaceCommand(
                plugin.getServer(),
                forwardingCmd.originalCmd,
                forwardingCmd
        );
    }

    @Override
    public void uninject(@NotNull WGExtender plugin) {
        CommandsUtils.replaceCommand(
                plugin.getServer(),
                forwardingCmd,
                forwardingCmd.originalCmd
        );
    }

    public final class Forwarding extends Command {
        private final Command originalCmd;

        public Forwarding(@NotNull Command original) {
            super(original.getName(), original.getDescription(), original.getUsage(), original.getAliases());
            this.originalCmd = original;
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NonNull String @NotNull [] args) {
            return CommandWrapper.this.execute(sender, commandLabel, args, originalCmd);
        }
    }
}
