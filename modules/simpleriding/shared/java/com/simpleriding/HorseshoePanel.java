package com.simpleriding;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;

/** Geometry of the hoof panel left of the horse inventory (relative to the screen's left/top). */
public final class HorseshoePanel {
 private HorseshoePanel() {}
 public static final int X=-24, Y=12, W=24, H=82;
 public static boolean hasShoeSlots(AbstractContainerMenu menu){
  if(!(menu instanceof HorseInventoryMenu))return false;
  for(var slot:menu.slots)if(slot instanceof HorseshoeSlot)return true;
  return false;
 }
 public static boolean contains(double x,double y){return x>=X&&x<X+W&&y>=Y&&y<Y+H;}
}
