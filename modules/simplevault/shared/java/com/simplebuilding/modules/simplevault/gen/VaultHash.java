package com.simplebuilding.modules.simplevault.gen;

/** Stable 64-bit hashing for the Bedrock Vault layout (seed, region, salt); same input, same vault, on every machine. */
public final class VaultHash {
    private VaultHash() {}

    public static long mix(long seed, int rx, int rz, long salt) {
        long h = seed ^ 0x9E3779B97F4A7C15L;
        h = step(h + rx * 0xC2B2AE3D27D4EB4FL);
        h = step(h + rz * 0x165667B19E3779F9L);
        return step(h + salt * 0x27D4EB2F165667C5L);
    }

    private static long step(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
