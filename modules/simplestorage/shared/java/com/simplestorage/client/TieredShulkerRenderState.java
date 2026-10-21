package com.simplestorage.client;

import com.simplestorage.chest.ChestTier;
import org.jetbrains.annotations.Nullable;

/** The tier of a rare shulker in its render state ({@code ShulkerRenderStateMixin}, split stage 8b-3). */
public interface TieredShulkerRenderState {
    @Nullable ChestTier simplestorage$tier();

    void simplestorage$setTier(@Nullable ChestTier tier);
}
