package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.EndNetherServer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.EndGatewayFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Every End gateway (dragon-fight ring, outer-island exits, worldgen) gets its four Shellker guards. */
@Mixin(EndGatewayFeature.class)
public abstract class EndGatewayFeatureMixin {
    @Inject(method = "place", at = @At("RETURN"))
    private void simplemobs$guards(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin, CallbackInfoReturnable<Boolean> cir) {
        EndNetherServer.guardGateway(level, origin);
    }
}
