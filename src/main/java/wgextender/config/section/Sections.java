package wgextender.config.section;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

final class Sections {
    private Sections() { }

    static <T> T at(@NotNull ConfigurationSection section, @NotNull String path, @NotNull Function<ConfigurationSection, T> creator) {
        var subSection = section.getConfigurationSection(path);
        return creator.apply(subSection == null ? section.createSection(path) : subSection);
    }
}
