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

package wgextender;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;
import wgextender.command.impl.WGExCommand;
import wgextender.config.ConfigurationProvider;
import wgextender.external.updater.ModrinthUpdater;
import wgextender.features.VersionHandler;
import wgextender.features.claimcommand.BlockLimitsHandler;
import wgextender.features.claimcommand.WGRegionCommandWrapper;
import wgextender.features.extendedwand.WEWand;
import wgextender.features.extendedwand.WEWandCommandWrapper;
import wgextender.features.extendedwand.WEWandHandler;
import wgextender.features.flags.ConsumeFlagsHandler;
import wgextender.features.flags.MobRenameFlagHandler;
import wgextender.features.flags.OldPVPFlagsHandler;
import wgextender.features.flags.WGExtenderFlags;
import wgextender.features.regionprotect.ownormembased.PvPModeHandler;
import wgextender.features.regionprotect.ownormembased.RestrictCommandsHandler;
import wgextender.features.regionprotect.regionbased.Explode;
import wgextender.features.regionprotect.regionbased.FireBurn;
import wgextender.features.regionprotect.regionbased.LiquidFlow;
import wgextender.integration.LpIntegration;
import wgextender.integration.PapiIntegration;
import wgextender.integration.PluginIntegration;
import wgextender.utils.Injectable;

import java.util.*;
import java.util.logging.Logger;

// TODO Might wanna separate for the actual API?
public final class WGExtender extends JavaPlugin {
    private final List<PluginIntegration> integrations = new ArrayList<>();
    private final List<Injectable> injectables = new ArrayList<>();
    private ModrinthUpdater updater;
    private ConfigurationProvider cfgProvider;
    private BlockLimitsHandler blockLimitsHandler;

    @ApiStatus.Internal
    public @UnknownNullability ConfigurationProvider getConfigurationProvider() {
        return cfgProvider;
    }

    public @UnknownNullability BlockLimitsHandler getBlockLimitsHandler() {
        return blockLimitsHandler;
    }

    public @UnknownNullability ModrinthUpdater getUpdater() {
        return updater;
    }

    @ApiStatus.Internal
    @Override
    public void onLoad() {
        updater = new ModrinthUpdater("JFMgRt9t", getPluginMeta().getVersion());
        WGExtenderFlags.registerFlags(logger());
        integrations.add(new LpIntegration());
        integrations.add(new PapiIntegration(this));
    }

    @ApiStatus.Internal
    @Override
    public void onEnable() {
        cfgProvider = new ConfigurationProvider(this);
        cfgProvider.reload();

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(new WGExCommand(this).node().build(), List.of("wgextender"));
        });

        WEWand weWand = new WEWand();

        listener(blockLimitsHandler = new BlockLimitsHandler(cfgProvider));
        listener(new VersionHandler(this));
        listener(new MobRenameFlagHandler(cfgProvider));
        listener(new RestrictCommandsHandler(this));
        listener(new LiquidFlow(cfgProvider));
        listener(new FireBurn(cfgProvider));
        listener(new Explode(cfgProvider));
        listener(new WEWandHandler(cfgProvider, weWand));
        listener(new ConsumeFlagsHandler(cfgProvider));
        listener(new PvPModeHandler(cfgProvider));

        injectables.add(new WGRegionCommandWrapper(this));
        injectables.add(new WEWandCommandWrapper(cfgProvider, weWand));
        if (cfgProvider.miscCfg().oldPvpFlags()) {
            logger().warn(
                    "Enabling the old-PvP flags. Do note that they're not supported, " +
                            "as they're very out of scope of extending WG capabilities and may harm performance. " +
                            "Consider turning them off by setting 'misc.old-pvp-flags' to 'false'"
            );
            injectables.add(new OldPVPFlagsHandler(this));
        }
        try {
            for (Injectable injectable : injectables) {
                logger().debug("Injecting {}", injectable.getClass().getSimpleName());
                injectable.inject(this);
            }
        } catch (Exception e) {
            logger().error("Unable to inject, shutting down", e);
            getServer().shutdown();
        }

        var pluginManager = getServer().getPluginManager();
        for (PluginIntegration integration : integrations) {
            boolean enable = true;
            for (var pluginName : integration.requiredPlugins()) {
                if (!pluginManager.isPluginEnabled(pluginName)) {
                    enable = false;
                    break;
                }
            }
            if (enable) {
                logger().info("Enabling {} integration", integration.requiredPlugins());
                integration.onEnable(this);
            }
        }
        cfgProvider.reloadSubscribers();

        initMetrics();
    }

    private void listener(@NotNull Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }

    @ApiStatus.Internal
    @Override
    public void onDisable() {
        try {
            for (Injectable injectable : injectables) {
                logger().debug("Uninjecting {}", injectable.getClass().getSimpleName());
                injectable.uninject(this);
            }
        } catch (Exception e) {
            if (getServer().isStopping()) {
                logger().error("Unable to uninject", e);
            } else {
                logger().error("Unable to uninject, shutting down", e);
                getServer().shutdown();
            }
        }
    }

    public @NotNull ComponentLogger logger() {
        return getComponentLogger();
    }

    @Deprecated
    @Override
    public @NotNull Logger getLogger() {
        return super.getLogger();
    }

    @Deprecated
    @Override
    public org.slf4j.@NotNull Logger getSLF4JLogger() {
        return super.getSLF4JLogger();
    }

    @ApiStatus.Obsolete
    @Override
    public @NotNull ComponentLogger getComponentLogger() {
        return super.getComponentLogger();
    }

    private void initMetrics() {
        Metrics metrics = new Metrics(this, 32104);

        metrics.addCustomChart(
                new SimplePie(
                        "old_pvp_flags",
                        () -> cfgProvider.miscCfg().oldPvpFlags() ? "enabled" : "disabled"
                )
        );

        metrics.addCustomChart(
                new SimplePie(
                        "pvp_mode",
                        () -> {
                            var pvpMode = cfgProvider.miscCfg().pvpMode();
                            return pvpMode == null ? "default" : pvpMode.name().toLowerCase(Locale.ROOT);
                        }
                )
        );

        metrics.addCustomChart(
                new DrilldownPie(
                        "message_serializer",
                        () -> {
                            Map<String, Map<String, Integer>> data = new HashMap<>();
                            Map<String, Integer> sub = new HashMap<>();

                            String serializer = cfgProvider.messagesCfg().serializer().toLowerCase(Locale.ROOT);
                            String category;
                            String value = switch (serializer) {
                                case "legacy", "legacy_section", "legacy_ampersand" -> {
                                    category = "legacy";
                                    yield serializer;
                                }
                                case "minimessage", "mini_message" -> {
                                    category = "rich";
                                    yield "minimessage";
                                }
                                default -> {
                                    category = "unknown";
                                    yield serializer;
                                }
                            };

                            sub.put(value, 1);
                            data.put(category, sub);
                            return data;
                        }
                )
        );
    }
}
