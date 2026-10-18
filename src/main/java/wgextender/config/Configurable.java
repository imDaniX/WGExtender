package wgextender.config;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import wgextender.config.message.MessagesProvider;

@FunctionalInterface
public interface Configurable<T> {
    void onReload(@NotNull T section);

    abstract class Base<T> implements Configurable<T> {
        protected final ConfigurationProvider cfgProvider;
        protected final MessagesProvider msg;

        protected Base(
                @NotNull ConfigurationProvider cfgProvider,
                @NotNull Pointer<T> pointer
        ) {
            this.cfgProvider = cfgProvider;
            this.msg = cfgProvider.messageProvider();
            cfgProvider.register(this, pointer);
        }
    }

    abstract class GlobalBase<T> extends Base<T> {
        protected T config;

        protected GlobalBase(
                @NotNull ConfigurationProvider cfgProvider,
                @NotNull Pointer.Global<T> pointer
        ) {
            super(cfgProvider, pointer);
            this.config = cfgProvider.section(pointer);
        }

        @Override
        public final void onReload(@NonNull T section) {
            T oldConfig = config;
            this.config = section;
            subReload(oldConfig);
        }

        protected void subReload(@Nullable T oldConfig) {
            // No-op by default
        }
    }

    abstract class ScopedBase<T> extends Base<T> {
        private final Pointer.Scoped<T> pointer;

        protected ScopedBase(
                @NotNull ConfigurationProvider cfgProvider,
                @NotNull Pointer.Scoped<T> pointer
        ) {
            super(cfgProvider, pointer);
            this.pointer = pointer;
        }

        protected final @NotNull T config(@Nullable String world) {
            return cfgProvider.section(pointer, world);
        }

        protected final @NotNull T config(@NotNull World world) {
            return config(world.getName());
        }

        @Override
        public final void onReload(@NonNull T section) {
            subReload();
        }

        protected void subReload() {
            // No-op by default
        }
    }
}
