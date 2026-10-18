package wgextender.integration;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wgextender.WGExtender;
import wgextender.features.flags.WGExtenderFlags;
import wgextender.utils.WGUtils;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static me.clip.placeholderapi.PlaceholderAPIPlugin.booleanFalse;
import static me.clip.placeholderapi.PlaceholderAPIPlugin.booleanTrue;

@ApiStatus.Internal
public final class PapiIntegration extends PlaceholderExpansion implements PluginIntegration {
    private final WGExtender plugin;

    public PapiIntegration(@NotNull WGExtender plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "wgex";
    }

    @Override
    public @NotNull String getAuthor() {
        return "imDaniX";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer offPlayer, @NotNull String paramsRaw) {
        String params = paramsRaw.toLowerCase(Locale.ROOT);

        return switch (params) {
            case "context_helper" -> handleContextHelper(offPlayer);
            case "in_region" -> handleInRegion(offPlayer);
            default -> {
                // TODO QoL: add claim count limit
                String rest = stripPrefix(params, "blocklimit");
                if (rest == null) rest = stripPrefix(params, "limit_blocks");
                yield rest != null ? handleBlockLimit(offPlayer, rest) : null;
            }
        };
    }

    private static @Nullable String stripPrefix(@NotNull String params, @NotNull String prefix) {
        if (!params.startsWith(prefix)) return null;
        String rest = params.substring(prefix.length());
        if (rest.startsWith("_")) return rest.substring(1);
        return rest.isEmpty() || rest.startsWith(",") ? rest : null;
    }

    private @Nullable String handleContextHelper(@Nullable OfflinePlayer offPlayer) {
        if (offPlayer == null) {
            return null;
        }
        Location location = offPlayer.getLocation();
        return location != null
                ? WGUtils.getFlagValue(location, WGExtenderFlags.CONTEXT_HELPER_FLAG)
                : null;
    }

    private @Nullable String handleInRegion(@Nullable OfflinePlayer offPlayer) {
        if (offPlayer instanceof Player player) {
            return WGUtils.getRegionsAt(player.getLocation()).size() > 0
                    ? booleanTrue()
                    : booleanFalse();
        } else {
            return null;
        }
    }

    private @Nullable String handleBlockLimit(@Nullable OfflinePlayer offPlayer, @NotNull String rest) {
        var handler = plugin.getBlockLimitsHandler();
        if (rest.startsWith("group_")) {
            String groupRaw = rest.substring("group_".length());
            if (groupRaw.isEmpty()) return null;

            String groupName = groupRaw.indexOf('{') != -1
                    ? PlaceholderAPI.setBracketPlaceholders(offPlayer, groupRaw)
                    : groupRaw;

            int groupCommaIndex = groupName.lastIndexOf(',');
            return handler.groupBlockLimit(
                    withoutWorld(groupName, groupCommaIndex),
                    worldOf(groupName, groupCommaIndex, offPlayer)
            ).toString();
        }
        int commaIndex = rest.lastIndexOf(',');
        String world = worldOf(rest, commaIndex, offPlayer);
        return switch (withoutWorld(rest, commaIndex)) {
            case "", "refresh" -> offPlayer instanceof Player player
                    ? handler.refreshBlockLimit(player, world).toString()
                    : null;
            case "cached", "cache" -> offPlayer instanceof Player player
                    ? handler.cachedBlockLimit(player, world).toString()
                    : null;
            case "calc" -> offPlayer != null
                    ? handler.calculateBlockLimit(offPlayer, world).toString()
                    : null;
            default -> null;
        };
    }

    private static @NotNull String withoutWorld(@NotNull String raw, int commaIndex) {
        return commaIndex == -1 ? raw : raw.substring(0, commaIndex);
    }

    private static @Nullable String worldOf(@NotNull String raw, int commaIndex, @Nullable OfflinePlayer offPlayer) {
        if (commaIndex == -1) {
            return offPlayer instanceof Player player ? player.getWorld().getName() : null;
        }
        String world = raw.substring(commaIndex + 1);
        return world.isEmpty() ? null : world;
    }

    @Override
    public @NotNull Collection<@NotNull String> requiredPlugins() {
        return List.of("PlaceholderAPI");
    }

    @Override
    public void onEnable(@NotNull WGExtender plugin) {
        register();
    }
}
