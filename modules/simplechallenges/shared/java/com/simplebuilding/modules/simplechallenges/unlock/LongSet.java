package com.simplebuilding.modules.simplechallenges.unlock;

import java.util.Arrays;

/** Minimal open-addressing set of longs (insert and lookup only): the unlocked cells, one entry of 8 bytes each (plus load slack). */
public final class LongSet {
    private static final long EMPTY = Long.MIN_VALUE;
    private long[] table = new long[16];
    private int size;
    private boolean hasEmptyKey;

    public LongSet() {
        Arrays.fill(table, EMPTY);
    }

    public int size() {
        return size;
    }

    private static int mix(long k) {
        long h = k * 0x9E3779B97F4A7C15L;
        return (int) (h ^ (h >>> 32));
    }

    public boolean contains(long key) {
        if (key == EMPTY) return hasEmptyKey;
        int mask = table.length - 1;
        for (int i = mix(key) & mask; ; i = (i + 1) & mask) {
            long t = table[i];
            if (t == key) return true;
            if (t == EMPTY) return false;
        }
    }

    /** @return true when the key was new. */
    public boolean add(long key) {
        if (key == EMPTY) {
            if (hasEmptyKey) return false;
            hasEmptyKey = true;
            size++;
            return true;
        }
        if ((size + 1) * 2 > table.length) grow();
        int mask = table.length - 1;
        for (int i = mix(key) & mask; ; i = (i + 1) & mask) {
            long t = table[i];
            if (t == key) return false;
            if (t == EMPTY) {
                table[i] = key;
                size++;
                return true;
            }
        }
    }

    private void grow() {
        long[] old = table;
        table = new long[old.length * 2];
        Arrays.fill(table, EMPTY);
        int mask = table.length - 1;
        for (long k : old) {
            if (k == EMPTY) continue;
            int i = mix(k) & mask;
            while (table[i] != EMPTY) i = (i + 1) & mask;
            table[i] = k;
        }
    }

    /** All keys, sorted (stable order for saving and tests). */
    public long[] toSortedArray() {
        long[] out = new long[size];
        int n = 0;
        if (hasEmptyKey) out[n++] = EMPTY;
        for (long k : table) if (k != EMPTY) out[n++] = k;
        Arrays.sort(out);
        return out;
    }
}
