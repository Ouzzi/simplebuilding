package com.simplestorage.client;

/** Set on Vanilla's bundle tooltip by {@code ClientBundleTooltipMixin} (split stage 8c-1): capacity scale, bar text colour, slot tint. */
public interface BundleTooltipTint {
    void simplestorage$setCapacityScale(float scale);

    void simplestorage$setProgressColor(int argb);

    /** Colour (ARGB) every slot background is multiplied with; -1 = Vanilla. */
    void simplestorage$setSlotTint(int argb);

    int simplestorage$getSlotTint();
}
