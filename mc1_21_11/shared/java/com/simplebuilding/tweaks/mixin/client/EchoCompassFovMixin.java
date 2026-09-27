package com.simplebuilding.tweaks.mixin.client;

import com.simplebuilding.tweaks.client.EchoCompassFov;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * FOV-Sog beim Aufladen des Echo-Kompasses (Fabric und NeoForge; Forge nutzt ComputeFovModifierEvent).
 * Am Ende von getFieldOfViewModifier multipliziert - auf NeoForge also nach dessen eigenem Ereignis.
 */
@Mixin(AbstractClientPlayer.class)
public abstract class EchoCompassFovMixin {
    @Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
    private void simplebuilding$echoCompassFov(boolean firstPerson, float effectScale, CallbackInfoReturnable<Float> cir) {
        float factor = EchoCompassFov.factor((AbstractClientPlayer) (Object) this, effectScale);
        if (factor != 1.0f) {
            cir.setReturnValue(cir.getReturnValueF() * factor);
        }
    }
}
