package com.simpleriding.mixin.client;

import com.simpleriding.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla-style side panel with the four hoof slots to the left of the horse inventory. */
@Mixin(AbstractMountInventoryScreen.class)
public abstract class HorseshoeScreenMixin<T extends AbstractMountInventoryMenu> extends AbstractContainerScreen<T> {
 @Unique private static final Identifier SIMPLERIDING$PANEL=Riding.id("textures/gui/container/horseshoe_panel.png");
 @Unique private static final Identifier SIMPLERIDING$SLOT=Identifier.withDefaultNamespace("container/slot");
 private HorseshoeScreenMixin(){super(null,null,Component.empty());}
 @Inject(method="extractBackground",at=@At("TAIL"))
 private void simpleriding$panel(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float a,CallbackInfo ci){
  if(!HorseshoePanel.hasShoeSlots(this.menu))return;
  int xo=this.leftPos, yo=this.topPos;
  graphics.blit(RenderPipelines.GUI_TEXTURED,SIMPLERIDING$PANEL,xo+HorseshoePanel.X,yo+HorseshoePanel.Y,0F,0F,HorseshoePanel.W,HorseshoePanel.H,HorseshoePanel.W,HorseshoePanel.H);
  for(int i=0;i<Horseshoes.SLOTS;i++)graphics.blitSprite(RenderPipelines.GUI_TEXTURED,SIMPLERIDING$SLOT,xo+HorseshoeSlot.X-1,yo+HorseshoeSlot.Y-1+i*18,18,18);
 }
}
