package com.simpleshops.logic;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Which shop music a listener hears (a-shopmusic37, plan docs/ai/PLAN-A-SHOPMUSIC37-2026-11-12.md). Plain Java without game
 * classes, the client ({@code ShopMusicClient}) feeds it the shops with a disc around the player:
 * <ul>
 * <li><b>Group:</b> shops with the same song and the same start tick (the server gives a shop the start of a shop with the same
 * disc nearby, {@link ShopMusic#startFor}) play one piece together - one sound at the group member nearest to the listener,
 * like SimpleBuilding's Jukebox Amplifier (Astralit) chain.</li>
 * <li><b>Boost:</b> every further member widens the group's reach by {@link #BOOST_PER_SHOP} of the base reach, at most
 * {@link #MAX_BOOST_SHOPS} members count - the Note Amplifier's (Nihilit) rule, at most {@link #MAX_FACTOR} times.</li>
 * <li><b>Nearest wins:</b> among the groups the listener is in reach of, the one with the nearest member plays; the playing
 * group keeps the music until another is {@link #HYSTERESIS} blocks nearer (no flapping on the border).</li>
 * </ul>
 */
public final class ShopMusicPick {
    /** Extra reach per further shop of a group (50 %, like the Note Amplifier). */
    public static final double BOOST_PER_SHOP = 0.5;
    /** At most this many further shops count. */
    public static final int MAX_BOOST_SHOPS = 3;
    /** The largest reach factor: 1 + 3 x 50 %. */
    public static final double MAX_FACTOR = 1.0 + MAX_BOOST_SHOPS * BOOST_PER_SHOP;
    /** Another group must be this many blocks nearer than the playing one to take over. */
    public static final double HYSTERESIS = 2.0;

    /** A shop with a disc: position (block centre), song id, start tick, song length, base reach in blocks. */
    public record Shop(double x, double y, double z, String song, long start, float seconds, int range) {
        double distance(double lx, double ly, double lz) {
            double dx = x - lx, dy = y - ly, dz = z - lz;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        /** Same piece at the same moment: one group. */
        public boolean sameGroup(Shop other) {
            return other != null && song.equals(other.song) && start == other.start;
        }
    }

    /** The music to play: the group, its member nearest to the listener ({@code at}), the listener's distance to it and the group's reach. */
    public record Pick(Shop at, List<Shop> group, double distance, double reach) {
        /** The volume at the listener: full at the shop, falling linearly to 0 at the reach. */
        public float gain() {
            return ShopMusicPick.gain(distance, reach);
        }
    }

    /** Reach factor of a group of {@code size} shops. */
    public static double factor(int size) {
        return 1.0 + Math.min(Math.max(size - 1, 0), MAX_BOOST_SHOPS) * BOOST_PER_SHOP;
    }

    /** Linear falloff: 1 at the shop, 0 at {@code reach} and beyond. */
    public static float gain(double distance, double reach) {
        if (reach <= 0 || distance >= reach) return 0.0F;
        return (float) Math.min(1.0, Math.max(0.0, 1.0 - distance / reach));
    }

    /** Seconds into the loop at {@code gameTime} for a piece started at {@code start} that is {@code seconds} long. */
    public static int offsetSeconds(long gameTime, long start, float seconds) {
        long length = Math.round(seconds * 20.0);
        if (length <= 0) return 0;
        return (int) (Math.floorMod(gameTime - start, length) / 20L);
    }

    /**
     * The music the listener at {@code (lx, ly, lz)} hears, or {@code null} (no group in reach). {@code playing} is any shop of
     * the group that plays now (or null), for the hysteresis.
     */
    public static @Nullable Pick pick(List<Shop> shops, double lx, double ly, double lz, @Nullable Shop playing) {
        List<List<Shop>> groups = new ArrayList<>();
        for (Shop shop : shops) {
            List<Shop> home = null;
            for (List<Shop> g : groups) if (g.get(0).sameGroup(shop)) home = g;
            if (home == null) groups.add(home = new ArrayList<>());
            home.add(shop);
        }
        Pick best = null, current = null;
        for (List<Shop> group : groups) {
            Shop near = null;
            double distance = Double.MAX_VALUE;
            for (Shop shop : group) {
                double d = shop.distance(lx, ly, lz);
                if (d < distance) {
                    distance = d;
                    near = shop;
                }
            }
            double reach = group.get(0).range() * factor(group.size());
            if (distance >= reach) continue;
            Pick pick = new Pick(near, List.copyOf(group), distance, reach);
            if (best == null || distance < best.distance()) best = pick;
            if (playing != null && group.get(0).sameGroup(playing)) current = pick;
        }
        if (current != null && best != null && current != best && current.distance() <= best.distance() + HYSTERESIS) return current;
        return best;
    }

    private ShopMusicPick() {}
}
