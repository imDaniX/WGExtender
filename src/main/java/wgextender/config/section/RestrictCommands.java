package wgextender.config.section;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import wgextender.config.Pointer;
import wgextender.config.Snapshot;

import java.util.List;

import static wgextender.config.section.Sections.at;

public record RestrictCommands(
        boolean enabled,
        boolean aliasedSearch,
        boolean prefixedSearch,
        List<String> commands
) {
    public static final Pointer.Scoped<RestrictCommands> POINTER = Pointer.scoped(Snapshot.Scope::restrictCommands);

    public static @NotNull RestrictCommands load(@NotNull ConfigurationSection config) {
        return at(config, "restrictcommands", rcSection -> new RestrictCommands(
                rcSection.getBoolean("enabled", false),
                rcSection.getBoolean("aliased-search", true),
                rcSection.getBoolean("prefixed-search", true),
                List.copyOf(rcSection.getStringList("commands"))
        ));
    }
}
