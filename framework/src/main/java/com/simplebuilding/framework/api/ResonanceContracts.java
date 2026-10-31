package com.simplebuilding.framework.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Simple Resonance contract (WP-22, docs/ai/KONZEPT-A-WP22-2026-10-29.md §2.4), since 0.1.15. Simple Resonance measures how
 * experienced a player is and how dangerous the land is at a place; a mod that wants either (Simple Mobs: effect chances
 * for new monsters) asks {@link #get} and gets {@code null} when Simple Resonance is not installed. Pure Java, no Minecraft
 * class: players are {@link UUID}s, dimensions their id string, places plain doubles. Nobody imports the other side.
 */
public final class ResonanceContracts {
    private static volatile Service service;

    private ResonanceContracts() {
    }

    /** What a monster spawn at a place gets: the chosen owner (none when nobody is near), the zone level and the final level. */
    public record Spawn(Optional<UUID> owner, double zone, double level) {
        public static final Spawn NONE = new Spawn(Optional.empty(), 0.0, 0.0);
    }

    public interface Service {
        /** Whether zones and values are in use (super-mod switch and own switch); false = treat every place as Vanilla. */
        boolean enabled();

        /** The player value W in 0..1 (never exactly 0 for a known player, 0 for an unknown one). */
        double playerValue(UUID player);

        /** The player level P = 5 x W in 0..5. */
        double playerLevel(UUID player);

        /** The share of the old trim resonance F in 0..1 (experience, survival and combat since the last death). */
        double fraction(UUID player);

        /** The old trim multiplier, 0.2..2.0 (configured base times the three factors' mean). */
        double trimMultiplier(UUID player);

        /** The zone level Z in 0..5 at a place (distance to the zone centre, soft). 0 when the dimension is switched off. */
        double zoneLevel(String dimension, double x, double z);

        /** The owner of a spawn attempt: one player near the place, weighted by 1/distance. {@code roll} is uniform in [0,1). */
        Optional<UUID> pickOwner(String dimension, double x, double y, double z, double roll);

        /** The level S (0..5) a monster spawning there gets, with all caps; {@code ownerRoll} and {@code noiseRoll} are uniform in [0,1). */
        Spawn sample(String dimension, double x, double y, double z, double ownerRoll, double noiseRoll);

        /** Gives {@code points} to a player's long-term value (boss kills and the like), rate limited per {@code source}. */
        void award(UUID player, String source, double points);

        /** Whether the player is still in the grace period (first playing hours). */
        boolean isNewcomer(UUID player);
    }

    /** Called by Simple Resonance once; {@code null} removes the service. */
    public static void set(Service impl) {
        service = impl;
    }

    /** The registered service, {@code null} without Simple Resonance. */
    public static Service get() {
        return service;
    }
}
