package com.simplelib.mixin.client;

import com.simplelib.api.client.FreeLook;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Free look zoom (DW04A-2): the field-of-view factor of the wheel zoom (only below 1; otherwise Vanilla's value). */
@Mixin(AbstractClientPlayer.class)
public abstract class FreeLookFovMixin {
    @Inject(method = "getFieldOfViewModifier", at = @At("HEAD"), cancellable = true)
    private void simplelib$freeLookFov(boolean firstPerson, float effectScale, CallbackInfoReturnable<Float> cir) {
        float f = FreeLook.zoomFov();
        if (firstPerson && f < 1f) cir.setReturnValue(f);
    }
}
