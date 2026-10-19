package com.simplebuilding.modules.simpleoptmemory;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * O4 shared block-state tables, own implementation (FerriteCore describes "deduplicate block-state property storage and
 * cached data"; Vanilla 26.3 already keeps properties in arrays and shares the neighbor arrays per pivot). Left over in
 * Vanilla: every state owns its value array, although all blocks with the same properties (all stairs, all slabs ...)
 * hold the same values; every block owns its key array; every state owns an 18-entry face-support table, of which only
 * a few patterns exist. These arrays are only ever read, so states can share one canonical copy:
 * <ul>
 * <li>object arrays are shared when they have the same array class and the same elements by identity ({@code ==}), so a
 * foreign value class with an odd {@code equals} can never be merged with another value;</li>
 * <li>boolean arrays are shared when their contents are equal.</li>
 * </ul>
 */
public final class StateTables {
    private static final Map<Key, Object[]> OBJECTS = new ConcurrentHashMap<>();
    private static final Map<Faces, boolean[]> FACES = new ConcurrentHashMap<>();
    private static final LongAdder SEEN = new LongAdder(), SHARED = new LongAdder(), SAVED = new LongAdder();

    private StateTables() {}

    /** The canonical array with the same class and identical elements; empty arrays are left alone. */
    @SuppressWarnings("unchecked")
    public static <T> T[] share(T[] array) {
        if (array == null || array.length == 0) return array;
        SEEN.increment();
        Object[] canon = OBJECTS.computeIfAbsent(new Key(array), k -> array);
        if (canon == array) return array;
        SHARED.increment();
        SAVED.add(align(16 + 4L * array.length));
        return (T[]) canon;
    }

    public static boolean[] share(boolean[] array) {
        if (array == null || array.length == 0) return array;
        SEEN.increment();
        boolean[] canon = FACES.computeIfAbsent(new Faces(array), k -> array);
        if (canon == array) return array;
        SHARED.increment();
        SAVED.add(align(16 + array.length));
        return canon;
    }

    private static long align(long bytes) {
        return (bytes + 7) & ~7L;
    }

    public static long seen() {
        return SEEN.sum();
    }

    public static long shared() {
        return SHARED.sum();
    }

    /** Estimated bytes no longer held (compressed references, 16-byte array header). */
    public static long savedBytes() {
        return SAVED.sum();
    }

    public static String summary() {
        return String.format(Locale.ROOT, "O4 state tables: %d of %d arrays shared, about %d KiB saved", shared(), seen(), savedBytes() / 1024);
    }

    private record Key(Object[] array) {
        @Override public boolean equals(Object o) {
            if (!(o instanceof Key k) || k.array.getClass() != array.getClass() || k.array.length != array.length) return false;
            for (int i = 0; i < array.length; i++) if (k.array[i] != array[i]) return false;
            return true;
        }

        @Override public int hashCode() {
            int h = array.getClass().hashCode();
            for (Object e : array) h = 31 * h + System.identityHashCode(e);
            return h;
        }
    }

    private record Faces(boolean[] array) {
        @Override public boolean equals(Object o) {
            return o instanceof Faces f && Arrays.equals(f.array, array);
        }

        @Override public int hashCode() {
            return Arrays.hashCode(array);
        }
    }
}
