package wgextender.config.section;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import wgextender.config.Pointer;
import wgextender.config.Snapshot;

import static wgextender.config.section.Sections.at;

public record Updater(
        boolean enabled,
        int checkInterval,
        boolean joinNotify,
        boolean intervalNotify,
        boolean allowStaging,
        @NotNull String baseUrl,
        boolean logFailures
) {
    public static final Pointer.Global<Updater> POINTER = Pointer.global(Snapshot::updater);

    public static @NotNull Updater load(@NotNull ConfigurationSection config) {
        return at(config, "updater", updaterSection -> at(updaterSection, "notify", notifySection -> new Updater(
                updaterSection.getBoolean("enabled", true),
                updaterSection.getInt("check-interval", 86400),
                notifySection.getBoolean("on-join", true),
                notifySection.getBoolean("on-interval", false),
                updaterSection.getBoolean("allow-staging", false),
                updaterSection.getString("url-base", "https://api.modrinth.com"),
                updaterSection.getBoolean("log-failures", true)
        )));
    }
}
