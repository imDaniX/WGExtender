package wgextender.utils;

import org.jetbrains.annotations.NotNull;
import wgextender.WGExtender;

// TODO Might need a better naming
public interface Injectable {
    void inject(@NotNull WGExtender plugin) throws Exception;

    void uninject(@NotNull WGExtender plugin) throws Exception;
}
