package com.simplebuilding.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simplebuilding.util.SpeakerBoost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NoteBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Nihilit-Lautsprecher (2026-10-03): der Notenblock spielt seinen Ton mit der verstaerkten Lautstaerke
 * ({@link SpeakerBoost#noteBlockVolume}) - derselbe Ton zur selben Zeit, nur weiter hoerbar. Der Server entscheidet;
 * Vanilla schickt den Ton an jeden Spieler in der Hoerweite der Lautstaerke.
 */
@Mixin(NoteBlock.class)
public abstract class NoteBlockSpeakerMixin {
    @WrapOperation(method = "triggerEvent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;playSeededSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V"))
    private void simplebuilding$speakerVolume(Level level, Entity except, double x, double y, double z, Holder<SoundEvent> sound,
                                              SoundSource source, float volume, float pitch, long seed, Operation<Void> original) {
        float boosted = volume * SpeakerBoost.multiplier(level, BlockPos.containing(x, y, z), SpeakerBoost.Source.NOTE_BLOCK);
        original.call(level, except, x, y, z, sound, source, boosted, pitch, seed);
    }
}
