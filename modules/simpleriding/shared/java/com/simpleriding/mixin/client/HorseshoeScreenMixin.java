package com.simpleriding.mixin.client;

import com.simpleriding.*;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiPalette;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The hoof panel left of the horse inventory in the container style (SimpleLib's style blocks, IRON palette;
 * simplecontainers preview W0-B decision 9): a tab whose right end lies under the horse window's left frame, so the
 * slot (x -20) and two frames fit into the 20 px. Drawn right after the dimmed world and before the horse window
 * (Vanilla PNG or a styled box), which then covers that end. Empty hoof slots keep their own horseshoe icon
 * ({@code HorseshoeSlot#getNoItemIcon}, drawn by Vanilla over the slot).
 */
@Mixin(AbstractMountInventoryScreen.class)
public abstract class HorseshoeScreenMixin<T extends AbstractMountInventoryMenu> extends AbstractContainerScreen<T> {
 private HorseshoeScreenMixin(){super(null,null,Component.empty());}
 @Inject(method="extractBackground",at=@At(value="INVOKE",shift=At.Shift.AFTER,
   target="Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
 private void simpleriding$panel(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float a,CallbackInfo ci){
  if(!HorseshoePanel.hasShoeSlots(this.menu))return;
  int xo=this.leftPos, yo=this.topPos;
  UiPalette p=UiPalette.IRON;
  UiBoxes.box(graphics,xo+HorseshoePanel.X,yo+HorseshoePanel.Y,HorseshoePanel.BOX_W,HorseshoePanel.H,p);
  for(Slot slot:this.menu.slots){
   if(!(slot instanceof HorseshoeSlot))continue;
   UiBoxes.slot(graphics,xo+slot.x,yo+slot.y,p);
  }
 }
}
