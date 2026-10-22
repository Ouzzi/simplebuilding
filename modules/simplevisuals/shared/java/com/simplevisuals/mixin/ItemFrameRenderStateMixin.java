package com.simplevisuals.mixin;

import com.simplevisuals.client.TiltedFrameState;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemFrameRenderState.class)
public abstract class ItemFrameRenderStateMixin implements TiltedFrameState {
    @Unique private boolean simplevisuals$tilted;
    @Unique private float simplevisuals$push;

    @Override public boolean simplevisuals$tilted() { return simplevisuals$tilted; }

    @Override public float simplevisuals$push() { return simplevisuals$push; }

    @Override public void simplevisuals$setTilt(boolean tilted, float push) {
        simplevisuals$tilted = tilted;
        simplevisuals$push = push;
    }
}
