package com.simplequalityoflife.mixin;
import com.simplequalityoflife.*;
import com.simplequalityoflife.config.SimplequalityoflifeConfig;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public abstract class LadderSpeedMixin {
 @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method="handleRelativeFrictionAndCalculateMovement",at=@At(value="CONSTANT",args="doubleValue=0.2"))
 private double qol$up(double speed){var e=(LivingEntity)(Object)this;return e instanceof Player&&e.onClimbable()&&!e.isShiftKeyDown()?Simplequalityoflife.configFor(e.level()).qOL.ladderClimbingSpeed:speed;}
 @Inject(method="handleOnClimbable",at=@At("RETURN"),cancellable=true)
 private void qol$down(Vec3 motion,CallbackInfoReturnable<Vec3> ci){
  var e=(LivingEntity)(Object)this;var c=Simplequalityoflife.configFor(e.level()).qOL;var v=ci.getReturnValue();
  if(e instanceof Player&&e.onClimbable()&&c.enableFastLadderSlide&&e.getXRot()>45&&v.y<=0
    &&(c.ladderSlideActivation==SimplequalityoflifeConfig.SlideActivationMode.CAMERA?e.isShiftKeyDown():!e.isShiftKeyDown()))ci.setReturnValue(new Vec3(v.x,-c.ladderSlideSpeed,v.z));
 }
}
