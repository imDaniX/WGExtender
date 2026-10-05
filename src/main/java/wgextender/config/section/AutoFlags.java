package wgextender.config.section;

import com.sk89q.worldguard.protection.flags.Flag;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import wgextender.config.Pointer;
import wgextender.config.Snapshot;
import wgextender.utils.WGUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static wgextender.config.section.Sections.at;

public record AutoFlags(boolean enabled, boolean showMessages, @NotNull Map<Flag<?>, String> flags) {
    public static final Pointer.Scoped<AutoFlags> POINTER = Pointer.scoped(Snapshot.Scope::autoFlags);

    public static @NotNull AutoFlags load(@NotNull ConfigurationSection config, @NotNull Consumer<String> onUnknownFlag) {
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
                            onUnknownFlag.accept(key);
                        }
                    }
                    return Map.copyOf(flags);
                })
        ));
    }
}
