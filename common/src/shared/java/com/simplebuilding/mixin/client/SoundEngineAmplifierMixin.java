package com.simplebuilding.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simplebuilding.client.AmplifiedSound;
import com.simplebuilding.util.SpeakerBoost;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Verstärker (2026-10-06): Vanilla kappt jede Kanal-Lautstaerke bei 1,0 - ein verstaerkter Plattenspieler oder
 * Notenblock klang darum direkt daneben genau wie ohne Verstärker. Fuer {@link AmplifiedSound}s multipliziert dieser
 * Mixin die fertige Kanal-Lautstaerke (nach den Reglern des Spielers) mit deren Faktor, hoechstens
 * {@link SpeakerBoost#MAX_GAIN}; {@code ChannelAmplifierMixin} hebt dafuer die OpenAL-Obergrenze der Quelle an.
 * Alle anderen Klaenge bleiben unberuehrt.
 */
@Mixin(SoundEngine.class)
public abstract class SoundEngineAmplifierMixin {
    @WrapOperation(method = "play", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/sounds/SoundEngine;calculateVolume(FLnet/minecraft/sounds/SoundSource;)F"))
    private float simplebuilding$amplifyStart(SoundEngine engine, float volume, SoundSource source, Operation<Float> original,
                                              @Local(argsOnly = true) SoundInstance instance) {
        return simplebuilding$amplify(instance, original.call(engine, volume, source));
    }

    @WrapOperation(method = "tickInGameSound", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/sounds/SoundEngine;calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F"))
    private float simplebuilding$amplifyTick(SoundEngine engine, SoundInstance instance, Operation<Float> original) {
        return simplebuilding$amplify(instance, original.call(engine, instance));
    }

    private static float simplebuilding$amplify(SoundInstance instance, float volume) {
        if (instance instanceof AmplifiedSound amplified) {
            float gain = amplified.simplebuilding$gain();
            return volume * (Float.isNaN(gain) ? 1.0F : Math.min(Math.max(gain, 1.0F), SpeakerBoost.MAX_GAIN));
        }
        return volume;
    }
}
