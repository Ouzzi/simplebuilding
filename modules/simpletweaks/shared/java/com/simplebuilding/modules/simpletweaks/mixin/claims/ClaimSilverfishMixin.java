package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
/** A silverfish never infests a claimed block; refused hosts are simply not compatible. */
@Mixin(targets="net.minecraft.world.entity.monster.Silverfish$SilverfishMergeWithStoneGoal")
public abstract class ClaimSilverfishMixin extends RandomStrollGoal {
 private ClaimSilverfishMixin(){super(null,0);}
 @WrapOperation(method="start",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/InfestedBlock;isCompatibleHostBlock(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
 private boolean claims$infest(BlockState state,Operation<Boolean> original,@Local BlockPos pos){
  return original.call(state)&&(!(mob.level() instanceof ServerLevel level)||Claims.mob(mob,level,pos));
 }
}
