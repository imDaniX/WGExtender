package wgextender.config;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wgextender.config.section.*;

import java.util.Map;
import java.util.function.Consumer;

public record Snapshot(
        @NotNull Scope general,
        @NotNull Map<String, Scope> worlds,
        @NotNull Messages messages,
        @NotNull Updater updater,
        @NotNull Features features
) {
    @NotNull Scope scopeOf(@Nullable String world) {
        return world == null ? general : worlds.getOrDefault(world, general);
    }

    public record Scope(
            @NotNull Claim claim,
            @NotNull BlockLimits blockLimits,
            @NotNull Flow flow,
            @NotNull Fire fire,
            @NotNull Explosion explosion,
            @NotNull AutoFlags autoFlags,
            @NotNull RestrictCommands restrictCommands,
            @NotNull Misc misc
    ) {
        static @NotNull Snapshot.Scope load(@NotNull ConfigurationSection config, @NotNull Consumer<String> onUnknownFlag) {
            return new Scope(
                    Claim.load(config),
                    BlockLimits.load(config),
                    Flow.load(config),
                    Fire.load(config),
                    Explosion.load(config),
                    AutoFlags.load(config, onUnknownFlag),
                    RestrictCommands.load(config),
                    Misc.load(config)
            );
        }
    }
}
