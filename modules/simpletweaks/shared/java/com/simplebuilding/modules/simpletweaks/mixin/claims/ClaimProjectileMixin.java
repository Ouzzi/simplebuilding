package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(net.minecraft.world.entity.projectile.Projectile.class)
public abstract class ClaimProjectileMixin {
 @Inject(method="hitTargetOrDeflectSelf",at=@At("HEAD"),cancellable=true)
 private void claims$hit(net.minecraft.world.phys.HitResult hit,CallbackInfoReturnable<net.minecraft.world.entity.projectile.ProjectileDeflection> cir){
  var projectile=(net.minecraft.world.entity.projectile.Projectile)(Object)this;
  if(!(projectile.level() instanceof ServerLevel level)||!Claims.enabled(level.getServer()))return;
  var actor=projectile.getOwner();boolean permitted=true;
  if(hit instanceof net.minecraft.world.phys.EntityHitResult entity)permitted=Claims.action(actor,entity.getEntity());
  else if(hit instanceof net.minecraft.world.phys.BlockHitResult block)permitted=Claims.action(actor,level,block.getBlockPos())&&Claims.action(actor,level,block.getBlockPos().relative(block.getDirection()));
  if(!permitted){projectile.discard();cir.setReturnValue(net.minecraft.world.entity.projectile.ProjectileDeflection.NONE);}
 }
}
