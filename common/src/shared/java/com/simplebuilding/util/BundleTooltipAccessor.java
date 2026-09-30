package com.simplebuilding.util;

public interface BundleTooltipAccessor {
    void simplebuilding$setCapacityScale(float scale);

    default void simplebuilding$setProgressColor(int argb) {}

    /** Farbe (ARGB), mit der der Hintergrund jedes Felds multipliziert wird; -1 = Vanilla. */
    void simplebuilding$setSlotTint(int argb);

    int simplebuilding$getSlotTint();
}
