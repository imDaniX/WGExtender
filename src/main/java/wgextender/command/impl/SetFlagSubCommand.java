package wgextender.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldguard.protection.flags.BooleanFlag;
import com.sk89q.worldguard.protection.flags.EnumFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import wgextender.WGExtender;
import wgextender.command.SubCommandBase;
import wgextender.config.message.MKey;
import wgextender.utils.WEUtils;
import wgextender.utils.WGUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static io.papermc.paper.command.brigadier.Commands.argument;
import static wgextender.command.Brigadierable.cmd;
import static wgextender.utils.WGUtils.getWorldGuard;

final class SetFlagSubCommand extends SubCommandBase {
    private static final List<String> STATE_VALUES = List.of("ALLOW", "DENY");
    private static final List<String> BOOLEAN_VALUES = List.of("true", "false");

    SetFlagSubCommand(@NotNull WGExtender plugin) {
        super(plugin, "setflag");
    }

    @Override
    public @NotNull LiteralArgumentBuilder<CommandSourceStack> node() {
        return root()
                .then(argument("world", StringArgumentType.word())
                        .suggests(this::suggestWorlds)
                        .then(argument("flag", StringArgumentType.word())
                                .suggests(this::suggestFlags)
                                .then(argument("value", StringArgumentType.greedyString())
                                        .suggests(this::suggestValue)
                                        .executes(cmd(this::execute))
                                )
                        )
                );
    }

    private @NotNull CompletableFuture<Suggestions> suggestWorlds(@NotNull CommandContext<CommandSourceStack> ctx, @NotNull SuggestionsBuilder builder) {
        server.getWorlds().forEach(world -> builder.suggest(world.getName()));
        return builder.buildFuture();
    }

    private @NotNull CompletableFuture<Suggestions> suggestFlags(@NotNull CommandContext<CommandSourceStack> ctx, @NotNull SuggestionsBuilder builder) {
        getWorldGuard().getFlagRegistry().forEach(flag -> builder.suggest(flag.getName()));
        return builder.buildFuture();
    }

    private @NotNull CompletableFuture<Suggestions> suggestValue(@NotNull CommandContext<CommandSourceStack> ctx, @NotNull SuggestionsBuilder builder) {
        Flag<?> flag = WGUtils.matchFlag(StringArgumentType.getString(ctx, "flag"));
        if (flag instanceof StateFlag) {
            STATE_VALUES.forEach(builder::suggest);
        } else if (flag instanceof BooleanFlag) {
            BOOLEAN_VALUES.forEach(builder::suggest);
        } else if (flag instanceof EnumFlag<?> enumFlag) {
            try {
                for (Enum<?> constant : enumFlag.getEnumClass().getEnumConstants()) {
                    builder.suggest(constant.toString());
                }
            } catch (Exception ignored) { }
        }
        return builder.buildFuture();
    }

    private void execute(@NotNull CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        World world = server.getWorld(StringArgumentType.getString(ctx, "world"));
        if (world == null) {
            msg.sendMessage(sender, MKey.WGEX_COMMAND__SETFLAG__WORLD_NOT_FOUND);
            return;
        }
        Flag<?> flag = WGUtils.matchFlag(StringArgumentType.getString(ctx, "flag"));
        if (flag == null) {
            msg.sendMessage(sender, MKey.WGEX_COMMAND__SETFLAG__FLAG_NOT_FOUND);
            return;
        }
        String value = StringArgumentType.getString(ctx, "value");
        try {
            RegionManager regionManager = WGUtils.getRegionManager(world);
            if (regionManager == null) {
                msg.sendMessage(sender, MKey.COMMON__ERROR__WORLD_DISABLED, world.getName());
                return;
            }
            for (ProtectedRegion region : regionManager.getRegions().values()) {
                if (region instanceof GlobalProtectedRegion) {
                    continue;
                }
                WGUtils.setFlagNaturally(WEUtils.privilegedActor(sender, false), world, region, flag, value);
            }
            msg.sendMessage(sender, MKey.WGEX_COMMAND__SETFLAG__SUCCESS);
        } catch (CommandException e) {
            msg.sendMessage(sender, MKey.WGEX_COMMAND__SETFLAG__INVALID_FORMAT, flag.getName(), e.getMessage());
        }
    }
}
