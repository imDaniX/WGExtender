package wgextender.features;

import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import wgextender.config.Configurable;
import wgextender.config.ConfigurationProvider;
import wgextender.config.Pointer;

public abstract class ScopedListenerBase<T> extends Configurable.ScopedBase<T> implements Listener {
    protected ScopedListenerBase(
            @NotNull ConfigurationProvider cfgProvider,
            @NotNull Pointer.Scoped<T> pointer
    ) {
        super(cfgProvider, pointer);
    }
}
