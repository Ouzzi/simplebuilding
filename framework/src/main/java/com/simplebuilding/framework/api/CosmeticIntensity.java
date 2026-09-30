package com.simplebuilding.framework.api;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Optional, process-local cosmetic settings. Never use these values for gameplay. */
public final class CosmeticIntensity {
    public enum Level { OFF, SUBTLE, NORMAL, STRONG, MAXIMUM }

    private static final Map<String, Supplier<Level>> PROVIDERS = new ConcurrentHashMap<>();

    private CosmeticIntensity() {}

    /** A loaded module publishes its current local level, including later config replacements. */
    public static void register(String modId, Supplier<Level> provider) {
        PROVIDERS.put(Objects.requireNonNull(modId), Objects.requireNonNull(provider));
    }

    /** Null means this optional module has not published a level in this process. */
    public static Level current(String modId) {
        var provider = PROVIDERS.get(modId);
        return provider == null ? null : provider.get();
    }
}
