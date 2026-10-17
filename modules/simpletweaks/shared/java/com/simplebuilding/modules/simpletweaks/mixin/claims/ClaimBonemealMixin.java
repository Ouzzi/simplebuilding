package com.simplebuilding.modules.simpletweaks.mixin.claims;

import com.simplebuilding.modules.simpletweaks.claims.Claims;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.GrassBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Bone meal on a spreader (grass, nylium, moss, ...) is no valid use when its spread would cover foreign land. */
@Mixin({GrassBlock.class,net.minecraft.world.level.block.NyliumBlock.class,net.minecraft.world.level.block.NetherrackBlock.class,net.minecraft.world.level.block.BonemealableFeaturePlacerBlock.class})
public abstract class ClaimBonemealMixin {
 @Inject(method="isValidBonemealTarget",at=@At("HEAD"),cancellable=true)
 private void claims$spread(LevelReader reader,BlockPos pos,net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.block.BonemealSource source,CallbackInfoReturnable<Boolean> cir){
  if(reader instanceof ServerLevel server&&!Claims.sameLand(server,pos,pos,(Object)this instanceof GrassBlock?4:8))cir.setReturnValue(false);
 }
}
