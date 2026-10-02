package com.simpleriding.mixin;

import com.simpleriding.*;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Appends the four hoof slots after the player inventory, identically on client and server. */
@Mixin(HorseInventoryMenu.class)
public abstract class HorseshoeMenuMixin extends AbstractMountInventoryMenu {
 private HorseshoeMenuMixin(){super(0,null,null,null);}
 @Inject(method="<init>",at=@At("TAIL"))
 private void simpleriding$slots(int containerId,Inventory inventory,Container horseInventory,AbstractHorse horse,int columns,CallbackInfo ci){
  if(!Horseshoes.canWear(horse))return;
  var shoes=Horseshoes.container(horse);
  for(int i=0;i<Horseshoes.SLOTS;i++)this.addSlot(new HorseshoeSlot(shoes,i));
 }
}
