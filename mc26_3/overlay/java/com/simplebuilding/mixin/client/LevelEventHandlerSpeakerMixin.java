package com.simplebuilding.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simplebuilding.util.SpeakerBoost;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelEventHandler;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Besitzer N41: der Plattenspieler bleibt Vanilla. Nur wenn Musik-Verstärker in Funkweite sind
 * ({@link SpeakerBoost#chain}), spielt statt Vanillas Klang ein gleich lauter, der zum naechsten Abspielpunkt wandert
 * ({@link com.simplebuilding.client.ChainedJukeboxSound}). Ohne Verstärker: unveraenderter Vanilla-Klang.
 */
@Mixin(LevelEventHandler.class)
public abstract class LevelEventHandlerSpeakerMixin {
    @Shadow
    @Final
    private ClientLevel level;

    @WrapOperation(method = "playJukeboxSong", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/sounds/SimpleSoundInstance;forJukeboxSong(Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/client/resources/sounds/SimpleSoundInstance;"))
    private SimpleSoundInstance simplebuilding$speakerVolume(SoundEvent sound, Vec3 at, Operation<SimpleSoundInstance> original) {
        BlockPos pos = BlockPos.containing(at);
        if (!SpeakerBoost.chain(level, pos, SpeakerBoost.Source.JUKEBOX).isEmpty()) {
            return new com.simplebuilding.client.ChainedJukeboxSound(sound, level, pos);
        }
        return original.call(sound, at);
    }
}
