package com.simplebuilding.util;

/** 26.2 twin of the 26.3 bridge to framework {@code WornHeads}: the framework is not on this line, no mod head looks are known. */
public final class HeadLooks {
    private HeadLooks() {
    }

    public static Object skullType(Object stack, Object type) {
        return type;
    }

    public static float skullFactor(Object type) {
        return 1.0F;
    }

    public static float skullEdge(Object type) {
        return 0.0F;
    }
}
