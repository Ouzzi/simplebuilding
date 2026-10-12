package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
/** A protected cell is never a holdable candidate, so nothing is removed or carried. */
@Mixin(targets="net.minecraft.world.entity.monster.Enderman$EndermanTakeBlockGoal")
public abstract class ClaimEndermanTakeMixin {
 @Shadow @Final private Enderman enderman;
 @WrapOperation(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
 private BlockState claims$take(Level level,BlockPos pos,Operation<BlockState> original){
  return level instanceof ServerLevel server&&!Claims.mob(enderman,server,pos)?Blocks.AIR.defaultBlockState():original.call(level,pos);
 }
}
