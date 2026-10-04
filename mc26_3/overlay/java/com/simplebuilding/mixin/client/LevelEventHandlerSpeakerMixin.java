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
 * Amplified jukebox playback uses full gain inside the existing range, with one sound instance
 * following the nearest playback point. This also covers direct amplification with chains disabled.
 * Jukeboxes without active amplifiers keep vanilla playback.
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
        float multiplier = SpeakerBoost.multiplier(level, pos, SpeakerBoost.Source.JUKEBOX);
        if (multiplier > 1.0F || !SpeakerBoost.chain(level, pos, SpeakerBoost.Source.JUKEBOX).isEmpty()) {
            return new com.simplebuilding.client.ChainedJukeboxSound(sound, level, pos, SpeakerBoost.JUKEBOX_VOLUME * multiplier);
        }
        return original.call(sound, at);
    }
}
