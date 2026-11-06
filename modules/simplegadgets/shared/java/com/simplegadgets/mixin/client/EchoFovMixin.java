package com.simplegadgets.mixin.client;

import com.simplegadgets.client.EchoFov;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * FOV-Sog beim Aufladen des Echolots (Simple Gadgets, G4): am Ende von getFieldOfViewModifier multipliziert.
 */
@Mixin(AbstractClientPlayer.class)
public abstract class EchoFovMixin {
    @Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
    private void simplegadgets$echoFov(boolean firstPerson, float effectScale, CallbackInfoReturnable<Float> cir) {
        float factor = EchoFov.factor((AbstractClientPlayer) (Object) this, effectScale);
        if (factor != 1.0f) {
            cir.setReturnValue(cir.getReturnValueF() * factor);
        }
    }
}
