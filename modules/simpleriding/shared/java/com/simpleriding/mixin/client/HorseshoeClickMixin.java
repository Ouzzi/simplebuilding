package com.simpleriding.mixin.client;

import com.simpleriding.HorseshoePanel;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Clicks on the hoof panel are inside the screen (otherwise Vanilla would throw the carried item). */
@Mixin(AbstractContainerScreen.class)
public abstract class HorseshoeClickMixin {
 @Shadow @Final protected AbstractContainerMenu menu;
 @Inject(method="hasClickedOutside",at=@At("HEAD"),cancellable=true)
 private void simpleriding$panel(double mx,double my,int xo,int yo,CallbackInfoReturnable<Boolean> cir){
  if(HorseshoePanel.hasShoeSlots(menu)&&HorseshoePanel.contains(mx-xo,my-yo))cir.setReturnValue(false);
 }
}
