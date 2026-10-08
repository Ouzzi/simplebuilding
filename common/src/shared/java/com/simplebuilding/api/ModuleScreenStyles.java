package com.simplebuilding.api;

import java.util.ServiceLoader;

/**
 * Screen restyling hints other mods publish through the framework
 * ({@code com.simplebuilding.framework.api.ContainerStyleHints}, e.g. simplecontainers): while a container screen is
 * drawn in such a style, SimpleBuilding leaves out decorations made for the Vanilla look (astral vault tint). The 26.3
 * overlay supplies the bridge (like {@link ModuleTransformHints}); lines without the framework never restyle.
 */
public final class ModuleScreenStyles {
    /** Implemented by the 26.3 framework adapter. */
    public interface Bridge {
        boolean isRestyled(Object screen);
    }

    private static final Bridge BRIDGE = ServiceLoader.load(Bridge.class, ModuleScreenStyles.class.getClassLoader()).findFirst().orElse(null);

    private ModuleScreenStyles() {
    }

    /** Whether another mod draws {@code screen}'s background in its own style right now. */
    public static boolean isRestyled(Object screen) {
        return BRIDGE != null && BRIDGE.isRestyled(screen);
    }
}
