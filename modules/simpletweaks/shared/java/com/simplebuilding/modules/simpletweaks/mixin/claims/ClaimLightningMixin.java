package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(LightningBolt.class)
public abstract class ClaimLightningMixin {
 @WrapOperation(method="spawnFire",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
 private BlockState claims$fire(ServerLevel level,BlockPos pos,Operation<BlockState> original){
  // A protected cell is not an ignition candidate; Vanilla's success counter stays accurate.
  return Claims.environmentBlock(level,pos)?original.call(level,pos):Blocks.STONE.defaultBlockState();
 }
 @WrapOperation(method="clearCopperOnLightningStrike",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
 private static boolean claims$copper(Level level,BlockPos pos,BlockState state,Operation<Boolean> original){
  return (!(level instanceof ServerLevel server)||Claims.environmentBlock(server,pos))&&original.call(level,pos,state);
 }
 @WrapOperation(method="randomStepCleaningCopper",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
 private static BlockState claims$step(Level level,BlockPos pos,Operation<BlockState> original){
  // Skip this candidate before both the lambda mutation and its cleaning particles.
  return level instanceof ServerLevel server&&!Claims.environmentBlock(server,pos)?Blocks.STONE.defaultBlockState():original.call(level,pos);
 }
}
