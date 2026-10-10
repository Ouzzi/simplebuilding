package com.simplebuilding.framework.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Since 0.1.6. Super-mod / sub-mod switch (docs/ai/KONZEPT-SUPERMOD-SUBMOD-2026-10-06.md): a super-mod sets
 * "Enable Simple XY" per sub-mod id, the sub-mod asks {@link #enabled} at each gameplay hook. Without a super-mod
 * every sub-mod is on, so a sub-mod alone always plays. No mod classes are shared, only the mod id string.
 */
public final class SubMods {
    private static final Map<String, Boolean> SWITCHES = new ConcurrentHashMap<>();

    private SubMods() {}

    public static boolean enabled(String modId) {
        return SWITCHES.getOrDefault(modId, true);
    }

    public static void set(String modId, boolean enabled) {
        SWITCHES.put(modId, enabled);
    }
}
