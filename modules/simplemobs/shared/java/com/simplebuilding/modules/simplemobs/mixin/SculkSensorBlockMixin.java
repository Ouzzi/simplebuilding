package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.HowlerRitual;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hollow Howler ritual: a sculk sensor that actually heard a vibration tells the ritual (frequency + source). */
@Mixin(SculkSensorBlock.class)
public abstract class SculkSensorBlockMixin {
    @Inject(method = "activate", at = @At("TAIL"))
    private void simplemobs$howler(@Nullable Entity source, Level level, BlockPos pos, BlockState state, int power, int frequency, CallbackInfo ci) {
        HowlerRitual.onSensorActivated(level, pos, source, frequency);
    }
}
