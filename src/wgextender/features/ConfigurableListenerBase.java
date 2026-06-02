package wgextender.features;

import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import wgextender.config.Config;
import wgextender.config.message.Messages;

public abstract class ConfigurableListenerBase implements Listener {
    protected final Config config;
    protected final Messages msg;

    protected ConfigurableListenerBase(@NotNull Config config) {
        this.config = config;
        this.msg = config.messages();
    }
}
