package wgextender.features;

import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import wgextender.config.Configurable;
import wgextender.config.ConfigurationProvider;

import java.util.function.Function;

// TODO We can disable the listening itself, but requires some more engineering, might not worth it
public abstract class ConfigurableListenerBase<T> extends Configurable.Base<T> implements Listener {
    protected ConfigurableListenerBase(
            @NotNull ConfigurationProvider cfgProvider,
            @NotNull Function<ConfigurationProvider, T> sectionGetter
    ) {
        super(cfgProvider, sectionGetter);
    }
}
