package wgextender.features.claimcommand;

import com.sk89q.wepif.PermissionsResolverManager;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import wgextender.config.ConfigurationProvider;
import wgextender.config.section.BlockLimits;
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
        Player player = mockPlayer();

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
        Player player = mockPlayer();
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
    void worldSpecificLimitTest() {
        Player overworld = mockPlayer("world");
        when(overworld.getUniqueId()).thenReturn(UUID.randomUUID());
        Player nether = mockPlayer("nether");
        when(nether.getUniqueId()).thenReturn(UUID.randomUUID());

        PermissionsResolverManager resolver = mock(PermissionsResolverManager.class);

        try (var permissions = setupPermissions(resolver, List.of("vip"))) {
            ConfigurationProvider cfgProvider = mock(ConfigurationProvider.class);
            var general = blockLimits(TEN, Map.of("vip", HUNDRED), ZERO, ZERO, ZERO);
            var netherCfg = blockLimits(TEN, Map.of("vip", FIFTY), ZERO, ZERO, ZERO);
            when(cfgProvider.section(eq(BlockLimits.POINTER), nullable(String.class))).thenReturn(general);
            when(cfgProvider.section(BlockLimits.POINTER, "nether")).thenReturn(netherCfg);
            BlockLimitsHandler handler = new BlockLimitsHandler(cfgProvider);

            assertEquals(HUNDRED, handler.cachedBlockLimit(overworld));
            assertEquals(FIFTY, handler.cachedBlockLimit(nether));
            assertEquals(HUNDRED, handler.groupBlockLimit("vip"));
            assertEquals(FIFTY, handler.groupBlockLimit("vip", "nether"));
        }
    }

    @Test
    void worldSpecificCacheTest() {
        Player player = mockPlayer("world");
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        PermissionsResolverManager resolver = mock(PermissionsResolverManager.class);

        try (var permissions = setupPermissions(resolver, List.of("vip"))) {
            BlockLimitsHandler handler = createHandler(blockLimits(TEN, Map.of("vip", HUNDRED), ZERO, ZERO, ZERO));

            handler.cachedBlockLimit(player, "nether");
            handler.cachedBlockLimit(player, "NETHER"); // world names are case-insensitive
            verify(resolver, times(1)).getGroups(player);

            handler.cachedBlockLimit(player); // "world" is cached separately
            handler.cachedBlockLimit(player, null); // so is the general one
            verify(resolver, times(3)).getGroups(player);

            handler.refreshBlockLimit(player, "nether");
            verify(resolver, times(4)).getGroups(player);
            handler.cachedBlockLimit(player, "nether");
            verify(resolver, times(4)).getGroups(player); // refreshed value is cached
        }
    }

    @Test
    void reloadClearsCache() {
        Player player = mockPlayer();
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        PermissionsResolverManager resolver = mock(PermissionsResolverManager.class);

        try (var permissions = setupPermissions(resolver, List.of("vip"))) {
            BlockLimitsHandler handler = createHandler(blockLimits(TEN, Map.of("vip", HUNDRED), ZERO, ZERO, ZERO));

            handler.cachedBlockLimit(player);
            handler.onReload(blockLimits(TEN, Map.of("vip", FIFTY), ZERO, ZERO, ZERO));
            handler.cachedBlockLimit(player);

            verify(resolver, times(2)).getGroups(player); // the cache was cleared in between
        }
    }

    @Test
    void refreshBlockLimitTest() {
        Player player = mockPlayer();
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
        Player player = mockPlayer();

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
        Player player = mockPlayer();
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
            Region region, BlockLimits blockLimits,
            ResultType expectedType, BigInteger expectedSize, BigInteger expectedLimit
    ) {
        Player player = mockPlayer();
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

    private static Player mockPlayer() {
        return mockPlayer("world");
    }

    private static Player mockPlayer(String worldName) {
        World world = mock(World.class);
        when(world.getName()).thenReturn(worldName);
        Player player = mock(Player.class);
        when(player.getWorld()).thenReturn(world);
        return player;
    }

    private static BlockLimitsHandler createHandler(BlockLimits blockLimits) {
        ConfigurationProvider cfgProvider = mock(ConfigurationProvider.class);
        when(cfgProvider.section(eq(BlockLimits.POINTER), nullable(String.class))).thenReturn(blockLimits);
        return new BlockLimitsHandler(cfgProvider);
    }

    private static BlockLimits blockLimits(
            BigInteger defaultLimit,
            Map<String, BigInteger> limits,
            BigInteger minimalVolume,
            BigInteger minimalHorizontal,
            BigInteger minimalVertical
    ) {
        return new BlockLimits(
                true, defaultLimit, limits, minimalVolume, minimalHorizontal, minimalVertical
        );
    }

    private static BlockLimits blockLimits(
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