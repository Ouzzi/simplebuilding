package com.simplebuilding.util;

import com.simplebuilding.framework.api.WornHeads;

/**
 * Bridge to framework {@link WornHeads} (26.3: the framework is on the classpath; the 26.2 twin answers "nothing known" until
 * the port run): the skull look a mod gives its heads in the extra head slot - variant, size factor, worn edge.
 */
public final class HeadLooks {
    private HeadLooks() {
    }

    /** The skull type {@code stack} shows ({@code type} = the block's own type). */
    public static Object skullType(Object stack, Object type) {
        return WornHeads.skullType(stack, type);
    }

    /** Size factor of a worn skull of {@code type} against Vanilla's worn skull (1.0 = as Vanilla). */
    public static float skullFactor(Object type) {
        return WornHeads.skullFactor(type);
    }

    /** Longest edge in model pixels of the worn skull of {@code type}; 0 when no mod knows it. */
    public static float skullEdge(Object type) {
        return WornHeads.skullEdge(type);
    }
}
