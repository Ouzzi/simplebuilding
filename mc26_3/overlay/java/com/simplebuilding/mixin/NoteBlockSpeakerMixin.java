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
 * The server selects listeners using the existing amplifier range and chain. Each receives one
 * unattenuated note from the nearest playback point, including direct amplification without a chain.
 * Note blocks without active amplifiers keep vanilla broadcasting.
 */
@Mixin(NoteBlock.class)
public abstract class NoteBlockSpeakerMixin {
    @WrapOperation(method = "triggerEvent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;playSeededSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V"))
    private void simplebuilding$speakerVolume(Level level, Entity except, double x, double y, double z, Holder<SoundEvent> sound,
                                              SoundSource source, float volume, float pitch, long seed, Operation<Void> original) {
        BlockPos pos = BlockPos.containing(x, y, z);
        float boosted = volume * SpeakerBoost.multiplier(level, pos, SpeakerBoost.Source.NOTE_BLOCK);
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            java.util.List<BlockPos> chain = SpeakerBoost.cachedChain(server, pos, SpeakerBoost.Source.NOTE_BLOCK);
            if (!chain.isEmpty() || boosted > volume) {
                SpeakerBoost.playChained(server, except, pos, chain, sound, source, boosted, pitch, seed);
                return;
            }
        }
        original.call(level, except, x, y, z, sound, source, boosted, pitch, seed);
    }
}
