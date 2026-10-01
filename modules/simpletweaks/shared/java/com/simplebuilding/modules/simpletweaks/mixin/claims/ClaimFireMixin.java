package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(net.minecraft.world.level.block.FireBlock.class)
public abstract class ClaimFireMixin {
 @Inject(method="tick",at=@At("HEAD"),cancellable=true)
 private void claims$tick(BlockState state,ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random,CallbackInfo ci){if(!Claims.environment(level,pos))ci.cancel();}
 @Inject(method="checkBurnOut",at=@At("HEAD"),cancellable=true)
 private void claims$burn(CallbackInfo ci,@com.llamalad7.mixinextras.sugar.Local(argsOnly=true) Level level,@com.llamalad7.mixinextras.sugar.Local(argsOnly=true) BlockPos pos){if(level instanceof ServerLevel server&&!Claims.environmentBlock(server,pos))ci.cancel();}
 @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
 private boolean claims$spread(ServerLevel level,BlockPos pos,BlockState state,com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original){return Claims.environmentBlock(level,pos)&&original.call(level,pos,state);}
}
