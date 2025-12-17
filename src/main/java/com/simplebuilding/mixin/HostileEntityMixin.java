package com.simplebuilding.mixin;

import com.simplebuilding.blocks.ModBlocks;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HostileEntity.class)
public class HostileEntityMixin {
    @Inject(method = "isSpawnDark", at = @At("HEAD"), cancellable = true)
    private static void allowSpawnOnConstructionLight(ServerWorldAccess world, BlockPos pos, Random random, CallbackInfoReturnable<Boolean> cir) {
        if (world.getBlockState(pos.down()).isOf(ModBlocks.CONSTRUCTION_LIGHT)) {
            cir.setReturnValue(true);
        }
    }
}