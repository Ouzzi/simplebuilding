package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.client.AimSway;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shivering makes the aim waver: once per frame, after the mouse turned the player. */
@Mixin(MouseHandler.class)
public abstract class AimSwayMixin {
    @Inject(method = "handleAccumulatedMovement", at = @At("TAIL"))
    private void simplemobs$sway(CallbackInfo ci) {
        AimSway.apply();
    }
}
