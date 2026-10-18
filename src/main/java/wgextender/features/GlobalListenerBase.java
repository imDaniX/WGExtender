package wgextender.features;

import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import wgextender.config.Configurable;
import wgextender.config.ConfigurationProvider;
import wgextender.config.Pointer;

// TODO We can disable the listening itself, but requires some more engineering, might not worth it
public abstract class GlobalListenerBase<T> extends Configurable.GlobalBase<T> implements Listener {
    protected GlobalListenerBase(
            @NotNull ConfigurationProvider cfgProvider,
            @NotNull Pointer.Global<T> pointer
    ) {
        super(cfgProvider, pointer);
    }
}
