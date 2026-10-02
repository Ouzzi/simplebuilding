package com.simplebuilding.mixin.client;

import com.simplebuilding.blocks.custom.ChestTier;
import com.simplebuilding.client.render.TieredShulkerRenderState;
import net.minecraft.client.renderer.entity.state.ShulkerRenderState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Traegt die Stufe eines seltenen Shulkers vom Extrahieren zum Zeichnen ({@link ShulkerRendererMixin}). */
@Mixin(ShulkerRenderState.class)
public abstract class ShulkerRenderStateMixin implements TieredShulkerRenderState {
    @Unique
    private @Nullable ChestTier simplebuilding$tier;

    @Override
    public @Nullable ChestTier simplebuilding$tier() {
        return this.simplebuilding$tier;
    }

    @Override
    public void simplebuilding$setTier(@Nullable ChestTier tier) {
        this.simplebuilding$tier = tier;
    }
}
