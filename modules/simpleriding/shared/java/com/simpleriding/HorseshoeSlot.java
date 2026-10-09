package com.simpleriding;

import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** One hoof slot in the horse inventory; accepts exactly one horseshoe. */
public final class HorseshoeSlot extends Slot {
 /** Item position of the top-left hoof slot; the four sit in a 2x2 grid (columns {@code X}/{@code X+STEP}). */
 public static final int X=-36, Y=18, STEP=18;
 public HorseshoeSlot(Container shoes,int index){super(shoes,index,X+(index%2)*STEP,Y+(index/2)*STEP);}
 @Override public boolean mayPlace(ItemStack stack){return Horseshoes.isHorseshoe(stack);}
 @Override public int getMaxStackSize(){return 1;}
 @Override public int getMaxStackSize(ItemStack stack){return 1;}
 @Override public Identifier getNoItemIcon(){return Horseshoes.EMPTY_SLOT_ICON;}
}
