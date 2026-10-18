package wgextender.config.section;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import wgextender.config.Pointer;
import wgextender.config.Snapshot;

import static wgextender.config.section.Sections.at;

public record Flow(boolean lava, boolean water, boolean other) {
    public static final Pointer.Scoped<Flow> POINTER = Pointer.scoped(Snapshot.Scope::flow);

    public static @NotNull Flow load(@NotNull ConfigurationSection config) {
        return at(config, "regionprotect", protectionSection -> at(protectionSection, "flow", flowSection -> new Flow(
                flowSection.getBoolean("lava", false),
                flowSection.getBoolean("water", false),
                flowSection.getBoolean("other", false)
        )));
    }
}
