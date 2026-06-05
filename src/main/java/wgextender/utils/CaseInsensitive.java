package wgextender.utils;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

/**
 * <a href="https://github.com/vigna/fastutil/issues/376#issuecomment-4296870913">FastUtil Case Insensitive Collections</a>
 * @author imDaniX
 */
public final class CaseInsensitive {
    private CaseInsensitive() { }

    private static final Hash.Strategy<String> CI_STRATEGY = new Hash.Strategy<>() {
        @Override
        public int hashCode(String str) {
            if (str == null) return -1;
            final int length = str.length();
            int result = 0;
            for (int i = 0; i < length; i++) {
                result = 31 * result + Character.toLowerCase(str.charAt(i));
            }
            return result;
        }

        @Override
        public boolean equals(String left, String right) {
            return left == null
                    ? right == null
                    : left.equalsIgnoreCase(right);
        }
    };

    public static @NotNull Hash.Strategy<String> strategy() {
        return CI_STRATEGY;
    }

    public static <V> @NotNull Map<String, V> newMap() {
        return new Object2ObjectOpenCustomHashMap<>(CI_STRATEGY);
    }

    public static @NotNull Set<String> newSet() {
        return new ObjectOpenCustomHashSet<>(CI_STRATEGY);
    }
}