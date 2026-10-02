package com.simplebuilding.client.render;

import com.simplebuilding.blocks.custom.ChestTier;
import org.jetbrains.annotations.Nullable;

/** Die Stufe eines seltenen Shulkers im Render-State ({@code ShulkerRenderStateMixin}). */
public interface TieredShulkerRenderState {
    @Nullable ChestTier simplebuilding$tier();

    void simplebuilding$setTier(@Nullable ChestTier tier);
}
