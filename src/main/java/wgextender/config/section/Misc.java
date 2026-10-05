package wgextender.config.section;

import com.sk89q.worldguard.protection.flags.StateFlag.State;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wgextender.config.Pointer;
import wgextender.config.Snapshot;

import java.util.Locale;

import static wgextender.config.section.Sections.at;

public record Misc(boolean extendedWeWand, @Nullable State pvpMode) {
    public static final Pointer.Scoped<Misc> POINTER = Pointer.scoped(Snapshot.Scope::misc);

    public static @NotNull Misc load(@NotNull ConfigurationSection config) {
        return at(config, "misc", miscSection -> new Misc(
                config.getBoolean("extendedwewand", false),
                switch (miscSection.getString("pvpmode", "default").toLowerCase(Locale.ROOT)) {
                    case "allow" -> State.ALLOW;
                    case "deny" -> State.DENY;
                    default -> null;
                }
        ));
    }
}
