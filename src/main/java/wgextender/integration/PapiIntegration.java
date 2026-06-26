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
        var reader = new ParamsReader(paramsRaw);

        return switch (reader.input) {
            case "context_helper" -> handleContextHelper(offPlayer);
            case "in_region" -> handleInRegion(offPlayer);
            default -> {
                String next = reader.pop();
                // TODO QoL: add claim count limit
                if (next.equals("blocklimit") || (next.equals("limit") && reader.pop().equals("blocks"))) {
                    yield handleBlockLimit(offPlayer, reader);
                } else {
                    yield null;
                }
            }
        };
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

    private @Nullable String handleBlockLimit(@Nullable OfflinePlayer offPlayer, @NotNull ParamsReader reader) {
        var handler = plugin.getBlockLimitsHandler();
        return switch (reader.pop()) {
            case "", "refresh" -> offPlayer instanceof Player player
                    ? handler.refreshBlockLimit(player).toString()
                    : null;
            case "cached", "cache" -> offPlayer instanceof Player player
                    ? handler.cachedBlockLimit(player).toString()
                    : null;
            case "calc" -> offPlayer != null
                    ? handler.calculateBlockLimit(offPlayer).toString()
                    : null;
            case "group" -> {
                String groupRaw = reader.remaining();
                if (groupRaw.isEmpty()) yield null;

                String groupName = groupRaw.indexOf('{') != -1
                        ? PlaceholderAPI.setBracketPlaceholders(offPlayer, groupRaw)
                        : groupRaw;

                yield handler.groupBlockLimit(groupName).toString();
            }
            default -> null;
        };
    }

    @Override
    public @NotNull Collection<@NotNull String> requiredPlugins() {
        return List.of("PlaceholderAPI");
    }

    @Override
    public void onEnable(@NotNull WGExtender plugin) {
        register();
    }

    private static class ParamsReader {
        private final String input;
        private int index;

        ParamsReader(String input) {
            this.input = input.toLowerCase(Locale.ROOT);
            this.index = 0;
        }

        String pop() {
            int end = input.indexOf('_', index);
            String current = end == -1 ? input.substring(index) : input.substring(index, end);
            index = end == -1 ? input.length() : end + 1;
            return current.isEmpty() ? "" : current;
        }

        String remaining() {
            return index >= input.length() ? "" : input.substring(index);
        }
    }
}
