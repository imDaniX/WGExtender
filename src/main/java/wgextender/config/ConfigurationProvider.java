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
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wgextender.WGExtender;
import wgextender.config.message.Messages;
import wgextender.utils.WGUtils;

import java.io.File;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ConfigurationProvider {
    public static final Function<ConfigurationProvider, Void> VOID_SECTION = cfg -> null;

    private final Plugin plugin;
    private final File configFile;
    private final Messages messages;
    private final List<Consumer<ConfigurationProvider>> subscribers = new ArrayList<>();

    private Claim claim;
    private Protection protection;
    private AutoFlags autoFlags;
    private RestrictCommands restrictCommands;
    private Misc misc;

    public ConfigurationProvider(WGExtender plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.messages = new Messages(plugin);
    }

    public void reload() {
        plugin.saveDefaultConfig();
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        this.claim = loadClaim(config);
        this.protection = loadProtection(config);
        this.autoFlags = loadAutoFlags(config);
        this.restrictCommands = loadRestrictCommands(config);
        this.misc = loadMisc(config);
        loadMessages(config);
        subscribers.forEach(s -> s.accept(this));
    }

    public <T> void register(@NotNull Configurable<T> reloadable, @NotNull Function<ConfigurationProvider, T> section) {
        subscribers.add(provider -> reloadable.onReload(section.apply(provider)));
    }

    public void register(@NotNull Configurable<ConfigurationProvider> reloadable) {
        register(reloadable, Function.identity());
    }

    public @NotNull Claim claim() {
        return claim;
    }

    public @NotNull Protection protection() {
        return protection;
    }

    public @NotNull AutoFlags autoFlags() {
        return autoFlags;
    }

    public @NotNull RestrictCommands restrictCommands() {
        return restrictCommands;
    }

    public @NotNull Misc misc() {
        return misc;
    }

    public @NotNull Messages messages() {
        return messages;
    }

    private @NotNull Claim loadClaim(@NotNull FileConfiguration config) {
        return at(config, "claim", claimSection -> new Claim(
                claimSection.getBoolean("vertexpand", false),
                loadBlockLimits(claimSection)
        ));
    }

    private @NotNull BlockLimits loadBlockLimits(@NotNull ConfigurationSection claimSection) {
        return at(claimSection, "blocklimits", blockLimitsSection -> at(blockLimitsSection, "minimal", minimalSection -> {
            ConfigurationSection limitsSection = blockLimitsSection.getConfigurationSection("limits");
            Map<String, BigInteger> limits = new LinkedHashMap<>();
            BigInteger defaultLimit = BigInteger.ZERO;
            if (limitsSection != null) {
                defaultLimit = readBigInteger(limitsSection, "default");
                for (String group : limitsSection.getKeys(false)) {
                    limits.put(group.toLowerCase(Locale.ROOT), readBigInteger(limitsSection, group));
                }
            }
            return new BlockLimits(
                    blockLimitsSection.getBoolean("enabled", false),
                    defaultLimit,
                    limits,
                    readBigInteger(minimalSection, "volume"),
                    readBigInteger(minimalSection, "horizontal"),
                    readBigInteger(minimalSection, "vertical")
            );
        }));
    }

    private @NotNull Protection loadProtection(@NotNull FileConfiguration config) {
        return at(config, "regionprotect", protectionSection -> new Protection(
                at(protectionSection, "flow", flowSection -> new Flow(
                        flowSection.getBoolean("lava", false),
                        flowSection.getBoolean("water", false),
                        flowSection.getBoolean("other", false)
                )),
                at(protectionSection, "fire", fireSection -> new Fire(
                        fireSection.getBoolean("spread.toregion", false),
                        fireSection.getBoolean("spread.inregion", false),
                        fireSection.getBoolean("burn", false)
                )),
                at(protectionSection, "explosion", explosionSection -> new Explosion(
                        explosionSection.getBoolean("block", false),
                        explosionSection.getBoolean("entity", false),
                        explosionSection.getBoolean("source-detection.creeper-target", false),
                        explosionSection.getBoolean("source-detection.tnt-prime", true)
                ))
        ));
    }

    private @NotNull AutoFlags loadAutoFlags(@NotNull FileConfiguration config) {
        return at(config, "autoflags", autoFlagsSection -> {
            Map<Flag<?>, String> flags = new LinkedHashMap<>();
            ConfigurationSection flagsSection = autoFlagsSection.getConfigurationSection("flags");
            if (flagsSection != null) {
                for (String key : flagsSection.getKeys(false)) {
                    Flag<?> flag = WGUtils.matchFlag(key);
                    if (flag != null) flags.put(flag, flagsSection.getString(key));
                }
            }
            return new AutoFlags(
                    autoFlagsSection.getBoolean("enabled", false),
                    autoFlagsSection.getBoolean("show-messages", false),
                    flags
            );
        });
    }

    private @NotNull RestrictCommands loadRestrictCommands(@NotNull FileConfiguration config) {
        return at(config, "restrictcommands", rcSection -> new RestrictCommands(
                rcSection.getBoolean("enabled", false),
                rcSection.getBoolean("aliased-search", true),
                rcSection.getBoolean("prefixed-search", true),
                rcSection.getInt("recheck-ticks", 100),
                new ArrayList<>(rcSection.getStringList("commands"))
        ));
    }

    private @NotNull Misc loadMisc(@NotNull FileConfiguration config) {
        return at(config, "misc", miscSection -> {
            State pvpMode = switch (miscSection.getString("pvpmode", "default").toLowerCase(Locale.ROOT)) {
                case "allow" -> State.ALLOW;
                case "deny" -> State.DENY;
                default -> null;
            };
            return new Misc(
                    config.getBoolean("extendedwewand", false),
                    pvpMode,
                    miscSection.getBoolean("old-pvp-flags", true)
            );
        });
    }

    private void loadMessages(@NotNull FileConfiguration config) {
        messages.setDecoder(switch (config.getString("messages.serializer", "LEGACY_AMPERSAND").toUpperCase(Locale.ROOT)) {
            case "MINIMESSAGE", "MINI_MESSAGE" -> MiniMessage.miniMessage();
            case "LEGACY_SECTION" -> LegacyComponentSerializer.legacySection();
            default -> LegacyComponentSerializer.legacyAmpersand();
        });
        messages.loadMessages(config.getString("messages.locale", "en"));
    }

    private static <T> T at(ConfigurationSection config, String path, Function<ConfigurationSection, T> creator) {
        var section = config.getConfigurationSection(path);
        return creator.apply(section == null ? config.createSection(path) : section);
    }

    private static @NotNull BigInteger readBigInteger(@NotNull ConfigurationSection section, @NotNull String key) {
        if (section.isLong(key)) return BigInteger.valueOf(section.getLong(key));
        if (section.isInt(key)) return BigInteger.valueOf(section.getInt(key));
        String value = section.getString(key, "0");
        return value.equals("0") ? BigInteger.ZERO : new BigInteger(value);
    }

    public record Claim(boolean expandSelectionVertical, BlockLimits blockLimits) {
        public static final Function<ConfigurationProvider, Claim> SECTION = ConfigurationProvider::claim;
    }

    public record BlockLimits(
            boolean enabled,
            BigInteger defaultLimit,
            Map<String, BigInteger> limits,
            BigInteger minimalVolume,
            BigInteger minimalHorizontal,
            BigInteger minimalVertical
    ) {
        public static final Function<ConfigurationProvider, BlockLimits> SECTION = cfg -> cfg.claim().blockLimits();
    }

    public record Protection(Flow flow, Fire fire, Explosion explosion) {
        public static final Function<ConfigurationProvider, Protection> SECTION = ConfigurationProvider::protection;
    }

    public record Flow(boolean lava, boolean water, boolean other) {
        public static final Function<ConfigurationProvider, Flow> SECTION = cfg -> cfg.protection().flow();
    }

    public record Fire(boolean spreadToRegion, boolean spreadInRegion, boolean burn) {
        public static final Function<ConfigurationProvider, Fire> SECTION = cfg -> cfg.protection().fire();
    }

    public record Explosion(boolean block, boolean entity, boolean creeperTarget, boolean tntPrime) {
        public static final Function<ConfigurationProvider, Explosion> SECTION = cfg -> cfg.protection().explosion();
    }

    public record AutoFlags(boolean enabled, boolean showMessages, Map<Flag<?>, String> flags) {
        public static final Function<ConfigurationProvider, AutoFlags> SECTION = ConfigurationProvider::autoFlags;
    }

    public record RestrictCommands(
            boolean enabled,
            boolean aliasedSearch,
            boolean prefixedSearch,
            int recheckTicks,
            List<String> commands
    ) {
        public static final Function<ConfigurationProvider, RestrictCommands> SECTION = ConfigurationProvider::restrictCommands;
    }

    public record Misc(boolean extendedWeWand, @Nullable State pvpMode, boolean oldPvpFlags) {
        public static final Function<ConfigurationProvider, Misc> SECTION = ConfigurationProvider::misc;
    }
}