package wgextender.utils;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.jetbrains.annotations.NotNull;

public abstract class CommandWrapper extends Command {
    protected final Command originalCmd;
    protected final Server server;

    protected CommandWrapper(@NotNull Server server, @NotNull String originalCmd) {
        this(server, CommandsUtils.getCommands(server).get(originalCmd));
    }

    protected CommandWrapper(@NotNull Server server, @NotNull Command originalCmd) {
        super(originalCmd.getName(), originalCmd.getDescription(), originalCmd.getUsage(), originalCmd.getAliases());
        this.server = server;
        this.originalCmd = originalCmd;
    }

    public void inject() { // TODO Injectable interface
        CommandsUtils.replaceCommand(server, originalCmd, this);
    }

    public void uninject() {
        CommandsUtils.replaceCommand(server, this, originalCmd);
    }
}
