package wgextender.config;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigTest {

    private static Plugin mockPlugin(Path dataFolder, String resourceYaml) {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getSLF4JLogger()).thenReturn(LoggerFactory.getLogger("test"));
        when(plugin.getResource(anyString())).thenAnswer(
                inv -> new ByteArrayInputStream(resourceYaml.getBytes(StandardCharsets.UTF_8))
        );
        return plugin;
    }

    private static Config loadConfig(Path dataFolder, String resourceYaml) {
        Config config = new Config(mockPlugin(dataFolder, resourceYaml));
        config.loadConfig();
        return config;
    }

    private static String withVersion(int version, String body) {
        return "_version: " + version + "\n" + body;
    }

    private static final String REGION_PROTECT_ENABLED = """
        regionprotect:
          flow:
            lava: true
            water: true
        """;

    private static final String REGION_PROTECT_WITH_PER_WORLD = """
        regionprotect:
          flow:
            lava: false
            water: false
          per-world:
            world_nether:
              flow:
                lava: true
        """;

    static Stream<Object[]> limitForData() {
        Map<String, BigInteger> limits = new LinkedHashMap<>();
        limits.put("vip", BigInteger.valueOf(5000));
        limits.put("default", BigInteger.valueOf(1000));

        var blockLimits = new Config.ClaimSettings.BlockLimits(true, limits, Config.ClaimSettings.Minimal.DEFAULTS);
        var settings = new Config.ClaimSettings(false, blockLimits);

        return Stream.of(
                new Object[]{ settings, "vip", BigInteger.valueOf(5000) },
                new Object[]{ settings, "VIP", BigInteger.valueOf(5000) },
                new Object[]{ settings, "default", BigInteger.valueOf(1000) },
                new Object[]{ settings, "unknown", BigInteger.valueOf(1000) }
        );
    }

    @ParameterizedTest
    @MethodSource("limitForData")
    void limitForTest(Config.ClaimSettings settings, String group, BigInteger expected) {
        assertEquals(expected, settings.limitFor(group));
    }

    static Stream<Object[]> limitForWithFallbackData() {
        Map<String, BigInteger> limits = new LinkedHashMap<>();
        limits.put("admin", BigInteger.valueOf(99999));

        var blockLimits = new Config.ClaimSettings.BlockLimits(true, limits, Config.ClaimSettings.Minimal.DEFAULTS);
        var settings = new Config.ClaimSettings(false, blockLimits);
        BigInteger fallback = BigInteger.valueOf(42);

        return Stream.of(
                new Object[]{ settings, "admin", fallback, BigInteger.valueOf(99999) },
                new Object[]{ settings, "nobody", fallback, fallback }
        );
    }

    @ParameterizedTest
    @MethodSource("limitForWithFallbackData")
    void limitForWithFallbackTest(Config.ClaimSettings settings, String group, BigInteger fallback, BigInteger expected) {
        assertEquals(expected, settings.limitFor(group, fallback));
    }

    @Test
    void regionProtectionDefaultsTest(@TempDir Path dir) {
        Config config = loadConfig(dir, withVersion(1, ""));
        var regionProtect = config.forWorld("world").regionProtection();
        assertFalse(regionProtect.flow().lava());
        assertFalse(regionProtect.flow().water());
        assertFalse(regionProtect.explosion().block());
    }

    @Test
    void regionProtectionLoadedTest(@TempDir Path dir) {
        Config config = loadConfig(dir, withVersion(1, REGION_PROTECT_ENABLED));
        var regionProtect = config.forWorld("world").regionProtection();
        assertTrue(regionProtect.flow().lava());
        assertTrue(regionProtect.flow().water());
    }

    @Test
    void perWorldOverridesDefaultTest(@TempDir Path dir) {
        Config config = loadConfig(dir, withVersion(1, REGION_PROTECT_WITH_PER_WORLD));
        assertTrue(config.forWorld("world_nether").regionProtection().flow().lava());
        assertFalse(config.forWorld("world").regionProtection().flow().lava());
    }

    @Test
    void perWorldInheritsUnsetKeysTest(@TempDir Path dir) {
        Config config = loadConfig(dir, withVersion(1, REGION_PROTECT_WITH_PER_WORLD));
        assertFalse(config.forWorld("world_nether").regionProtection().flow().water());
    }

    @Test
    void migrateSameVersionNoBackupTest(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("config.yml"), withVersion(2, ""));
        loadConfig(dir, withVersion(2, ""));

        long backups = Files.list(dir)
                .filter(p -> p.getFileName().toString().endsWith(".bak"))
                .count();
        assertEquals(0, backups);
    }

    @Test
    void migrateNewerVersionTest(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("config.yml"), withVersion(1, ""));
        loadConfig(dir, withVersion(2, ""));

        long backups = Files.list(dir)
                .filter(p -> p.getFileName().toString().endsWith(".bak"))
                .count();
        assertEquals(1, backups);
        assertTrue(Files.readString(dir.resolve("config.yml")).contains("_version: 2"));
    }

    @Test
    void migrateMissingVersionTest(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("config.yml"), REGION_PROTECT_ENABLED);
        loadConfig(dir, withVersion(1, ""));

        long backups = Files.list(dir)
                .filter(p -> p.getFileName().toString().endsWith(".bak"))
                .count();
        assertEquals(1, backups);
    }

    @Test
    void migrateMergesMissingKeysTest(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("config.yml"), withVersion(1, ""));
        loadConfig(dir, withVersion(2, REGION_PROTECT_ENABLED));

        var regionProtect = loadConfig(dir, withVersion(2, REGION_PROTECT_ENABLED)).forWorld("world").regionProtection();
        assertTrue(regionProtect.flow().lava());
        assertTrue(regionProtect.flow().water());
    }

    @Test
    void migratePreservesExistingValuesTest(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("config.yml"), withVersion(1, REGION_PROTECT_ENABLED));
        Config config = loadConfig(dir, withVersion(2, ""));

        assertTrue(config.forWorld("world").regionProtection().flow().lava());
    }
}