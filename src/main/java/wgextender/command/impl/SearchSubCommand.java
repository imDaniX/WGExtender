package wgextender.command.impl;

import com.mojang.brigadier.context.CommandContext;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import wgextender.WGExtender;
import wgextender.command.SubCommandBase;
import wgextender.config.message.MKey;
import wgextender.utils.WEUtils;
import wgextender.utils.WGUtils;

import java.util.ArrayList;
import java.util.List;

final class SearchSubCommand extends SubCommandBase.Simple {
    SearchSubCommand(@NotNull WGExtender plugin) {
        super(plugin, "search");
    }

    @Override
    protected void execute(@NotNull CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
            msg.sendMessage(ctx.getSource().getSender(), MKey.COMMON__ERROR__PLAYER_ONLY);
            return;
        }
        try {
            Region sel = WEUtils.getSelection(player);
            ApplicableRegionSet regions = WGUtils.getRegionManager(player.getWorld()).getApplicableRegions(
                    new ProtectedCuboidRegion("wgexfakerg", sel.getMaximumPoint(), sel.getMinimumPoint())
            );
            List<String> regionIds = new ArrayList<>(regions.size());
            for (ProtectedRegion region : regions) {
                regionIds.add(region.getId());
            }
            if (regionIds.isEmpty()) {
                msg.sendMessage(player, MKey.WGEX_COMMAND__SEARCH__NOT_FOUND);
            } else {
                msg.sendMessage(player, MKey.WGEX_COMMAND__SEARCH__FOUND, regionIds);
            }
        } catch (IncompleteRegionException e) {
            msg.sendMessage(player, MKey.WGEX_COMMAND__SEARCH__INCOMPLETE_SELECTION);
        }
    }
}
