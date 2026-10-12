package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.DreadPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WitherSkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Placing a wither skeleton skull on the Dread figure summons the Dread (checked before the Wither's figure). */
@Mixin(WitherSkullBlock.class)
public abstract class WitherSkullDreadMixin {
    @Inject(method = "checkSpawn(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/SkullBlockEntity;)V",
            at = @At("HEAD"), cancellable = true)
    private static void simplemobs$dread(Level level, BlockPos pos, SkullBlockEntity skull, CallbackInfo ci) {
        if (!level.isClientSide() && DreadPattern.trySummon(level, pos)) ci.cancel();
    }
}
