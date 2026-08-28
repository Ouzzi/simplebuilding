package com.simplebuilding.mixin;

import com.simplebuilding.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Monster.class)
public class HostileEntityMixin {
    @Inject(method = "isDarkEnoughToSpawn", at = @At("HEAD"), cancellable = true)
    private static void allowSpawnOnConstructionLight(ServerLevelAccessor world, BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> cir) {
        if (world.getBlockState(pos.below()).is(ModBlocks.CONSTRUCTION_LIGHT)) {
            cir.setReturnValue(true);
        }
    }
}