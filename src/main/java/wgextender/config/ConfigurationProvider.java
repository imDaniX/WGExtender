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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import wgextender.WGExtender;
import wgextender.config.message.MessagesProvider;
import wgextender.utils.CaseInsensitive;
import wgextender.utils.WGUtils;

import java.io.File;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ConfigurationProvider {
    private final WGExtender plugin;
    private final File configFile;
    private final MessagesProvider msgProvider;
    private final List<Consumer<ConfigurationProvider>> subscribers = new ArrayList<>();

    private Claim claim;
    private Protection protection;
    private AutoFlags autoFlags;
    private RestrictCommands restrictCommands;
    private Misc misc;
    private Messages messages;
    private Updater updater;

    public ConfigurationProvider(WGExtender plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.msgProvider = new MessagesProvider(plugin, this);
    }

    public void reload() {
        plugin.saveDefaultConfig();
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        this.claim = Claim.load(config);
        this.protection = Protection.load(config);
        this.autoFlags = AutoFlags.load(config, plugin.logger());
        this.restrictCommands = RestrictCommands.load(config);
        this.misc = Misc.load(config);
        this.messages = Messages.load(config);
        this.updater = Updater.load(config);
    }

    public void reloadSubscribers() {
        subscribers.forEach(sub -> sub.accept(this));
    }

    public <T> void register(@NotNull Configurable<T> reloadable, @NotNull Function<ConfigurationProvider, T> section) {
        Consumer<ConfigurationProvider> sub = provider -> reloadable.onReload(section.apply(provider));
        subscribers.add(sub);
    }

    public void register(@NotNull Configurable<ConfigurationProvider> reloadable) {
        register(reloadable, Function.identity());
    }

    public @NotNull MessagesProvider messageProvider() {
        return msgProvider;
    }

    public @NotNull Claim claimCfg() {
        return claim;
    }

    public @NotNull Protection protectionCfg() {
        return protection;
    }

    public @NotNull AutoFlags autoFlagsCfg() {
        return autoFlags;
    }

    public @NotNull RestrictCommands restrictCommandsCfg() {
        return restrictCommands;
    }

    public @NotNull Misc miscCfg() {
        return misc;
    }

    public @NotNull Messages messagesCfg() {
        return messages;
    }

    public @NotNull Updater updaterCfg() {
        return updater;
    }

    private static <T> T at(@NotNull ConfigurationSection config, @NotNull String path, @NotNull Function<ConfigurationSection, T> creator) {
        var section = config.getConfigurationSection(path);
        return creator.apply(section == null ? config.createSection(path) : section);
    }

    private static @NotNull BigInteger readBigInteger(@NotNull ConfigurationSection section, @NotNull String key) {
        if (section.isLong(key)) return BigInteger.valueOf(section.getLong(key));
        if (section.isDouble(key)) return BigInteger.valueOf((long) section.getDouble(key));
        String value = section.getString(key, "0");
        return value.equals("0") ? BigInteger.ZERO : new BigInteger(value);
    }

    public record Claim(
            boolean expandSelectionVertical,
            boolean hijackHandler,
            @NotNull BlockLimits blockLimits
    ) {
        public static final Function<ConfigurationProvider, Claim> SECTION = ConfigurationProvider::claimCfg;

        static @NotNull Claim load(@NotNull FileConfiguration config) {
            return at(config, "claim", claimSection -> new Claim(
                    claimSection.getBoolean("vertexpand", false),
                    claimSection.getBoolean("hijack-handler", true),
                    BlockLimits.load(claimSection)
            ));
        }
    }

    public record BlockLimits(
            boolean enabled,
            @NotNull BigInteger defaultLimit,
            @NotNull Map<String, BigInteger> limits,
            @NotNull BigInteger minimalVolume,
            @NotNull BigInteger minimalHorizontal,
            @NotNull BigInteger minimalVertical
    ) {
        public static final Function<ConfigurationProvider, BlockLimits> SECTION = cfg -> cfg.claimCfg().blockLimits();

        static @NotNull BlockLimits load(@NotNull ConfigurationSection claimSection) {
            return at(claimSection, "blocklimits", blockLimitsSection -> at(blockLimitsSection, "minimal", minimalSection -> {
                ConfigurationSection limitsSection = blockLimitsSection.getConfigurationSection("limits");
                Map<String, BigInteger> limits = CaseInsensitive.newMap();
                BigInteger defaultLimit = BigInteger.ZERO;
                if (limitsSection != null) {
                    defaultLimit = readBigInteger(limitsSection, "default");
                    for (String group : limitsSection.getKeys(false)) {
                        limits.put(group.toLowerCase(Locale.ROOT), readBigInteger(limitsSection, group));
                    }
                }
                // TODO Compare max limit with the WG's and warn the user
                return new BlockLimits(
                        blockLimitsSection.getBoolean("enabled", false),
                        defaultLimit,
                        Collections.unmodifiableMap(limits),
                        readBigInteger(minimalSection, "volume"),
                        readBigInteger(minimalSection, "horizontal"),
                        readBigInteger(minimalSection, "vertical")
                );
            }));
        }
    }

    public record Protection(@NotNull Flow flow, @NotNull Fire fire, @NotNull Explosion explosion) {
        public static final Function<ConfigurationProvider, Protection> SECTION = ConfigurationProvider::protectionCfg;

        static @NotNull Protection load(@NotNull FileConfiguration config) {
            return at(config, "regionprotect", protectionSection -> new Protection(
                    Flow.load(protectionSection),
                    Fire.load(protectionSection),
                    Explosion.load(protectionSection)
            ));
        }
    }

    public record Flow(boolean lava, boolean water, boolean other) {
        public static final Function<ConfigurationProvider, Flow> SECTION = cfg -> cfg.protectionCfg().flow();

        static @NotNull Flow load(@NotNull ConfigurationSection protectionSection) {
            return at(protectionSection, "flow", flowSection -> new Flow(
                    flowSection.getBoolean("lava", false),
                    flowSection.getBoolean("water", false),
                    flowSection.getBoolean("other", false)
            ));
        }
    }

    public record Fire(boolean spreadToRegion, boolean spreadInRegion, boolean burn) {
        public static final Function<ConfigurationProvider, Fire> SECTION = cfg -> cfg.protectionCfg().fire();

        static @NotNull Fire load(@NotNull ConfigurationSection protectionSection) {
            return at(protectionSection, "fire", fireSection -> new Fire(
                    fireSection.getBoolean("spread.toregion", false),
                    fireSection.getBoolean("spread.inregion", false),
                    fireSection.getBoolean("burn", false)
            ));
        }
    }

    public record Explosion(boolean block, boolean entity, boolean creeperTarget, boolean tntPrime, boolean endCrystalDamager) {
        public static final Function<ConfigurationProvider, Explosion> SECTION = cfg -> cfg.protectionCfg().explosion();

        static @NotNull Explosion load(@NotNull ConfigurationSection protectionSection) {
            return at(protectionSection, "explosion", explosionSection -> new Explosion(
                    explosionSection.getBoolean("block", false),
                    explosionSection.getBoolean("entity", false),
                    explosionSection.getBoolean("source-detection.creeper-target", false),
                    explosionSection.getBoolean("source-detection.tnt-prime", false),
                    explosionSection.getBoolean("source-detection.end-crystal-damager", false)
            ));
        }
    }

    public record AutoFlags(boolean enabled, boolean showMessages, @NotNull Map<Flag<?>, String> flags) {
        public static final Function<ConfigurationProvider, AutoFlags> SECTION = ConfigurationProvider::autoFlagsCfg;

        static @NotNull AutoFlags load(@NotNull FileConfiguration config, @NotNull Logger logger) {
            return at(config, "autoflags", autoFlagsSection -> new AutoFlags(
                    autoFlagsSection.getBoolean("enabled", false),
                    autoFlagsSection.getBoolean("show-messages", false),
                    at(autoFlagsSection, "flags", flagsSection -> {
                        Map<Flag<?>, String> flags = new HashMap<>();
                        for (String key : flagsSection.getKeys(false)) {
                            Flag<?> flag = WGUtils.matchFlag(key);
                            if (flag != null) {
                                flags.put(flag, flagsSection.getString(key));
                            } else {
                                logger.warn("Unknown flag provided for autoflags: {}", key);
                            }
                        }
                        return Map.copyOf(flags);
                    })
            ));
        }
    }

    public record RestrictCommands(
            boolean enabled,
            boolean aliasedSearch,
            boolean prefixedSearch,
            int recheckTicks,
            List<String> commands
    ) {
        public static final Function<ConfigurationProvider, RestrictCommands> SECTION = ConfigurationProvider::restrictCommandsCfg;

        static @NotNull RestrictCommands load(@NotNull FileConfiguration config) {
            return at(config, "restrictcommands", rcSection -> new RestrictCommands(
                    rcSection.getBoolean("enabled", false),
                    rcSection.getBoolean("aliased-search", true),
                    rcSection.getBoolean("prefixed-search", true),
                    rcSection.getInt("recheck-ticks", 100),
                    List.copyOf(rcSection.getStringList("commands"))
            ));
        }
    }

    public record Misc(boolean extendedWeWand, @Nullable State pvpMode, boolean oldPvpFlags) {
        public static final Function<ConfigurationProvider, Misc> SECTION = ConfigurationProvider::miscCfg;

        static @NotNull Misc load(@NotNull FileConfiguration config) {
            return at(config, "misc", miscSection -> new Misc(
                    config.getBoolean("extendedwewand", false),
                    switch (miscSection.getString("pvpmode", "default").toLowerCase(Locale.ROOT)) {
                        case "allow" -> State.ALLOW;
                        case "deny" -> State.DENY;
                        default -> null;
                    },
                    miscSection.getBoolean("old-pvp-flags", false)
            ));
        }
    }

    public record Messages(@NotNull String serializer, @NotNull String locale) {
        public static final Function<ConfigurationProvider, Messages> SECTION = ConfigurationProvider::messagesCfg;

        static @NotNull Messages load(@NotNull FileConfiguration config) {
            return at(config, "messages", messagesSection -> new Messages(
                    messagesSection.getString("serializer", "LEGACY"),
                    messagesSection.getString("locale", "en")
            ));
        }
    }

    public record Updater(
            boolean enabled,
            int checkInterval,
            boolean joinNotify,
            boolean intervalNotify,
            boolean allowStaging,
            @NotNull String baseUrl,
            boolean logFailures
    ) {
        public static final Function<ConfigurationProvider, Updater> SECTION = ConfigurationProvider::updaterCfg;

        static @NotNull Updater load(@NotNull FileConfiguration config) {
            return at(config, "updater", updaterSection -> at(updaterSection, "notify", notifySection -> new Updater(
                    updaterSection.getBoolean("enabled", true),
                    updaterSection.getInt("check-interval", 86400),
                    notifySection.getBoolean("on-join", true),
                    notifySection.getBoolean("on-interval", false),
                    updaterSection.getBoolean("allow-staging", false),
                    updaterSection.getString("url-base", "https://api.modrinth.com"),
                    updaterSection.getBoolean("log-failures", true)
            )));
        }
    }
}