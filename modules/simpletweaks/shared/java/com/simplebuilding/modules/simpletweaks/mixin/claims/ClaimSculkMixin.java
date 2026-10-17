package com.simplebuilding.modules.simpletweaks.mixin.claims;

import com.simplebuilding.modules.simpletweaks.claims.Claims;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A sculk charge dies where it would leave the land of its catalyst (or touch a neighbouring claim). */
@Mixin(targets="net.minecraft.world.level.block.SculkSpreader$ChargeCursor")
public abstract class ClaimSculkMixin {
 @Shadow private int charge;
 @Shadow public abstract BlockPos getPos();
 @Inject(method="update",at=@At("HEAD"),cancellable=true)
 private void claims$border(LevelAccessor level,BlockPos origin,RandomSource random,net.minecraft.world.level.block.SculkSpreader spreader,boolean veins,CallbackInfo ci){
  if(level instanceof ServerLevel server&&!Claims.sameLand(server,origin,getPos(),1)){charge=0;ci.cancel();}
 }
}
