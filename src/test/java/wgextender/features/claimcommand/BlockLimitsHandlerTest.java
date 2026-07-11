package wgextender.features.claimcommand;

import com.sk89q.wepif.PermissionsResolverManager;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import wgextender.config.ConfigurationProvider;
import wgextender.features.claimcommand.BlockLimitsHandler.EvaluationResult;
import wgextender.features.claimcommand.BlockLimitsHandler.ResultType;
import wgextender.utils.WEUtils;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static java.math.BigInteger.TEN;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BlockLimitsHandlerTest {
    private static final BigInteger MAX_VALUE = BigInteger.valueOf(Integer.MAX_VALUE);
    private static final BigInteger HUNDRED = BigInteger.valueOf(100);
    private static final BigInteger FIFTY = BigInteger.valueOf(50);

    static Stream<Arguments> calculateBlockLimitData() {
        return Stream.of(
                Arguments.of(
                        List.of(),
                        Map.of(),
                        TEN,
                        TEN
                ),
                Arguments.of(
                        List.of("vip", "member"),
                        Map.of(
                                "vip", HUNDRED,
                                "member", FIFTY
                        ),
                        TEN,
                        HUNDRED
                ),
                Arguments.of(
                        List.of("unknown"),
                        Map.of(),
                        TEN,
                        ZERO
                )
        );
    }

    @ParameterizedTest
    @MethodSource("calculateBlockLimitData")
    void calculateBlockLimitTest(List<String> groups, Map<String, BigInteger> limits, BigInteger defaultLimit, BigInteger expected) {
        PermissionsResolverManager resolver = mock(PermissionsResolverManager.class);
        Player player = mock(Player.class);

        try (var permissions = setupPermissions(resolver, groups)) {
            BlockLimitsHandler handler = createHandler(blockLimits(defaultLimit, limits, ZERO, ZERO, ZERO));

            assertEquals(expected, handler.calculateBlockLimit(player));
            for (String group : groups) {
                assertEquals(limits.getOrDefault(group, defaultLimit), handler.groupBlockLimit(group)); // just in case
            }
            assertEquals(defaultLimit, handler.groupBlockLimit("__not_in_map__"));
        }
    }

    @Test
    void cachedBlockLimitTest() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        PermissionsResolverManager resolver = mock(PermissionsResolverManager.class);

        try (var permissions = setupPermissions(resolver, List.of("vip"))) {
            BlockLimitsHandler handler = createHandler(blockLimits(TEN, Map.of("vip", HUNDRED), ZERO, ZERO, ZERO));

            BigInteger first = handler.cachedBlockLimit(player);
            BigInteger second = handler.cachedBlockLimit(player);

            assertEquals(HUNDRED, first);
            assertEquals(first, second);
            verify(resolver, times(1)).getGroups(player); // since value is cached, we expect getGroups call only once
        }
    }

    @Test
    void refreshBlockLimitTest() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        PermissionsResolverManager resolver = mock(PermissionsResolverManager.class);

        try (var permissions = setupPermissions(resolver, List.of("vip"))) {
            BlockLimitsHandler handler = createHandler(blockLimits(TEN, Map.of("vip", HUNDRED), ZERO, ZERO, ZERO));

            handler.cachedBlockLimit(player);
            BigInteger refreshed = handler.refreshBlockLimit(player);

            assertEquals(HUNDRED, refreshed);
            verify(resolver, times(2)).getGroups(player); // since cache was empty, it should be called twice
        }
    }

    @Test
    void evaluateResultIncompleteSelectionTest() {
        Player player = mock(Player.class);

        try (var weUtils = mockStatic(WEUtils.class)) {
            weUtils.when(() -> WEUtils.getSelection(player)).thenThrow(new IncompleteRegionException());

            BlockLimitsHandler handler = createHandler(blockLimits(TEN, Map.of(), ZERO, ZERO, ZERO));
            EvaluationResult result = handler.evaluateResult(player);

            assertEquals(ResultType.ALLOW, result.type());
            assertEquals(MAX_VALUE, result.assignedSize());
            assertEquals(MAX_VALUE, result.assignedLimit());
        }
    }

    @Test
    void evaluateResultUnlimitedBypassTest() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.hasPermission("worldguard.region.unlimited")).thenReturn(true);

        Region region = cuboid(0, 0, 0);
        PermissionsResolverManager resolver = mock(PermissionsResolverManager.class);

        try (
                var weUtils = mockStatic(WEUtils.class);
                var permissions = setupPermissions(resolver, List.of("default"))
        ) {
            weUtils.when(() -> WEUtils.getSelection(player)).thenReturn(region);

            BlockLimitsHandler handler = createHandler(
                    blockLimits(TEN, Map.of("default", ZERO), HUNDRED, HUNDRED, HUNDRED)
            );

            EvaluationResult result = handler.evaluateResult(player);

            assertEquals(ResultType.ALLOW, result.type());
            assertEquals(MAX_VALUE, result.assignedSize());
            assertEquals(MAX_VALUE, result.assignedLimit());
        }
    }

    static Stream<Arguments> evaluateResultData() {
        Region tinyRegion = cuboid(0, 0, 0);
        Region hugeRegion = cuboid(2000, 2000, 2000);
        Region shortHorizontalRegion = cuboid(9, 9, 1);
        Region shortVerticalRegion = cuboid(9, 1, 9);
        Region cubeRegion = cuboid(9, 9, 9);

        return Stream.of(
                // volume above Integer.MAX_VALUE denies
                Arguments.of(
                        hugeRegion,
                        blockLimits(0, 0, 0, 0),
                        ResultType.DENY_MAX_VOLUME, BigInteger.valueOf(hugeRegion.getVolume()), MAX_VALUE
                ),
                // volume below minimal volume
                Arguments.of(
                        tinyRegion,
                        blockLimits(100, 0, 0, 0),
                        ResultType.DENY_MIN_VOLUME, BigInteger.ONE, BigInteger.valueOf(100)
                ),
                // horizontal distance below minimal horizontal
                Arguments.of(
                        shortHorizontalRegion,
                        blockLimits(50, 5, 1, 0),
                        ResultType.DENY_HORIZONTAL, BigInteger.valueOf(2), BigInteger.valueOf(5)
                ),
                // vertical distance below minimal vertical
                Arguments.of(
                        shortVerticalRegion,
                        blockLimits(50, 5, 5, 0),
                        ResultType.DENY_VERTICAL, BigInteger.valueOf(2), BigInteger.valueOf(5)
                ),
                // volume above the player's block limit
                Arguments.of(
                        cubeRegion,
                        blockLimits(50, 5, 5, 500),
                        ResultType.DENY_MAX_VOLUME, BigInteger.valueOf(cubeRegion.getVolume()), BigInteger.valueOf(500)
                ),
                // everything within limits
                Arguments.of(
                        cubeRegion,
                        blockLimits(50, 5, 5, 5000),
                        ResultType.ALLOW, MAX_VALUE, MAX_VALUE
                )
        );
    }

    @ParameterizedTest
    @MethodSource("evaluateResultData")
    void evaluateResultTest(
            Region region, ConfigurationProvider.BlockLimits blockLimits,
            ResultType expectedType, BigInteger expectedSize, BigInteger expectedLimit
    ) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        PermissionsResolverManager resolver = mock(PermissionsResolverManager.class);

        try (
                var weUtils = mockStatic(WEUtils.class);
                var permissions = setupPermissions(resolver, List.of("default"))
        ) {
            weUtils.when(() -> WEUtils.getSelection(player)).thenReturn(region);

            BlockLimitsHandler handler = createHandler(blockLimits);
            EvaluationResult result = createHandler(blockLimits).evaluateResult(player);

            assertEquals(expectedType, result.type());
            assertEquals(expectedSize, result.assignedSize());
            assertEquals(expectedLimit, result.assignedLimit());
        }
    }

    private static MockedStatic<PermissionsResolverManager> setupPermissions(
            PermissionsResolverManager resolver, List<String> groups
    ) {
        when(resolver.getGroups(any(OfflinePlayer.class))).thenReturn(groups.toArray(new String[0]));

        MockedStatic<PermissionsResolverManager> permissions = mockStatic(PermissionsResolverManager.class);
        permissions.when(PermissionsResolverManager::getInstance).thenReturn(resolver);

        return permissions;
    }

    private static BlockLimitsHandler createHandler(ConfigurationProvider.BlockLimits blockLimits) {
        ConfigurationProvider cfgProvider = mock(ConfigurationProvider.class);
        ConfigurationProvider.Claim claim = new ConfigurationProvider.Claim(true, true, blockLimits);
        when(cfgProvider.claimCfg()).thenReturn(claim);
        return new BlockLimitsHandler(cfgProvider);
    }

    private static ConfigurationProvider.BlockLimits blockLimits(
            BigInteger defaultLimit,
            Map<String, BigInteger> limits,
            BigInteger minimalVolume,
            BigInteger minimalHorizontal,
            BigInteger minimalVertical
    ) {
        return new ConfigurationProvider.BlockLimits(
                true, defaultLimit, limits, minimalVolume, minimalHorizontal, minimalVertical
        );
    }

    private static ConfigurationProvider.BlockLimits blockLimits(
            long minimalVolume, long minimalHorizontal, long minimalVertical, long groupLimit
    ) {
        return blockLimits(
                TEN, Map.of("default", BigInteger.valueOf(groupLimit)),
                BigInteger.valueOf(minimalVolume), BigInteger.valueOf(minimalHorizontal), BigInteger.valueOf(minimalVertical)
        );
    }

    private static Region cuboid(int maxX, int maxY, int maxZ) {
        return new CuboidRegion(BlockVector3.at(0, 0, 0), BlockVector3.at(maxX, maxY, maxZ));
    }
}