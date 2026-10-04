package com.simplebuilding.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simplebuilding.util.SpeakerBoost;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelEventHandler;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Astralit-Lautsprecher (2026-10-03): das Plattenspieler-Stueck startet mit Vanillas Lautstaerke 4,0 mal dem Faktor der
 * angrenzenden Lautsprecher ({@link SpeakerBoost}, Config vom Server). Sonst genau Vanillas Klang (Kategorie Platten,
 * lineare Abschwaechung, nicht wiederholt) - kein zweiter Klang, keine Verzoegerung.
 */
@Mixin(LevelEventHandler.class)
public abstract class LevelEventHandlerSpeakerMixin {
    @Shadow
    @Final
    private ClientLevel level;

    @WrapOperation(method = "playJukeboxSong", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/sounds/SimpleSoundInstance;forJukeboxSong(Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/client/resources/sounds/SimpleSoundInstance;"))
    private SimpleSoundInstance simplebuilding$speakerVolume(SoundEvent sound, Vec3 at, Operation<SimpleSoundInstance> original) {
        float multiplier = SpeakerBoost.multiplier(level, BlockPos.containing(at), SpeakerBoost.Source.JUKEBOX);
        if (multiplier <= 1.0F) {
            return original.call(sound, at);
        }
        return new SimpleSoundInstance(sound.location(), SoundSource.RECORDS, SpeakerBoost.JUKEBOX_VOLUME * multiplier, 1.0F,
                SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.LINEAR, at.x, at.y, at.z, false);
    }
}
