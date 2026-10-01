package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(net.minecraft.world.level.material.LavaFluid.class)
public abstract class ClaimLavaFireMixin {
 @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="randomTick",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
 private boolean claims$fire(ServerLevel level,BlockPos pos,BlockState state,com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original){return Claims.environmentBlock(level,pos)&&original.call(level,pos,state);}
}
