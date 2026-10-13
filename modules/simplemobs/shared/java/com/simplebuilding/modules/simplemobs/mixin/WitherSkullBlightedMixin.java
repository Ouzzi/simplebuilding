package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.BlightedPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WitherSkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The last wither skeleton skull on a Wither figure whose middle skull wears a Netherite or Enderite Helmet summons the Blighted or Abyssal Wither. */
@Mixin(WitherSkullBlock.class)
public abstract class WitherSkullBlightedMixin {
    @Inject(method = "checkSpawn(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/SkullBlockEntity;)V",
            at = @At("HEAD"), cancellable = true)
    private static void simplemobs$blighted(Level level, BlockPos pos, SkullBlockEntity skull, CallbackInfo ci) {
        if (!level.isClientSide() && BlightedPattern.trySummon(level, pos)) ci.cancel();
    }
}
