package wgextender.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import wgextender.utils.CaseInsensitive;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

final class WorldLoader {
    private static final String EXTENSION = ".yml";
    private static final Set<String> GLOBAL_ONLY = Set.of(
            "messages", "updater", "misc.old-pvp-flags", "restrictcommands.recheck-ticks"
    );

    private final File folder;
    private final Logger logger;

    WorldLoader(@NotNull File folder, @NotNull Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    @NotNull Map<String, Snapshot.Scope> load(@NotNull ConfigurationSection base, @NotNull Consumer<String> onUnknownFlag) {
        File[] files = folder.listFiles((dir, name) -> name.endsWith(EXTENSION));
        if (files == null) {
            return Map.of();
        }
        Map<String, Snapshot.Scope> worlds = CaseInsensitive.newMap();
        for (File file : files) {
            String fileName = file.getName();
            String world = fileName.substring(0, fileName.length() - EXTENSION.length());
            YamlConfiguration overrides = new YamlConfiguration();
            try {
                overrides.load(file);
            } catch (IOException | InvalidConfigurationException e) {
                logger.warn("Unable to load the config of world '{}', using the general one", world, e);
                continue;
            }
            for (String path : GLOBAL_ONLY) {
                if (overrides.contains(path)) {
                    logger.warn("'{}' in {} is global-only and cannot be set per-world, ignoring", path, fileName);
                }
            }
            if (worlds.put(world, Snapshot.Scope.load(cascade(base, overrides), onUnknownFlag)) != null) {
                logger.warn("The config of world '{}' is defined more than once, {} takes precedence", world, fileName);
            }
        }
        return Collections.unmodifiableMap(worlds);
    }

    static @NotNull YamlConfiguration cascade(@NotNull ConfigurationSection base, @NotNull ConfigurationSection overrides) {
        YamlConfiguration result = new YamlConfiguration();
        copy(base, result);
        overlay(result, overrides);
        return result;
    }

    private static void copy(@NotNull ConfigurationSection from, @NotNull ConfigurationSection to) {
        for (String key : from.getKeys(false)) {
            ConfigurationSection section = from.getConfigurationSection(key);
            if (section != null) {
                copy(section, to.createSection(key));
            } else {
                to.set(key, from.get(key));
            }
        }
    }

    private static void overlay(@NotNull ConfigurationSection target, @NotNull ConfigurationSection overrides) {
        for (String key : overrides.getKeys(false)) {
            ConfigurationSection section = overrides.getConfigurationSection(key);
            if (section == null) {
                target.set(key, overrides.get(key));
            } else if (section.getKeys(false).isEmpty()) {
                target.set(key, null); // An empty section removes the key
            } else {
                ConfigurationSection targetSection = target.getConfigurationSection(key);
                overlay(targetSection == null ? target.createSection(key) : targetSection, section);
            }
        }
    }
}
