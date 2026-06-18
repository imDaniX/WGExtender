package wgextender.features;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import wgextender.WGExtender;
import wgextender.config.ConfigurationProvider;
import wgextender.config.message.MKey;
import wgextender.utils.ModrinthUpdater;

import java.util.concurrent.TimeUnit;

public final class VersionHandler extends ConfigurableListenerBase<ConfigurationProvider.Updater> {
    private final WGExtender plugin;
    private final ModrinthUpdater updater;

    private ScheduledTask checkTask;

    public VersionHandler(@NotNull WGExtender plugin) {
        super(plugin.getConfigurationProvider(), ConfigurationProvider::updater);
        this.plugin = plugin;
        this.updater = plugin.getUpdater();
    }

    @Override
    protected void subReload(ConfigurationProvider.Updater oldConfig) {
        if (checkTask != null && !checkTask.isCancelled()) {
            checkTask.cancel();
        }

        if (!config.enabled()) {
            return;
        }

        if (config.checkInterval() < 60) {
            plugin.getSLF4JLogger().error(
                    "<ModrinthUpdater> Check interval is less than a minute ({} seconds). Not enabling the version check task.",
                    config.checkInterval()
            );
            return;
        }

        checkTask = plugin.getServer().getAsyncScheduler().runAtFixedRate(
                plugin,
                task -> checkForUpdate(),
                10,
                config.checkInterval(),
                TimeUnit.SECONDS
        );
    }

    private void checkForUpdate() {
        ComponentLogger logger = plugin.getComponentLogger();
        ModrinthUpdater.CheckResult result = updater.checkForUpdate(config.allowStaging());
        switch (result) {
            case ModrinthUpdater.CheckResult.Available available -> {
                logger.info(
                        msg.rich(
                                MKey.WGEX_COMMAND__UPDATE__AVAILABLE,
                                available.currentRaw(), available.latestRaw(), available.latestFile().versionType()
                        )
                );
                if (config.intervalNotify()) {
                    plugin.getServer().broadcast(
                            msg.rich(
                                    MKey.WGEX_COMMAND__UPDATE__AVAILABLE,
                                    available.currentRaw(),
                                    available.latestRaw(),
                                    available.latestFile().versionType()
                            ),
                            "wgextender.admin"
                    );
                }
            }
            case ModrinthUpdater.CheckResult.Failure failure ->
                    logger.error(
                            msg.rich(MKey.WGEX_COMMAND__UPDATE__FAILURE, failure.cause().getMessage()),
                            failure.cause()
                    );
            case ModrinthUpdater.CheckResult.Unknown ignored ->
                    logger.warn(msg.rich(MKey.WGEX_COMMAND__UPDATE__UNKNOWN));
            default -> {}
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!config.enabled() || !config.joinNotify() || !event.getPlayer().hasPermission("wgextender.admin")) {
            return;
        }

        updater.lastResult().ifPresent(result -> {
            if (result instanceof ModrinthUpdater.CheckResult.Available available) {
                msg.sendMessage(
                        event.getPlayer(),
                        MKey.WGEX_COMMAND__UPDATE__AVAILABLE,
                        available.currentRaw(),
                        available.latestRaw()
                );
            }
        });
    }
}