package com.simpleriding.mixin;

import com.simpleriding.*;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Shift-click: horseshoes go to free hoof slots first; hoof and chest slots only move into the player inventory. */
@Mixin(AbstractMountInventoryMenu.class)
public abstract class HorseshoeQuickMoveMixin extends AbstractContainerMenu {
 @Shadow @Final protected Container mountContainer;
 private HorseshoeQuickMoveMixin(){super(null,0);}
 @Inject(method="quickMoveStack",at=@At("HEAD"),cancellable=true)
 private void simpleriding$quickMove(Player player,int index,CallbackInfoReturnable<ItemStack> cir){
  int first=-1;
  for(int i=0;i<this.slots.size();i++)if(this.slots.get(i) instanceof HorseshoeSlot){first=i;break;}
  if(first<0||index<0||index>=this.slots.size())return;
  int playerStart=2+mountContainer.getContainerSize(), playerEnd=playerStart+36;
  Slot slot=this.slots.get(index);
  if(!slot.hasItem()){cir.setReturnValue(ItemStack.EMPTY);return;}
  ItemStack stack=slot.getItem(), copy=stack.copy();
  boolean moved;
  if(index>=first||index<playerStart)moved=this.moveItemStackTo(stack,playerStart,playerEnd,true);
  else if(Horseshoes.isHorseshoe(stack)){moved=this.moveItemStackTo(stack,first,first+Horseshoes.SLOTS,false);if(!moved)return;}
  else return; // other player-inventory items keep the Vanilla rules; their ranges never include hoof slots
  if(!moved){cir.setReturnValue(ItemStack.EMPTY);return;}
  if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
  cir.setReturnValue(copy);
 }
}
