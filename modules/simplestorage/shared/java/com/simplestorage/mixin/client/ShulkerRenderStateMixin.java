package com.simplestorage.mixin.client;

import com.simplestorage.chest.ChestTier;
import com.simplestorage.client.TieredShulkerRenderState;
import net.minecraft.client.renderer.entity.state.ShulkerRenderState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Carries the tier of a rare shulker from extracting to drawing ({@link ShulkerRendererMixin}). */
@Mixin(ShulkerRenderState.class)
public abstract class ShulkerRenderStateMixin implements TieredShulkerRenderState {
    @Unique
    private @Nullable ChestTier simplestorage$tier;

    @Override
    public @Nullable ChestTier simplestorage$tier() {
        return this.simplestorage$tier;
    }

    @Override
    public void simplestorage$setTier(@Nullable ChestTier tier) {
        this.simplestorage$tier = tier;
    }
}
