package com.simplebuilding.util;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Read access to the two {@code ItemEntity} fields vanilla keeps private and that decide who may
 * pick an item up; implemented by {@code com.simplebuilding.mixin.ItemEntityMixin}.
 */
public interface ItemEntityPickupInfo {

    /** Vanilla's {@code pickupDelay}; {@value AttractorFilter#NEVER_PICK_UP} means "never" ({@code setNeverPickUp}). */
    int simplebuilding$pickupDelay();

    /** Vanilla's {@code target} (saved as {@code Owner}): only this player may pick the item up, or null for anyone. */
    @Nullable UUID simplebuilding$pickupOwner();
}
