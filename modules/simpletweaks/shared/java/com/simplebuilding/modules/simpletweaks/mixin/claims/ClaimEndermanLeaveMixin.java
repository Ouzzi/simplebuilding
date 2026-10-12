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
/** A protected placement cell reads as occupied, so the carried block stays with the Enderman. */
@Mixin(targets="net.minecraft.world.entity.monster.Enderman$EndermanLeaveBlockGoal")
public abstract class ClaimEndermanLeaveMixin {
 @Shadow @Final private Enderman enderman;
 @WrapOperation(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",ordinal=0))
 private BlockState claims$leave(Level level,BlockPos pos,Operation<BlockState> original){
  return level instanceof ServerLevel server&&!Claims.mob(enderman,server,pos)?Blocks.STONE.defaultBlockState():original.call(level,pos);
 }
}
