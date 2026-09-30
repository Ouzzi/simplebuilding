package com.simplevisuals.mixin;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(net.minecraft.world.item.ItemStack.class)
public abstract class TooltipMixin {
 @Inject(method="getTooltipImage",at=@At("HEAD"),cancellable=true)
 private void visuals$map(CallbackInfoReturnable<java.util.Optional<net.minecraft.world.inventory.tooltip.TooltipComponent>> ci){
  var mc=net.minecraft.client.Minecraft.getInstance();var stack=(net.minecraft.world.item.ItemStack)(Object)this;
  if(!com.simplevisuals.Visuals.CONFIG.visuals.enableMapTooltips||mc.level==null||!(stack.getItem() instanceof net.minecraft.world.item.MapItem))return;
  var id=stack.get(net.minecraft.core.component.DataComponents.MAP_ID);
  if(id!=null){var data=net.minecraft.world.item.MapItem.getSavedData(id,mc.level);if(data!=null)ci.setReturnValue(java.util.Optional.of(new com.simplevisuals.client.MapPreview(id,data)));}
 }
}
