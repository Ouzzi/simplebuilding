package com.simpleriding;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;

/** Geometry of the hoof panel left of the horse inventory (relative to the screen's left/top). */
public final class HorseshoePanel {
 private HorseshoePanel() {}
 /** Clickable part left of the horse window; the drawn box is {@link #BOX_W} wide and ends under its left frame.
  *  The four hoof slots sit in a 2x2 grid, so the box stays a short tab above the container box's bottom (y 77). */
 public static final int X=-44, Y=10, W=44, H=53, BOX_W=49; // N64 (A7 container margin): 3 px fill round the 2x2 hoof slots (left, top, bottom; the right side tucks under the window)
 public static boolean hasShoeSlots(AbstractContainerMenu menu){
  if(!(menu instanceof HorseInventoryMenu))return false;
  for(var slot:menu.slots)if(slot instanceof HorseshoeSlot)return true;
  return false;
 }
 public static boolean contains(double x,double y){return x>=X&&x<X+W&&y>=Y&&y<Y+H;}
}
