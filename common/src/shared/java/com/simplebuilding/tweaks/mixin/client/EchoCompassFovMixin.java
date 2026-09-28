package com.simplebuilding.tweaks.mixin.client;

import com.simplebuilding.tweaks.client.EchoCompassFov;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * FOV-Sog beim Aufladen des Echo-Kompasses - einziger Pfad auf allen Loadern (Fabric, NeoForge und
 * Forge; Forge laedt simplebuilding.tweaks.mixins.json per Manifest/-mixin.config). Am Ende von
 * getFieldOfViewModifier multipliziert - auf NeoForge/Forge also nach deren FOV-Ereignis. Kein
 * zusaetzlicher ComputeFovModifierEvent-Hoerer auf Forge, sonst wirkt der Faktor doppelt.
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
