package wgextender.utils;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

public enum Comparison {
    // Having this order to preserve enum's default compareTo
    BELOW(-1),
    EQUAL(0),
    ABOVE(1);

    private final int magic;

    Comparison(int compare) {
        this.magic = compare;
    }

    public int magic() {
        return magic;
    }

    public static @NotNull Comparison ofMagic(int magic) {
        if (magic < 0) {
            return BELOW;
        } else if (magic > 0) {
            return ABOVE;
        } else {
            return EQUAL;
        }
    }

    public static <T> @NotNull Comparison of(@NotNull Comparable<T> tested, @NotNull T against) {
        return ofMagic(tested.compareTo(against));
    }

    public static <T> @NotNull Comparison of(@NotNull Comparator<T> comparator, @NotNull T tested, @NotNull T against) {
        return ofMagic(comparator.compare(tested, against));
    }

    public static <T> boolean is(@NotNull Comparable<T> tested, @NotNull Comparison expected, @NotNull T against) {
        return of(tested, against) == expected;
    }

    public static <T> boolean is(@NotNull Comparator<T> comparator, @NotNull T tested, @NotNull Comparison expected, @NotNull T against) {
        return of(comparator, tested, against) == expected;
    }
}
