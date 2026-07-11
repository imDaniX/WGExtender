package wgextender.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import wgextender.config.message.MessagesProvider;

import java.util.function.Function;

@FunctionalInterface
public interface Configurable<T> {
    void onReload(@NotNull T section);

    abstract class Base<T> implements Configurable<T> {
        protected final ConfigurationProvider configProvider;
        protected final MessagesProvider msg;
        protected T config;

        public Base(
                @NotNull ConfigurationProvider configProvider,
                @NotNull Function<ConfigurationProvider, T> sectionGetter
        ) {
            this.configProvider = configProvider;
            this.msg = configProvider.messageProvider();
            this.config = sectionGetter.apply(configProvider);
            configProvider.register(this, sectionGetter);
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
}

