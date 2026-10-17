package com.simplebuilding.modules.simpletweaks.protect;

/**
 * Seed protection: login and respawn packets carry a random hashed seed (new every server start) instead of
 * {@code BiomeManager.obfuscateSeed(seed)}. The client uses it only for the biome noise at borders, so colors at biome
 * borders blend slightly differently; seed crackers lose this source.
 */
public final class SeedHash {
    private static volatile long fake = new java.security.SecureRandom().nextLong();

    static void reset() { fake = new java.security.SecureRandom().nextLong(); }
    /** What the client gets instead of {@code vanilla}. */
    public static long sent(long vanilla) {
        return Protect.on() && Protect.config().seedHash().enabled() ? fake : vanilla;
    }
    private SeedHash() {}
}
