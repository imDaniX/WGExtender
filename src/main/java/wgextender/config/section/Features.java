package wgextender.config.section;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import wgextender.config.Pointer;
import wgextender.config.Snapshot;

public record Features(boolean oldPvpFlags, int restrictCommandsRecheckTicks) {
    public static final Pointer.Global<Features> POINTER = Pointer.global(Snapshot::features);

    public static @NotNull Features load(@NotNull ConfigurationSection config) {
        return new Features(
                config.getBoolean("misc.old-pvp-flags", false),
                config.getInt("restrictcommands.recheck-ticks", 12000)
        );
    }
}
