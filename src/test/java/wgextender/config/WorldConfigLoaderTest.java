package wgextender.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WorldConfigLoaderTest {
    private static final String BASE = """
            regionprotect:
              explosion:
                block: false
                entity: true
              flow:
                lava: true
            restrictcommands:
              enabled: true
              commands: [sethome, setwarp]
              recheck-ticks: 100
            """;

    @TempDir
    Path folder;

    private YamlConfiguration base;

    @BeforeEach
    void setUp() {
        base = yaml(BASE);
    }

    @Test
    void missingFolderLoadsNothing() {
        var loader = new WorldLoader(folder.resolve("missing").toFile(), NOPLogger.NOP_LOGGER);
        assertTrue(loader.load(base, flag -> { }).isEmpty());
    }

    @Test
    void worldOverridesCascadeOverBase() throws IOException {
        Snapshot.Scope nether = loadWorld("nether", """
                regionprotect:
                  explosion:
                    block: true
                restrictcommands:
                  commands: [tpa]
                """);

        assertTrue(nether.explosion().block());
        assertTrue(nether.explosion().entity()); // inherited
        assertTrue(nether.flow().lava()); // inherited
        assertEquals(List.of("tpa"), nether.restrictCommands().commands());
        assertTrue(nether.restrictCommands().enabled()); // inherited
    }

    @Test
    void emptySectionFallsBackToDefault() throws IOException {
        Snapshot.Scope anarchy = loadWorld("anarchy", """
                regionprotect:
                  flow: {}
                """);

        assertFalse(anarchy.flow().lava()); // hardcoded default instead of the base's true
        assertTrue(anarchy.explosion().entity());
    }

    @ParameterizedTest
    @ValueSource(strings = {"world_nether", "WORLD_NETHER", "World_Nether"})
    void worldNamesAreCaseInsensitive(String lookup) throws IOException {
        write("World_Nether.yml", "regionprotect:\n  explosion:\n    block: true\n");

        assertNotNull(load().get(lookup));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "notes.txt                      | hello",
            "example.yml.disabled           | regionprotect: {}",
            "broken.yml                     | regionprotect: [unclosed",
    })
    void unusableFilesAreSkipped(String fileName, String content) throws IOException {
        write(fileName, content);
        write("fine.yml", "regionprotect: {}");

        assertEquals(Set.of("fine"), load().keySet());
    }

    @Test
    void globalOnlyKeysAreReported() throws IOException {
        Logger logger = mock(Logger.class);
        write("nether.yml", "restrictcommands:\n  recheck-ticks: 5\nmisc:\n  old-pvp-flags: true\n");

        load(logger);

        verify(logger).warn(anyString(), eq("restrictcommands.recheck-ticks"), eq("nether.yml"));
        verify(logger).warn(anyString(), eq("misc.old-pvp-flags"), eq("nether.yml"));
        verifyNoMoreInteractions(logger);
    }

    @Test
    void worldsDefinedTwiceAreReported() throws IOException {
        write("Nether.yml", "regionprotect: {}");
        assumeFalse(Files.exists(folder.resolve("nether.yml")), "Case-insensitive file system");
        write("nether.yml", "regionprotect: {}");
        Logger logger = mock(Logger.class);

        Map<String, Snapshot.Scope> worlds = load(logger);

        assertEquals(1, worlds.size());
        verify(logger).warn(anyString(), anyString(), anyString());
    }

    static Stream<Arguments> cascadeData() {
        return Stream.of(
                // Empty nested section removes the key
                Arguments.of(
                        """
                        sec1:
                          my_setting: true
                          subsec:
                            sub_setting: true
                        sec2:
                          other_setting: true
                        """,
                        """
                        sec1:
                          subsec: {}
                        """,
                        """
                        sec1:
                          my_setting: true
                        sec2:
                          other_setting: true
                        """
                ),
                // Empty top-level section removes it entirely
                Arguments.of(
                        """
                        sec1:
                          my_setting: true
                        sec2:
                          other_setting: true
                        """,
                        """
                        sec2: {}
                        """,
                        """
                        sec1:
                          my_setting: true
                        """
                ),
                // Values override, sections merge
                Arguments.of(
                        """
                        sec1:
                          my_setting: true
                          subsec:
                            sub_setting: true
                        """,
                        """
                        sec1:
                          my_setting: false
                          subsec:
                            new_setting: 5
                        """,
                        """
                        sec1:
                          my_setting: false
                          subsec:
                            sub_setting: true
                            new_setting: 5
                        """
                ),
                // Lists are replaced
                Arguments.of(
                        """
                        commands: [sethome, setwarp]
                        """,
                        """
                        commands: [tpa]
                        """,
                        """
                        commands: [tpa]
                        """
                ),
                // Empty overrides inherit everything
                Arguments.of(
                        """
                        sec1:
                          my_setting: true
                        """,
                        """
                        # nothing here
                        """,
                        """
                        sec1:
                          my_setting: true
                        """
                )
        );
    }

    @ParameterizedTest
    @MethodSource("cascadeData")
    void cascadeTest(String baseYaml, String overridesYaml, String expectedYaml) {
        YamlConfiguration base = yaml(baseYaml);

        var result = WorldLoader.cascade(base, yaml(overridesYaml));

        assertEquals(normalize(yaml(expectedYaml)), normalize(result));
        assertEquals(normalize(yaml(baseYaml)), normalize(base), "The base config must not be modified");
    }

    private Map<String, Snapshot.Scope> load() {
        return load(NOPLogger.NOP_LOGGER);
    }

    private Map<String, Snapshot.Scope> load(Logger logger) {
        return new WorldLoader(folder.toFile(), logger).load(base, flag -> { });
    }

    private Snapshot.Scope loadWorld(String world, String content) throws IOException {
        write(world + ".yml", content);
        return load().get(world);
    }

    private void write(String name, String content) throws IOException {
        Files.writeString(folder.resolve(name), content);
    }

    private static YamlConfiguration yaml(String content) {
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(content);
            return config;
        } catch (InvalidConfigurationException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static Map<String, Object> normalize(YamlConfiguration config) {
        Map<String, Object> shape = new LinkedHashMap<>();
        for (String path : config.getKeys(true)) {
            shape.put(path, config.isConfigurationSection(path) ? "<section>" : config.get(path));
        }
        return shape;
    }
}
