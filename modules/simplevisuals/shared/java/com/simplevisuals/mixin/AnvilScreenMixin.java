package com.simplevisuals.mixin;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.screens.inventory.*;import net.minecraft.world.inventory.AnvilMenu;import net.minecraft.network.chat.Component;import net.minecraft.world.entity.player.Inventory;
@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends AbstractContainerScreen<AnvilMenu> {
 @org.spongepowered.asm.mixin.Unique private net.minecraft.client.gui.components.Button visuals$button;
 private AnvilScreenMixin(AnvilMenu menu,Inventory inv,Component title){super(menu,inv,title);}
 @Inject(method="subInit",at=@At("TAIL")) private void visuals$browser(CallbackInfo ci){
  visuals$button=addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("\u2026"),b->{var stack=menu.getSlot(0).getItem();if(!stack.isEmpty())minecraft.setScreenAndShow(new com.simplevisuals.client.ModelBrowser((AnvilScreen)(Object)this,stack.getItem()));}).bounds(leftPos+4,topPos+43,20,20).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("simplevisuals.models"))).build());
  visuals$button.visible=!menu.getSlot(0).getItem().isEmpty()&&!com.simplevisuals.client.RenamedModels.entries(menu.getSlot(0).getItem().getItem()).isEmpty();
 }
 @Inject(method="containerTick",at=@At("TAIL")) private void visuals$buttonState(CallbackInfo ci){if(visuals$button!=null){var stack=menu.getSlot(0).getItem();visuals$button.visible=!stack.isEmpty()&&!com.simplevisuals.client.RenamedModels.entries(stack.getItem()).isEmpty();}}
 @Inject(method="extractLabels",at=@At("TAIL")) private void visuals$uses(net.minecraft.client.gui.GuiGraphicsExtractor g,int mx,int my,CallbackInfo ci){
  var stack=menu.getSlot(0).getItem();if(stack.isEmpty()||!menu.getSlot(2).getItem().isEmpty())return;
  int cost=stack.getOrDefault(net.minecraft.core.component.DataComponents.REPAIR_COST,0);int uses=cost<=0?0:31-Integer.numberOfLeadingZeros(cost+1);
  if(uses>0)g.text(font,Component.translatable("simplevisuals.screen.anvil_uses",uses),80,68,0xff404040,false);
 }
}
