package wgextender.config;

import java.util.function.Function;

public sealed interface Pointer<T> {
    static <T> Scoped<T> scoped(Function<Snapshot.Scope, T> getter) {
        return new Scoped<>(getter);
    }

    static <T> Global<T> global(Function<Snapshot, T> getter) {
        return new Global<>(getter);
    }

    // Can differ between worlds
    final class Scoped<T> implements Pointer<T> {
        private final Function<Snapshot.Scope, T> getter;

        Scoped(Function<Snapshot.Scope, T> getter) {
            this.getter = getter;
        }

        T get(Snapshot.Scope scope) {
            return getter.apply(scope);
        }
    }

    // The same for all the worlds
    final class Global<T> implements Pointer<T> {
        private final Function<Snapshot, T> getter;

        Global(Function<Snapshot, T> getter) {
            this.getter = getter;
        }

        T get(Snapshot snapshot) {
            return getter.apply(snapshot);
        }
    }
}
