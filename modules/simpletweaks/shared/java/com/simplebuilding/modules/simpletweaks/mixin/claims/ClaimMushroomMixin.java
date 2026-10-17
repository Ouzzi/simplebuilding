package com.simplebuilding.modules.simpletweaks.mixin.claims;

import com.simplebuilding.modules.simpletweaks.claims.Claims;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.MushroomBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Huge mushrooms are trees for claim purposes. */
@Mixin(MushroomBlock.class)
public abstract class ClaimMushroomMixin {
 @Inject(method="growMushroom",at=@At("HEAD"),cancellable=true)
 private void claims$grow(ServerLevel level,BlockPos pos,net.minecraft.world.level.block.state.BlockState state,net.minecraft.util.RandomSource random,CallbackInfoReturnable<Boolean> cir){
  if(!Claims.growth(level,pos))cir.setReturnValue(false);
 }
}
