package com.simplefoundry;

/**
 * Layer melt (the twist, concept T-A): the melts lie in the tank by density, gold at the bottom. A spout in tank level {@code level}
 * (0 = lowest) draws the layer that lies at its height; where two layers meet inside one level the upper one is drawn first.
 */
public final class Layers {
    /** Metal drawn at {@code level} of {@code levels}, or null. {@code units} per {@link Metal} ordinal (bottom to top), {@code capacity} in units. */
    public static Metal at(int[] units, int capacity, int levels, int level) {
        if (levels <= 0 || capacity <= 0 || level < 0 || level >= levels) return null;
        long lo = (long) level * capacity / levels;
        long hi = (long) (level + 1) * capacity / levels;
        long start = 0;
        Metal found = null;
        for (Metal metal : Metal.values()) {
            long end = start + units[metal.ordinal()];
            if (units[metal.ordinal()] > 0 && start < hi && end > lo) found = metal;
            start = end;
        }
        return found;
    }

    /** Metal that lies at {@code unit} (0 = bottom of the tank) when the melts are stacked bottom to top, or null above the melt. */
    public static Metal atUnit(int[] units, long unit) {
        long start = 0;
        for (Metal metal : Metal.values()) {
            long end = start + units[metal.ordinal()];
            if (unit >= start && unit < end) return metal;
            start = end;
        }
        return null;
    }

    /** What window part {@code part} (0 = lowest) of {@code parts} in tank level {@code level} shows: the metal at the middle of that part (Variant A, layers drawn on top of each other). */
    public static Metal windowPart(int[] units, int capacity, int levels, int level, int part, int parts) {
        if (levels <= 0 || capacity <= 0 || level < 0 || level >= levels || part < 0 || part >= parts) return null;
        long lo = (long) level * capacity / levels;
        long hi = (long) (level + 1) * capacity / levels;
        return atUnit(units, lo + (hi - lo) * (2L * part + 1) / (2L * parts));
    }

    private Layers() {}
}
