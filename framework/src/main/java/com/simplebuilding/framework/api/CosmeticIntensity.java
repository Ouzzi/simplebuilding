package com.simplebuilding.framework.api;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/** Optional, process-local cosmetic settings. Never use these values for gameplay. */
public final class CosmeticIntensity {
    public enum Level { OFF, SUBTLE, NORMAL, STRONG, MAXIMUM }

    private static final Map<String, Supplier<Level>> PROVIDERS = new ConcurrentHashMap<>();
    private static final Map<String, Function<String, Level>> EFFECT_PROVIDERS = new ConcurrentHashMap<>();

    private CosmeticIntensity() {}

    /** A loaded module publishes its current local level, including later config replacements. */
    public static void register(String modId, Supplier<Level> provider) {
        PROVIDERS.put(Objects.requireNonNull(modId), Objects.requireNonNull(provider));
    }

    /**
     * Since 0.1.2: a loaded module also publishes its per-effect levels (its own effect overrides).
     * The function returns null for an effect id without an override; {@link #current(String, String)}
     * then falls back to the module's global level.
     */
    public static void registerEffects(String modId, Function<String, Level> provider) {
        EFFECT_PROVIDERS.put(Objects.requireNonNull(modId), Objects.requireNonNull(provider));
    }

    /** Null means this optional module has not published a level in this process. */
    public static Level current(String modId) {
        var provider = PROVIDERS.get(modId);
        return provider == null ? null : provider.get();
    }

    /**
     * Since 0.1.2: the level the module uses for one effect id - its override when it has one,
     * otherwise its global level. Null means the module has published no level at all.
     */
    public static Level current(String modId, String effectId) {
        var effects = EFFECT_PROVIDERS.get(modId);
        Level level = effects == null || effectId == null ? null : effects.apply(effectId);
        return level != null ? level : current(modId);
    }
}
