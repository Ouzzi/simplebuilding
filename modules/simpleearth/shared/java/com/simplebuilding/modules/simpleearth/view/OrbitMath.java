package com.simplebuilding.modules.simpleearth.view;

/** Public face of the orbit maths for tests and the renderer (E29-2). */
public final class OrbitMath {
    private OrbitMath() {}

    /** Equiangular cube: face coordinate of a linear position -1..1 on a tile (tan of a quarter-turn share). */
    public static double angular(double s) {
        return OrbitBodyView.angular(s);
    }
}
