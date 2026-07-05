package wgextender.external.updater;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.List;
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
        ModrinthUpdater.Version v = ModrinthUpdater.Version.parse(input);
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
        ModrinthUpdater.Version aVer = ModrinthUpdater.Version.parse(a);
        ModrinthUpdater.Version bVer = ModrinthUpdater.Version.parse(b);
        assertThat(Integer.signum(aVer.compareTo(bVer))).isEqualTo(expectedSign);
    }

    private static @NotNull Stream<Arguments> resultData() {
        return Stream.of(
                Arguments.of("1.1.0", "1.0.0", ModrinthUpdater.Status.AHEAD),
                Arguments.of("1.0.0", "1.0.0", ModrinthUpdater.Status.UP_TO_DATE),
                Arguments.of("1.0.0", "1.1.0", ModrinthUpdater.Status.AVAILABLE)
        );
    }

    @ParameterizedTest
    @MethodSource("resultData")
    public void resultTest(@NotNull String currentVersion, @NotNull String latestVersion, @NotNull ModrinthUpdater.Status status) {
        ModrinthUpdater.Version current = ModrinthUpdater.Version.parse(currentVersion);
        ModrinthUpdater.Artifact latest = artifact(latestVersion);
        ModrinthUpdater.Result.Success result = new ModrinthUpdater.Result.Success(current, latest, status);
        assertThat(result.current()).isEqualTo(current);
        assertThat(result.status()).isEqualTo(status);
        assertThat(result.latestFile()).isEqualTo(latest);
    }

    @Test
    public void failureTest() {
        ModrinthUpdater.Version current = ModrinthUpdater.Version.parse("1.0.0");
        Exception cause = new IOException("no");
        ModrinthUpdater.Result.Failure failure = new ModrinthUpdater.Result.Failure(current, cause);
        assertThat(failure.current()).isEqualTo(current);
        assertThat(failure.cause()).isEqualTo(cause);
    }

    private static @NotNull ModrinthUpdater.Artifact artifact(@NotNull String version) {
        return new ModrinthUpdater.Artifact(ModrinthUpdater.Version.parse(version), ModrinthUpdater.Version.Type.RELEASE, "http://x", "f.jar");
    }
}