package com.simpletrims.mixin;

import com.simpletrims.TrimGlow;
import com.simpletrims.light.TrimLight;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Item frames have no tick of their own: radiance of a framed piece from {@code BlockAttachedEntity#tick}. */
@Mixin(BlockAttachedEntity.class)
public abstract class BlockAttachedEntityTrimsMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void simpletrims$radiance(CallbackInfo ci) {
        if ((Object) this instanceof ItemFrame frame) {
            if (frame.level().isClientSide()) TrimLight.tickGlowMotes(frame, () -> TrimGlow.getEmissionLevel(frame.getItem()));
            else TrimLight.tickItemFrame(frame);
        }
    }
}
