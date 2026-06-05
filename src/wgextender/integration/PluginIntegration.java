package wgextender.integration;

import org.jetbrains.annotations.NotNull;
import wgextender.WGExtender;

import java.util.Collection;

public interface PluginIntegration {
    @NotNull Collection<@NotNull String> requiredPlugins();

    void onEnable(@NotNull WGExtender plugin);
}
