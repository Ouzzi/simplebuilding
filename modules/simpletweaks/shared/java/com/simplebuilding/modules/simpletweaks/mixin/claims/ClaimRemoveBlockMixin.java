package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.RemoveBlockGoal;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
/** Zombies removing turtle eggs (and modded removers) respect claimed land. */
@Mixin(RemoveBlockGoal.class)
public abstract class ClaimRemoveBlockMixin {
 @Shadow @Final private Mob removerMob;
 @WrapOperation(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
 private boolean claims$remove(Level level,BlockPos pos,boolean moving,Operation<Boolean> original){
  return (!(level instanceof ServerLevel server)||Claims.mob(removerMob,server,pos))&&original.call(level,pos,moving);
 }
}
