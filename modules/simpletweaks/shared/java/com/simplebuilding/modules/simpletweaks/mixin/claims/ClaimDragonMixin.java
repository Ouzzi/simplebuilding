package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(EnderDragon.class)
public abstract class ClaimDragonMixin {
 @WrapOperation(method="checkWalls",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
 private boolean claims$wall(ServerLevel level,BlockPos pos,boolean moving,Operation<Boolean> original){
  return Claims.mob((EnderDragon)(Object)this,level,pos)&&original.call(level,pos,moving);
 }
}
