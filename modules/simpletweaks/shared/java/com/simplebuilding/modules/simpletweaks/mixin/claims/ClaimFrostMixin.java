package com.simplebuilding.modules.simpletweaks.mixin.claims;

import com.simplebuilding.modules.simpletweaks.claims.Claims;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Frost Walker turns water to ice only where its wearer may build (mobs: never on claimed land). */
@Mixin(net.minecraft.world.item.enchantment.effects.ReplaceDisk.class)
public abstract class ClaimFrostMixin {
 @WrapOperation(method="apply",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
 private boolean claims$ice(ServerLevel level,BlockPos pos,BlockState state,Operation<Boolean> original,@Local(argsOnly=true) Entity entity){
  return Claims.action(entity,level,pos)&&original.call(level,pos,state);
 }
}
