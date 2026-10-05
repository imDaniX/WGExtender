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

import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wgextender.WGExtender;
import wgextender.config.message.MessagesProvider;
import wgextender.config.section.Features;
import wgextender.config.section.Messages;
import wgextender.config.section.Updater;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class ConfigurationProvider {
    private static final String WORLDS_FOLDER = "worlds";
    private static final String WORLDS_EXAMPLE = "example.yml.disabled";

    private final WGExtender plugin;
    private final File configFile;
    private final WorldLoader worldLoader;
    private final MessagesProvider msgProvider;
    private final List<Consumer<ConfigurationProvider>> subscribers = new ArrayList<>();

    private volatile Snapshot current;

    public ConfigurationProvider(WGExtender plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.worldLoader = new WorldLoader(new File(plugin.getDataFolder(), WORLDS_FOLDER), plugin.logger());
        this.msgProvider = new MessagesProvider(plugin, this);
    }

    public void reload() {
        plugin.saveDefaultConfig();
        if (!new File(plugin.getDataFolder(), WORLDS_FOLDER).exists()) {
            plugin.saveResource(WORLDS_FOLDER + "/" + WORLDS_EXAMPLE, false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        // The same flag is likely to be seen in the general and the world configs, so report it once
        Set<String> unknownFlags = new HashSet<>();
        Consumer<String> onUnknownFlag = flag -> {
            if (unknownFlags.add(flag)) {
                plugin.logger().warn("Unknown flag provided for autoflags: {}", flag);
            }
        };
        this.current = new Snapshot(
                Snapshot.Scope.load(config, onUnknownFlag),
                worldLoader.load(config, onUnknownFlag),
                Messages.load(config),
                Updater.load(config),
                Features.load(config)
        );
    }

    public void reloadSubscribers() {
        subscribers.forEach(sub -> sub.accept(this));
    }

    public <T> void register(@NotNull Configurable<T> reloadable, @NotNull Pointer<T> pointer) {
        subscribers.add(provider -> reloadable.onReload(provider.section(pointer)));
    }

    public @NotNull MessagesProvider messageProvider() {
        return msgProvider;
    }

    public <T> @NotNull T section(@NotNull Pointer<T> pointer) {
        return switch (pointer) {
            case Pointer.Scoped<T> scoped -> scoped.get(current.general());
            case Pointer.Global<T> global -> global.get(current);
        };
    }

    public <T> @NotNull T section(@NotNull Pointer.Scoped<T> pointer, @Nullable String world) {
        return pointer.get(current.scopeOf(world));
    }

    public <T> @NotNull T section(@NotNull Pointer.Scoped<T> pointer, @NotNull World world) {
        return section(pointer, world.getName());
    }
}
