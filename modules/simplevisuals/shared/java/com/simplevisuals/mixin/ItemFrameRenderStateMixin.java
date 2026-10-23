package com.simplevisuals.mixin;

import com.simplevisuals.client.TiltedFrameState;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemFrameRenderState.class)
public abstract class ItemFrameRenderStateMixin implements TiltedFrameState {
    @Unique private boolean simplevisuals$tilted;
    @Unique private boolean simplevisuals$pane;
    @Unique private float simplevisuals$degrees;
    @Unique private final BlockModelRenderState simplevisuals$model = new BlockModelRenderState();

    @Override public boolean simplevisuals$tilted() { return simplevisuals$tilted; }

    @Override public boolean simplevisuals$pane() { return simplevisuals$pane; }

    @Override public float simplevisuals$degrees() { return simplevisuals$degrees; }

    @Override public BlockModelRenderState simplevisuals$model() { return simplevisuals$model; }

    @Override public void simplevisuals$setTilt(boolean tilted, boolean pane, float degrees) {
        simplevisuals$tilted = tilted;
        simplevisuals$pane = pane;
        simplevisuals$degrees = degrees;
        if (!tilted) simplevisuals$model.clear();
    }
}
