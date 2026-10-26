package com.simplebuilding.modules.simpleearth.view;

/** Public access to the folding world: phase values (client test: the measured curve) and the ease curve (E30-3). */
public final class FoldWorldViews {
    private FoldWorldViews() {}

    public static double ease(double t, double power) {
        return FoldWorldView.ease(t, power);
    }

    /** Blend and fold of the last frame, or -1/-1 when the view is no folding world. */
    public static double[] phases(EarthView view) {
        return view instanceof FoldWorldView f ? f.phases() : new double[]{-1, -1};
    }
}
