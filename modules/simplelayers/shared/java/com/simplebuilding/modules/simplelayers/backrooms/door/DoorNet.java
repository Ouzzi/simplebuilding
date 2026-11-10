package com.simplebuilding.modules.simplelayers.backrooms.door;

import com.simplebuilding.modules.simplelayers.backrooms.api.Band;
import com.simplebuilding.modules.simplelayers.backrooms.api.DoorEnd;
import com.simplebuilding.modules.simplelayers.backrooms.stack.BandStack;
import com.simplebuilding.modules.simplelayers.backrooms.stack.Bands;
import com.simplebuilding.modules.simplelayers.world.gen.GenHash;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * The door net of the stack (BACK22 core §5.4): a pure function "has chunk (cx, cz) a door pair between level n and n+1?" (density of
 * the upper level). Both ends lie in the SAME chunk (the upper level's door down, the lower level's door up), so the target is always
 * generated and loaded together with the door - no ticket, no stored link. Link id = hash(n, cx, cz). BACK22c §8.1: only a share
 * ({@link Band#upDoorShare}) of the pairs has the door back up; the others are one-way down. Ends are cached (generation, transit and
 * preloading ask often).
 */
public final class DoorNet {
    public static final int SALT_PAIR = 2100, SALT_UP = 2150, CACHE = 4096;
    private record Key(long seed, int band, boolean down, int cx, int cz) {}
    private static final Map<Key, Optional<DoorEnd>> ENDS = new LinkedHashMap<>(256, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, Optional<DoorEnd>> eldest) {
            return size() > CACHE;
        }
    };

    private DoorNet() {}

    /** Whether chunk (cx, cz) rolls a pair between {@code upper} and {@code upper + 1}. */
    public static boolean pair(long seed, int upper, int cx, int cz) {
        return upper >= 1 && upper < BandStack.BANDS && GenHash.unit(seed, cx, cz, SALT_PAIR + upper) < Bands.get(upper).doorDensity();
    }

    /** The upper level's door down of the pair in (cx, cz), or null (no pair or no place for it). */
    public static @Nullable DoorEnd down(long seed, int upper, int cx, int cz) {
        return pair(seed, upper, cx, cz) ? end(seed, upper, true, cx, cz) : null;
    }

    /** The lower level's door up of the pair in (cx, cz) (needs the door down, and the pair being two-sided), or null. */
    public static @Nullable DoorEnd up(long seed, int lower, int cx, int cz) {
        int upper = lower - 1;
        if (down(seed, upper, cx, cz) == null || !twoSided(seed, upper, cx, cz)) return null;
        return end(seed, lower, false, cx, cz);
    }

    public static boolean twoSided(long seed, int upper, int cx, int cz) {
        return GenHash.unit(seed, cx, cz, SALT_UP + upper) < Bands.get(upper + 1).upDoorShare();
    }

    public static long link(int upper, int cx, int cz) {
        return 0x5B22_0000_0000_0000L ^ ((long) upper << 52) ^ ((long) cx & 0x3FFFFFFL) << 26 ^ ((long) cz & 0x3FFFFFFL);
    }

    private static @Nullable DoorEnd end(long seed, int band, boolean down, int cx, int cz) {
        Key key = new Key(seed, band, down, cx, cz);
        synchronized (ENDS) {
            Optional<DoorEnd> hit = ENDS.get(key);
            if (hit != null) return hit.orElse(null);
        }
        DoorEnd e = Bands.get(band).layout().doorEnd(seed, cx, cz, down);
        synchronized (ENDS) {
            ENDS.put(key, Optional.ofNullable(e));
        }
        return e;
    }

    /** A door end found at a world position: its level, direction (down = to band + 1), the end and the pair's upper level. */
    public record Found(int band, boolean down, DoorEnd end, int upper) {
        /** The level the door leads to (= its symbol). */
        public int target() {
            return down ? band + 1 : band - 1;
        }
    }

    /** The stack door whose leaf stands at world (x, y, z) (lower half), or null. */
    public static @Nullable Found at(long seed, int x, int y, int z) {
        int band = BandStack.bandAt(y);
        if (band == 0) return null;
        int ly = BandStack.localY(y), cx = x >> 4, cz = z >> 4;
        DoorEnd d = band < BandStack.BANDS ? down(seed, band, cx, cz) : null;
        if (d != null && holds(d, x, ly, z)) return new Found(band, true, d, band);
        DoorEnd u = band > 1 ? up(seed, band, cx, cz) : null;
        if (u != null && holds(u, x, ly, z)) return new Found(band, false, u, band - 1);
        return null;
    }

    static boolean holds(DoorEnd e, int x, int ly, int z) {
        if (e.ly() != ly) return false;
        for (int i = 0; i < e.width(); i++) if (e.leafX(i) == x && e.leafZ(i) == z) return true;
        return false;
    }
}
