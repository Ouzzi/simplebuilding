package com.simplebuilding.framework.api;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Since 0.1.4. Optional screen restyling hints: a module that draws a container screen's background in its own style
 * (simplecontainers) publishes which screens it restyles right now, so other mods skip decorations painted for the
 * Vanilla look (SimpleBuilding's astral vault tint) instead of painting over the style. Purely cosmetic.
 *
 * <p><b>Loader- and version-neutral, server-safe:</b> no Minecraft classes; {@code screen} is the open container
 * screen object ({@code AbstractContainerScreen} on 26.3). Predicates run on the render thread and must be cheap.
 */
public final class ContainerStyleHints {
    private static final Map<String, Predicate<Object>> PROVIDERS = new ConcurrentHashMap<>();

    private ContainerStyleHints() {}

    /** A loaded module tells which screens it currently draws in its own style. */
    public static void register(String modId, Predicate<Object> restyled) {
        PROVIDERS.put(Objects.requireNonNull(modId), Objects.requireNonNull(restyled));
    }

    /** True if any loaded module draws {@code screen}'s background in its own style right now. */
    public static boolean isRestyled(Object screen) {
        for (Predicate<Object> p : PROVIDERS.values()) {
            if (p.test(screen)) return true;
        }
        return false;
    }
}
