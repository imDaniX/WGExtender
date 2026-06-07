package wgextender.integration;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import wgextender.WGExtender;
import wgextender.features.flags.WGExtenderFlags;
import wgextender.utils.WGUtils;

import java.util.Collection;
import java.util.List;

public class LpIntegration implements PluginIntegration {
    @Override
    public @NotNull Collection<@NotNull String> requiredPlugins() {
        return List.of("LuckPerms");
    }

    @Override
    public void onEnable(@NotNull WGExtender wgExtender) {
        RegisteredServiceProvider<LuckPerms> provider = wgExtender.getServer().getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            LuckPerms api = provider.getProvider();
            api.getContextManager().registerCalculator(new RegionContext());
        }
    }

    public static final class RegionContext implements ContextCalculator<Player> { // TODO Configure to disable
        @Override
        public void calculate(@NotNull Player target, @NotNull ContextConsumer context) {
            var regions = WGUtils.getRegionsAt(target.getLocation());
            // Passing null instead of player, so we wouldn't recurse
            String contextFlag = regions.queryValue(null, WGExtenderFlags.CONTEXT_HELPER_FLAG);
            if (contextFlag != null) {
                context.accept(
                        "wgex:context-helper",
                        contextFlag
                );
            }
            context.accept(
                    "wgex:in-region",
                    Boolean.toString(regions.size() > 0)
            );
        }
    }
}
