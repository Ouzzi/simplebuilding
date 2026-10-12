package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.modules.simpletweaks.claims.ClaimAutomation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(TransportItemsBetweenContainers.class)
public abstract class ClaimTransportMixin {
 @Inject(method="pickUpItems",at=@At("HEAD"),cancellable=true)
 private void claims$pickup(PathfinderMob body,Container container,CallbackInfo ci){
  if(!ClaimAutomation.carry(body,container,true))ci.cancel();
 }
 @Inject(method="putDownItem",at=@At("HEAD"),cancellable=true)
 private void claims$putdown(PathfinderMob body,Container container,CallbackInfo ci){
  if(!ClaimAutomation.carry(body,container,false))ci.cancel();
 }
}
