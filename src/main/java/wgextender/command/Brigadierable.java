package wgextender.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public interface Brigadierable {
    static @NotNull Command<CommandSourceStack> cmd(@NotNull Consumer<CommandContext<CommandSourceStack>> action) {
        return ctx -> {
            action.accept(ctx);
            return Command.SINGLE_SUCCESS;
        };
    }

    @NotNull LiteralArgumentBuilder<CommandSourceStack> node();
}
