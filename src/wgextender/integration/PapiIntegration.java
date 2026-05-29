package wgextender.integration;

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
    public @Nullable String onRequest(OfflinePlayer offPlayer, @NotNull String params) {
        if (offPlayer == null) return null;

        var handler = plugin.getBlockLimitsHandler();
        return switch (params.toLowerCase(Locale.ROOT)) {
            case "blocklimit_refresh" -> (offPlayer instanceof Player player)
                    ? handler.refreshBlockLimit(player).toString()
                    : null;
            case "blocklimit_cached", "blocklimit_cache" -> (offPlayer instanceof Player player)
                    ? handler.cachedBlocksLimit(player).toString()
                    : null;
            case "blocklimit_calc" -> handler.calculateBlocksLimit(offPlayer).toString();
            default -> null;
        };
    }
}
