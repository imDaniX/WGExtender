package wgextender.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public final class ModrinthUpdaterTest {
    private static @NotNull Stream<Arguments> parseData() {
        return Stream.of(
                Arguments.of("1.2.3", List.of(1L, 2L, 3L), false, List.of()),
                Arguments.of("1.2.3-SNAPSHOT.4", List.of(1L, 2L, 3L), true, List.of(4L)),
                Arguments.of("1.0-snapshot", List.of(1L, 0L), true, List.of()),
                Arguments.of("  1.0.0  ", List.of(1L, 0L, 0L), false, List.of()),
                Arguments.of("vUnknown", List.of(), false, List.of())
        );
    }

    @ParameterizedTest
    @MethodSource("parseData")
    public void parseTest(@NotNull String input, @NotNull List<Long> core, boolean snapshot, @NotNull List<Long> tail) {
        ModrinthUpdater.PluginVersion v = ModrinthUpdater.PluginVersion.parse(input);
        assertThat((List<? extends Long>) v.core()).isEqualTo(core);
        assertThat(v.snapshot()).isEqualTo(snapshot);
        assertThat((List<? extends Long>) v.tail()).isEqualTo(tail);
    }

    private static @NotNull Stream<Arguments> compareToData() {
        return Stream.of(
                Arguments.of("1.2.3", "1.2.3", 0),
                Arguments.of("1.2.3", "1.2.4", -1),
                Arguments.of("2.0.0", "1.0.0", 1),
                Arguments.of("1.2", "1.2.0", -1),
                Arguments.of("1.10.0", "1.9.0", 1),
                Arguments.of("1.2.3", "1.2.3-SNAPSHOT", 1),
                Arguments.of("1.2.3-SNAPSHOT", "1.2.3", -1),
                Arguments.of("1.2.3-SNAPSHOT.1", "1.2.3-SNAPSHOT.2", -1)
        );
    }

    @ParameterizedTest
    @MethodSource("compareToData")
    public void compareToTest(@NotNull String a, @NotNull String b, int expectedSign) {
        ModrinthUpdater.PluginVersion aVer = ModrinthUpdater.PluginVersion.parse(a);
        ModrinthUpdater.PluginVersion bVer = ModrinthUpdater.PluginVersion.parse(b);
        assertThat(Integer.signum(aVer.compareTo(bVer))).isEqualTo(expectedSign);
    }

    private static @NotNull Stream<Arguments> resultData() {
        return Stream.of(
                Arguments.of("1.1.0", "1.0.0", (ResultFactory) ModrinthUpdater.CheckResult.Ahead::new),
                Arguments.of("1.0.0", "1.0.0", (ResultFactory) ModrinthUpdater.CheckResult.UpToDate::new),
                Arguments.of("1.0.0", "1.1.0", (ResultFactory) ModrinthUpdater.CheckResult.Available::new)
        );
    }

    @ParameterizedTest
    @MethodSource("resultData")
    public void resultTest(@NotNull String currentVersion, @NotNull String latestVersion, @NotNull ResultFactory factory) {
        ModrinthUpdater.PluginVersion current = ModrinthUpdater.PluginVersion.parse(currentVersion);
        ModrinthUpdater.VersionFile latest = versionFile(latestVersion);
        ModrinthUpdater.CheckResult result = factory.apply(current, latest);
        assertThat(result.current()).isEqualTo(current);
        assertThat(latestOf(result)).isEqualTo(latest);
    }

    @Test
    public void failureTest() {
        ModrinthUpdater.PluginVersion current = ModrinthUpdater.PluginVersion.parse("1.0.0");
        Exception cause = new IOException("no");
        ModrinthUpdater.CheckResult.Failure failure = new ModrinthUpdater.CheckResult.Failure(current, cause);
        assertThat(failure.current()).isEqualTo(current);
        assertThat(failure.cause()).isEqualTo(cause);
    }

    private static @NotNull ModrinthUpdater.VersionFile versionFile(@NotNull String version) {
        return new ModrinthUpdater.VersionFile(ModrinthUpdater.PluginVersion.parse(version), ModrinthUpdater.VersionType.RELEASE, "http://x", "f.jar");
    }

    @Nullable
    private static ModrinthUpdater.VersionFile latestOf(@NotNull ModrinthUpdater.CheckResult result) {
        return switch (result) {
            case ModrinthUpdater.CheckResult.Ahead ahead -> ahead.latestFile();
            case ModrinthUpdater.CheckResult.UpToDate upToDate -> upToDate.latestFile();
            case ModrinthUpdater.CheckResult.Available available -> available.latestFile();
            default -> null;
        };
    }

    @FunctionalInterface
    public interface ResultFactory extends BiFunction<ModrinthUpdater.PluginVersion, ModrinthUpdater.VersionFile, ModrinthUpdater.CheckResult> {
        @NotNull ModrinthUpdater.CheckResult apply(@NotNull ModrinthUpdater.PluginVersion current, @NotNull ModrinthUpdater.VersionFile latest);
    }
}