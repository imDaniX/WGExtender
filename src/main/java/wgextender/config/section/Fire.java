package wgextender.config.section;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import wgextender.config.Pointer;
import wgextender.config.Snapshot;

import static wgextender.config.section.Sections.at;

public record Fire(boolean spreadToRegion, boolean spreadInRegion, boolean burn) {
    public static final Pointer.Scoped<Fire> POINTER = Pointer.scoped(Snapshot.Scope::fire);

    public static @NotNull Fire load(@NotNull ConfigurationSection config) {
        return at(config, "regionprotect", protectionSection -> at(protectionSection, "fire", fireSection -> new Fire(
                fireSection.getBoolean("spread.toregion", false),
                fireSection.getBoolean("spread.inregion", false),
                fireSection.getBoolean("burn", false)
        )));
    }
}
