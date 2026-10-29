package com.simplebuilding.modules.simpleinterfaces.mixin;

import com.simplebuilding.modules.simpleinterfaces.client.ZoomState;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Spyglass and zoom key (BS29-3): the field-of-view factor comes from the zoom state while this mod zooms. */
@Mixin(AbstractClientPlayer.class)
public abstract class ZoomFovMixin {
    @Inject(method = "getFieldOfViewModifier", at = @At("HEAD"), cancellable = true)
    private void sb$zoomFov(boolean firstPerson, float effectScale, CallbackInfoReturnable<Float> cir) {
        if (firstPerson && ZoomState.overridesFov((AbstractClientPlayer) (Object) this)) cir.setReturnValue(ZoomState.fov());
    }
}
