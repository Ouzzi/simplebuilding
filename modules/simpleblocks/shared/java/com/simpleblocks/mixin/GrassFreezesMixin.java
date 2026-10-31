package com.simpleblocks.mixin;

import com.simpleblocks.snow.FrostGrassBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SpreadingSnowyBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SN30-2: a Vanilla grass block under snow in the cold (cold biome or ice below) freezes to frost grass instead of its usual
 * random tick ({@link FrostGrassBlock#freezes}); everywhere else and for mycelium Vanilla's tick runs unchanged. Switch
 * {@code snow.frostGrass}.
 */
@Mixin(SpreadingSnowyBlock.class)
public abstract class GrassFreezesMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void simpleblocks$grassFreezesUnderSnow(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if ((Object) this == Blocks.GRASS_BLOCK && FrostGrassBlock.freezes(state, level, pos, random)) {
            ci.cancel();
        }
    }
}
