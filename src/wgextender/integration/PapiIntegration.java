package wgextender.integration;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wgextender.WGExtender;

import java.util.Locale;

@ApiStatus.Internal
public class PapiIntegration extends PlaceholderExpansion {
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
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer offPlayer, @NotNull String paramsRaw) {
        if (offPlayer == null) return null;

        var handler = plugin.getBlockLimitsHandler();
        var reader = new ParamsReader(paramsRaw);

        if (!reader.next().equals("blocklimit")) return null;

        return switch (reader.next()) {
            case "refresh" -> offPlayer instanceof Player player
                    ? handler.refreshBlockLimit(player).toString()
                    : null;
            case "cached", "cache" -> offPlayer instanceof Player player
                    ? handler.cachedBlockLimit(player).toString()
                    : null;
            case "calc" -> handler.calculateBlockLimit(offPlayer).toString();
            case "group" -> {
                String groupRaw = reader.remaining();
                if (groupRaw.isEmpty()) yield null;

                String groupName = groupRaw.contains("{")
                        ? PlaceholderAPI.setBracketPlaceholders(offPlayer, groupRaw)
                        : groupRaw;

                yield handler.blockLimitByGroup(groupName).toString();
            }
            default -> null;
        };
    }

    private static class ParamsReader {
        private final String input;
        private int index;

        ParamsReader(String input) {
            this.input = input.toLowerCase(Locale.ROOT);
            this.index = 0;
        }

        String next() {
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
