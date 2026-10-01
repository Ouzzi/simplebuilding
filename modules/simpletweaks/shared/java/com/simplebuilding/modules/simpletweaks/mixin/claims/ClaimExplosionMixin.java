package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(net.minecraft.world.level.ServerExplosion.class)
public abstract class ClaimExplosionMixin {
 @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="hurtEntities*",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;ignoreExplosion(Lnet/minecraft/world/level/Explosion;)Z"))
 private boolean claims$entities(net.minecraft.world.entity.Entity entity,net.minecraft.world.level.Explosion explosion,com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original){return !Claims.action(null,entity)||original.call(entity,explosion);}
 @Shadow @Final private ServerLevel level;
 @Inject(method="calculateExplodedPositions",at=@At("RETURN"),cancellable=true)
 private void claims$blocks(CallbackInfoReturnable<java.util.List<BlockPos>> cir){
  if(!Claims.enabled(level.getServer()))return;
  var positions=new java.util.ArrayList<>(cir.getReturnValue());positions.removeIf(p->!Claims.environmentBlock(level,p));cir.setReturnValue(positions);
 }
}
