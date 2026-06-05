package wgextender.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class CommandsUtilsTest {
    public static Stream<Arguments> computePrefixedVariantsData() {
        return Stream.of(
                Arguments.of(
                        List.of("home bed"),
                        Map.of("home", List.of("h")),
                        Set.of("home bed", "home bed x", "h bed", "h bed x"),
                        Set.of("home bedx", "h bedx", "home", "h")
                ),
                Arguments.of(
                        List.of("tp"),
                        Map.of("tp", List.of("teleport", "t", "warp")),
                        Set.of("tp", "tp x", "teleport", "teleport x", "t", "t x", "warp", "warp x"),
                        Set.of("tpx", "teleportx", "tx", "warpx")
                ),
                Arguments.of(
                        List.of("HOME set"),
                        Map.of("home", List.of("h")),
                        Set.of("HOME set", "home set x", "h set", "h set x"),
                        Set.of("home setx", "h setx")
                ),
                Arguments.of(
                        List.of("gamemode creative"),
                        Map.of(),
                        Set.of("gamemode creative", "gamemode creative x"),
                        Set.of("gamemode creativex", "gamemode", "creative")
                ),
                Arguments.of(
                        List.of("home", "TP here", "back"),
                        Map.of(
                                "home", List.of("h"),
                                "tp", List.of("teleport", "warp"),
                                "back", List.of("b")
                        ),
                        Set.of(
                                "home", "home x", "h", "h x",
                                "tp here", "tp here x", "teleport here", "teleport here x",
                                "warp here", "warp here x", "back", "back x", "b", "b x"
                        ),
                        Set.of("homex", "hx", "tp herex", "teleport herex", "backx", "bx")
                ),
                Arguments.of(
                        List.of("give player diamond 64"),
                        Map.of(),
                        Set.of("give player diamond 64", "give player diamond 64 x"),
                        Set.of("give player diamond", "give player diamond 64x", "give")
                )
        );
    }

    @ParameterizedTest
    @MethodSource("computePrefixedVariantsData")
    public void computePrefixedVariantsTest(
            Collection<String> commands,
            Map<String, List<String>> aliasMap,
            Set<String> shouldMatch,
            Set<String> shouldNotMatch
    ) {
        Predicate<String> predicate = CommandsUtils.computePrefixedVariants(
                commands,
                key -> aliasMap.getOrDefault(key.toLowerCase(Locale.ROOT), List.of())
        );
        assertThat(shouldMatch).allMatch(predicate);
        assertThat(shouldNotMatch).noneMatch(predicate);
    }

    public static Stream<Arguments> computeVariantsData() {
        return Stream.of(
                Arguments.of(
                        List.of("home bed"),
                        Map.of("home", List.of("h")),
                        Set.of("home bed", "HOME BED", "h bed"),
                        Set.of("home bed x", "home", "h")
                ),
                Arguments.of(
                        List.of("tp"),
                        Map.of("tp", List.of("teleport", "t")),
                        Set.of("tp", "TP", "teleport", "t"),
                        Set.of("tp x", "teleport x")
                ),
                Arguments.of(
                        List.of("gamemode creative"),
                        Map.of(),
                        Set.of("gamemode creative", "GAMEMODE CREATIVE"),
                        Set.of("gamemode creative x", "gamemode")
                )
        );
    }

    @ParameterizedTest
    @MethodSource("computeVariantsData")
    public void computeVariantsTest(
            Collection<String> commands,
            Map<String, List<String>> aliasMap,
            Set<String> shouldMatch,
            Set<String> shouldNotMatch
    ) {
        Predicate<String> predicate = CommandsUtils.computeVariants(
                commands,
                key -> aliasMap.getOrDefault(key.toLowerCase(Locale.ROOT), List.of())
        );
        assertThat(shouldMatch).allMatch(predicate);
        assertThat(shouldNotMatch).noneMatch(predicate);
    }
}