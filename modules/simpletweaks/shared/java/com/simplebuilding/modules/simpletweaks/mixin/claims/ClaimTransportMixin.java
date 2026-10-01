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
 @Inject(method={"pickUpItems","putDownItem"},at=@At("HEAD"),cancellable=true)
 private void claims$transport(PathfinderMob body,Container container,CallbackInfo ci){
  if(!ClaimAutomation.unowned(body,container))ci.cancel();
 }
}
