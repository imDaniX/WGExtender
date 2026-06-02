/**
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

package wgextender.config;

import com.sk89q.worldguard.protection.flags.Flag;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import wgextender.config.message.Messages;
import wgextender.utils.WGUtils;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;

public final class Config {
    private static final DateTimeFormatter BACKUP_FROMATTER = DateTimeFormatter.ofPattern("ddMMyyyy-HHmmss");
    private static final String VERSION_KEY = "_version";
    private static final String CONFIG_FILE = "config.yml";

    private static final TypeSerializer<BigInteger> BIG_INTEGER_SERIALIZER = TypeSerializer.of(
            BigInteger.class,
            (value, typeSupported) -> value.toString(),
            raw -> switch (raw) {
                case BigInteger bigInteger -> bigInteger;
                case Number number -> BigInteger.valueOf(number.longValue());
                case String text -> {
                    if (text.isEmpty()) {
                        yield BigInteger.ZERO;
                    }
                    try {
                        yield new BigInteger(text);
                    } catch (NumberFormatException e) {
                        throw new SerializationException(e);
                    }
                }
                default -> BigInteger.ZERO;
            }
    );

    private final Plugin plugin;
    private final File configFile;
    private final Messages msg;

    private ClaimSettings claim = ClaimSettings.DEFAULTS;
    private MiscSettings misc = MiscSettings.DEFAULTS;
    private boolean extendedWorldEditWand = false;
    private WorldSettings defaultWorld = new WorldSettings(RegionProtection.DEFAULTS, AutoFlags.DEFAULTS, RestrictedCommands.DEFAULTS);
    private Map<String, WorldSettings> worlds = Map.of();

    public Config(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), CONFIG_FILE);
        this.msg = new Messages(plugin);
    }

    public void loadConfig() {
        ensureExists();
        try {
            loadAndMigrate();
        } catch (ConfigurateException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load config.yml", e);
        }
    }

    public @NotNull ClaimSettings claim() {
        return claim;
    }

    public @NotNull MiscSettings misc() {
        return misc;
    }

    public boolean extendedWorldEditWand() {
        return extendedWorldEditWand;
    }

    public @NotNull WorldSettings forWorld(@NotNull String world) {
        WorldSettings ws = worlds.get(world.toLowerCase(Locale.ROOT));
        return ws != null ? ws : defaultWorld;
    }

    public @NotNull WorldSettings forWorld(@NotNull World world) {
        return forWorld(world.getName());
    }

    public @NotNull Messages messages() {
        return msg;
    }

    private @NotNull YamlConfigurationLoader loader() {
        return YamlConfigurationLoader.builder()
                .path(configFile.toPath())
                .defaultOptions(opt -> opt.serializers(ser -> ser.register(BigInteger.class, BIG_INTEGER_SERIALIZER)))
                .build();
    }

    private void loadAndMigrate() throws ConfigurateException {
        YamlConfigurationLoader loader = loader();
        CommentedConfigurationNode root = migrate(loader, loader.load());

        this.claim = root.node("claim").get(ClaimSettings.class, ClaimSettings.DEFAULTS);
        this.misc = root.node("misc").get(MiscSettings.class, MiscSettings.DEFAULTS);
        this.extendedWorldEditWand = root.node("extendedwewand").getBoolean(false);
        loadWorlds(root);
        loadMessages(root);
    }

    private void loadWorlds(@NotNull ConfigurationNode root) throws ConfigurateException {
        ConfigurationNode regionNode = root.node("regionprotect");
        ConfigurationNode autoNode = root.node("autoflags");
        ConfigurationNode restrictNode = root.node("restrictcommands");

        RegionProtection regionDef = regionNode.get(RegionProtection.class, RegionProtection.DEFAULTS);
        AutoFlags autoDef = autoNode.get(AutoFlags.class, AutoFlags.DEFAULTS);
        RestrictedCommands restrictDef = restrictNode.get(RestrictedCommands.class, RestrictedCommands.DEFAULTS);

        Map<String, RegionProtection> regionPer = loadPerWorld(regionNode, RegionProtection.class, regionDef);
        Map<String, AutoFlags> autoPer = loadPerWorld(autoNode, AutoFlags.class, autoDef);
        Map<String, RestrictedCommands> restrictPer = loadPerWorld(restrictNode, RestrictedCommands.class, restrictDef);

        Set<String> names = new HashSet<>();
        names.addAll(regionPer.keySet());
        names.addAll(autoPer.keySet());
        names.addAll(restrictPer.keySet());

        Map<String, WorldSettings> map = new HashMap<>();
        for (String name : names) {
            map.put(name, new WorldSettings(
                    regionPer.getOrDefault(name, regionDef),
                    autoPer.getOrDefault(name, autoDef),
                    restrictPer.getOrDefault(name, restrictDef)
            ));
        }

        this.defaultWorld = new WorldSettings(regionDef, autoDef, restrictDef);
        this.worlds = Map.copyOf(map);
    }

    private <T> @NotNull Map<String, T> loadPerWorld(@NotNull ConfigurationNode feature, @NotNull Class<T> type, @NotNull T fallback) throws ConfigurateException {
        Map<String, T> map = new HashMap<>();
        for (Map.Entry<Object, ? extends ConfigurationNode> e : feature.node("per-world").childrenMap().entrySet()) {
            ConfigurationNode merged = e.getValue().copy().mergeFrom(feature);
            map.put(String.valueOf(e.getKey()).toLowerCase(Locale.ROOT), merged.get(type, fallback));
        }
        return map;
    }

    private void loadMessages(@NotNull ConfigurationNode config) {
        msg.setDecoder(switch (config.node("messages", "serializer").getString("LEGACY_AMPERSAND").toUpperCase(Locale.ROOT)) {
            case "MINIMESSAGE", "MINI_MESSAGE" -> MiniMessage.miniMessage();
            case "LEGACY_SECTION" -> LegacyComponentSerializer.legacySection();
            default -> LegacyComponentSerializer.legacyAmpersand();
        });
        msg.loadMessages(config.node("messages", "locale").getString("en"));
    }

    private void ensureExists() {
        if (configFile.exists()) return;
        InputStream in = plugin.getResource(CONFIG_FILE);
        if (in == null) {
            plugin.getLogger().warning("Bundled " + CONFIG_FILE + " not found; cannot create default config.");
            return;
        }
        try (InputStream src = in) {
            Files.createDirectories(configFile.getParentFile().toPath());
            Files.copy(src, configFile.toPath());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to write default config.yml", e);
        }
    }

    private @NotNull CommentedConfigurationNode migrate(@NotNull YamlConfigurationLoader loader, @NotNull CommentedConfigurationNode root) throws ConfigurateException {
        CommentedConfigurationNode bundled = loadBundledDefaults();
        if (bundled == null) return root;

        int currentVersion = root.node(VERSION_KEY).getInt(0);
        int bundledVersion = bundled.node(VERSION_KEY).getInt(0);
        if (bundledVersion <= currentVersion) return root;

        plugin.getLogger().info("Updating config.yml: v" + currentVersion + " -> v" + bundledVersion);
        if (!backupConfig(currentVersion)) return root;

        root.mergeFrom(bundled);
        root.node(VERSION_KEY).set(bundledVersion);
        loader.save(root);
        return root;
    }

    private @Nullable CommentedConfigurationNode loadBundledDefaults() {
        InputStream in = plugin.getResource(CONFIG_FILE);
        if (in == null) {
            plugin.getLogger().warning("Bundled " + CONFIG_FILE + " not found; skipping migration.");
            return null;
        }
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .source(() -> new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
                .build();
        try {
            return loader.load();
        } catch (ConfigurateException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to read bundled " + CONFIG_FILE, e);
            return null;
        }
    }

    private boolean backupConfig(int oldVersion) {
        File backup = new File(plugin.getDataFolder(), "config-v" + oldVersion + "-" + LocalDateTime.now().format(BACKUP_FROMATTER) + ".yml.bak");
        if (backup.exists()) {
            plugin.getLogger().info("Backup " + backup.getName() + " already exists; keeping it.");
            return true;
        }
        try {
            Files.copy(configFile.toPath(), backup.toPath());
            plugin.getLogger().info("Backed up old config to " + backup.getName());
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to back up config.yml; aborting migration", e);
            return false;
        }
    }

    @ConfigSerializable
    public record ClaimSettings(
            @Setting("vertexpand") boolean expandSelectionVertical,
            @Setting("blocklimits") BlockLimits blockLimits
    ) {
        public static final ClaimSettings DEFAULTS = new ClaimSettings(false, BlockLimits.DEFAULTS);

        public ClaimSettings {
            if (blockLimits == null) blockLimits = BlockLimits.DEFAULTS;
        }

		public @NotNull BigInteger limitFor(@NotNull String group, @NotNull BigInteger def) {
			BigInteger value = blockLimits.limits().get(group.toLowerCase(Locale.ROOT));
			return value != null ? value : def;
		}

        public @NotNull BigInteger limitFor(@NotNull String group) {
			BigInteger value = blockLimits.limits().get(group.toLowerCase(Locale.ROOT));
			return value != null ? value : blockLimits.defaultLimit();
        }

        @ConfigSerializable
        public record BlockLimits(boolean enabled, Map<String, BigInteger> limits, Minimal minimal) {
            public static final BlockLimits DEFAULTS = new BlockLimits(false, Map.of(), Minimal.DEFAULTS);

            public BlockLimits {
                if (limits == null) limits = new LinkedHashMap<>();
                if (minimal == null) minimal = Minimal.DEFAULTS;
            }

            public @NotNull BigInteger defaultLimit() {
                return limits.getOrDefault("default", BigInteger.ZERO);
            }
        }

        @ConfigSerializable
        public record Minimal(BigInteger volume, BigInteger horizontal, BigInteger vertical) {
            public static final Minimal DEFAULTS = new Minimal(BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO);

            public Minimal {
                if (volume == null) volume = BigInteger.ZERO;
                if (horizontal == null) horizontal = BigInteger.ZERO;
                if (vertical == null) vertical = BigInteger.ZERO;
            }
        }
    }

    @ConfigSerializable
    public record MiscSettings(
			@Setting("pvpmode") String pvpMode,
			@Setting("old-pvp-flags") boolean oldPvpFlags
	) {
        public static final MiscSettings DEFAULTS = new MiscSettings("default", true);

        public MiscSettings {
            if (pvpMode == null) pvpMode = "default";
        }

        public @Nullable Boolean pvpFlagMode() {
            return switch (pvpMode.toLowerCase(Locale.ROOT)) {
                case "allow" -> Boolean.TRUE;
                case "deny" -> Boolean.FALSE;
                default -> null;
            };
        }
    }

    public record WorldSettings(
            RegionProtection regionProtection,
            AutoFlags autoFlags,
            RestrictedCommands restrictedCommands
    ) { }

    @ConfigSerializable
    public record RegionProtection(Flow flow, Fire fire, Explosion explosion) {
        public static final RegionProtection DEFAULTS = new RegionProtection(Flow.DEFAULTS, Fire.DEFAULTS, Explosion.DEFAULTS);

        public RegionProtection {
            if (flow == null) flow = Flow.DEFAULTS;
            if (fire == null) fire = Fire.DEFAULTS;
            if (explosion == null) explosion = Explosion.DEFAULTS;
        }

        @ConfigSerializable
        public record Flow(boolean lava, boolean water, boolean other) {
            public static final Flow DEFAULTS = new Flow(false, false, false);
        }

        @ConfigSerializable
        public record Fire(Spread spread, boolean burn) {
            public static final Fire DEFAULTS = new Fire(Spread.DEFAULTS, false);

            public Fire {
                if (spread == null) spread = Spread.DEFAULTS;
            }

            @ConfigSerializable
            public record Spread(
                    @Setting("toregion") boolean toRegion,
                    @Setting("inregion") boolean inRegion
            ) {
                public static final Spread DEFAULTS = new Spread(false, false);
            }
        }

        @ConfigSerializable
        public record Explosion(
				boolean block,
				boolean entity,
				@Setting("source-detection") SourceDetection sourceDetection
		) {
            public static final Explosion DEFAULTS = new Explosion(false, false, SourceDetection.DEFAULTS);

            public Explosion {
                if (sourceDetection == null) sourceDetection = SourceDetection.DEFAULTS;
            }

            @ConfigSerializable
            public record SourceDetection(
					@Setting("creeper-target") boolean creeperTarget,
					@Setting("tnt-prime") boolean tntPrime
			) {
                public static final SourceDetection DEFAULTS = new SourceDetection(false, true);
            }
        }
    }

    @ConfigSerializable
    public record AutoFlags(
			boolean enabled,
			@Setting("show-messages") boolean showMessages,
			Map<String, String> flags
	) {
        public static final AutoFlags DEFAULTS = new AutoFlags(false, false, Map.of());

        public AutoFlags {
            if (flags == null) flags = new LinkedHashMap<>();
        }

        public @NotNull Map<Flag<?>, String> resolvedFlags() {
            Map<Flag<?>, String> resolved = new LinkedHashMap<>();
            for (Map.Entry<String, String> e : flags.entrySet()) {
                Flag<?> flag = WGUtils.matchFlag(e.getKey());
                if (flag != null) {
                    resolved.put(flag, e.getValue());
                }
            }
            return resolved;
        }
    }

    @ConfigSerializable
    public record RestrictedCommands(
			boolean enabled,
			@Setting("recheck-ticks") int recheckTicks,
			List<String> commands
	) {
        public static final RestrictedCommands DEFAULTS = new RestrictedCommands(false, 100, List.of());

        public RestrictedCommands {
            if (recheckTicks <= 0) recheckTicks = 100;
            if (commands == null) commands = new ArrayList<>();
        }
    }
}