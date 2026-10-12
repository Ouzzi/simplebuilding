package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.goal.DoorInteractGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Zombies and Vindicators never start breaking a claimed door. */
@Mixin(BreakDoorGoal.class)
public abstract class ClaimDoorMixin extends DoorInteractGoal {
 private ClaimDoorMixin(){super(null);}
 @Inject(method="canUse",at=@At("RETURN"),cancellable=true)
 private void claims$door(CallbackInfoReturnable<Boolean> cir){
  if(cir.getReturnValueZ()&&mob.level() instanceof ServerLevel level&&!Claims.mob(mob,level,doorPos))cir.setReturnValue(false);
 }
 /** Also stops a break already in progress when the door's chunk became claimed meanwhile. */
 @Inject(method="tick",at=@At("HEAD"),cancellable=true)
 private void claims$progress(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){
  if(mob.level() instanceof ServerLevel level&&!Claims.mob(mob,level,doorPos))ci.cancel();
 }
}
