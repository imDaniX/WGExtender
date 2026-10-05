package wgextender.config.section;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import wgextender.config.Pointer;
import wgextender.config.Snapshot;

import static wgextender.config.section.Sections.at;

public record Claim(
        boolean expandSelectionVertical,
        boolean hijackHandler
) {
    public static final Pointer.Scoped<Claim> POINTER = Pointer.scoped(Snapshot.Scope::claim);

    public static @NotNull Claim load(@NotNull ConfigurationSection config) {
        return at(config, "claim", claimSection -> new Claim(
                claimSection.getBoolean("vertexpand", false),
                claimSection.getBoolean("hijack-handler", true)
        ));
    }
}
