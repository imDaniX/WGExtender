package wgextender.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import wgextender.WGExtender;
import wgextender.command.SubCommandBase;
import wgextender.config.message.MKey;
import wgextender.utils.WGUtils;

import java.util.Locale;
import java.util.UUID;

import static wgextender.command.Brigadierable.cmd;

abstract class RemoveDomainSubCommands extends SubCommandBase {
    private final boolean owner;
    private final MKey successKey;

    private RemoveDomainSubCommands(@NotNull WGExtender plugin, @NotNull String name, boolean owner, @NotNull MKey successKey) {
        super(plugin, name);
        this.owner = owner;
        this.successKey = successKey;
    }

    @Override
    public @NotNull LiteralArgumentBuilder<CommandSourceStack> node() {
        return root()
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            server.getOnlinePlayers().forEach(player -> builder.suggest(player.getName()));
                            return builder.buildFuture();
                        })
                        .executes(cmd(this::execute))
                );
    }

    private void execute(@NotNull CommandContext<CommandSourceStack> ctx) {
        String input = StringArgumentType.getString(ctx, "player");
        OfflinePlayer offPlayer = server.getOfflinePlayer(input);
        String name = (offPlayer.getName() == null ? input : offPlayer.getName()).toLowerCase(Locale.ROOT);
        UUID uuid = offPlayer.getUniqueId(); // TODO We should also support UUID as an argument
        for (RegionManager manager : WGUtils.getRegionContainer().getLoaded()) {
            for (ProtectedRegion region : manager.getRegions().values()) {
                DefaultDomain domain = owner ? region.getOwners() : region.getMembers();
                domain.removePlayer(uuid);
                domain.removePlayer(name);
                if (owner) {
                    region.setOwners(domain);
                } else {
                    region.setMembers(domain);
                }
            }
        }
        msg.sendMessage(ctx.getSource().getSender(), successKey);
    }

    static final class Owner extends RemoveDomainSubCommands {
        Owner(@NotNull WGExtender plugin) {
            super(plugin, "removeowner", true, MKey.WGEX_COMMAND__REMOVEOWNER__SUCCESS);
        }
    }

    static final class Member extends RemoveDomainSubCommands {
        Member(@NotNull WGExtender plugin) {
            super(plugin, "removemember", false, MKey.WGEX_COMMAND__REMOVEMEMBER__SUCCESS);
        }
    }
}
