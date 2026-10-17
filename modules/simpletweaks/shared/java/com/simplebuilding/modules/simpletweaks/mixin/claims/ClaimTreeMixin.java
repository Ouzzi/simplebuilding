package com.simplebuilding.modules.simpletweaks.mixin.claims;

import com.simplebuilding.modules.simpletweaks.claims.Claims;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.grower.TreeGrower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A tree never grows next to land of another owner; its leaves and logs would cross the claim border. */
@Mixin(TreeGrower.class)
public abstract class ClaimTreeMixin {
 @Inject(method="growTree",at=@At("HEAD"),cancellable=true)
 private void claims$grow(ServerLevel level,net.minecraft.world.level.chunk.ChunkGenerator generator,BlockPos pos,net.minecraft.world.level.block.state.BlockState state,net.minecraft.util.RandomSource random,CallbackInfoReturnable<Boolean> cir){
  if(!Claims.growth(level,pos))cir.setReturnValue(false);
 }
}
