package com.simplevisuals.mixin;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.class)
public interface ClientTooltipMixin {
 @Inject(method="create(Lnet/minecraft/world/inventory/tooltip/TooltipComponent;)Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;",at=@At("HEAD"),cancellable=true)
 private static void visuals$map(net.minecraft.world.inventory.tooltip.TooltipComponent data,CallbackInfoReturnable<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> ci){if(data instanceof com.simplevisuals.client.MapPreview map)ci.setReturnValue(map);}
}
