package com.simplebuilding.mixin.client;

import com.mojang.blaze3d.audio.Channel;
import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Verstärker (2026-10-06): OpenAL begrenzt die Lautstaerke einer Quelle standardmaessig auf {@code AL_MAX_GAIN} = 1.
 * Nur wenn eine Lautstaerke ueber 1 ankommt (das tut nur ein verstaerkter Klang, {@code SoundEngineAmplifierMixin}),
 * wird die Grenze dieser Quelle auf genau diesen Wert angehoben; Vanilla-Klaenge setzen nie mehr als 1.
 */
@Mixin(Channel.class)
public abstract class ChannelAmplifierMixin {
    @Shadow
    @Final
    private int source;

    @Inject(method = "setVolume", at = @At("HEAD"))
    private void simplebuilding$liftGainLimit(float volume, CallbackInfo ci) {
        if (volume > 1.0F) {
            AL10.alSourcef(source, AL10.AL_MAX_GAIN, volume);
        }
    }
}
