package com.simplebuilding.modules.simpleinterfaces.mixin;

import com.simplebuilding.modules.simpleinterfaces.client.ContainerPreviewTooltip;
import com.simplebuilding.modules.simpleinterfaces.tooltip.ContainerPreview;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Turns the shulker / ender chest preview into its drawing on every loader (same way as simplevisuals' map preview). */
@Mixin(ClientTooltipComponent.class)
public interface ClientTooltipPreviewMixin {
    @Inject(method = "create(Lnet/minecraft/world/inventory/tooltip/TooltipComponent;)Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;",
            at = @At("HEAD"), cancellable = true)
    private static void simpleinterfaces$preview(TooltipComponent data, CallbackInfoReturnable<ClientTooltipComponent> ci) {
        if (data instanceof ContainerPreview preview) ci.setReturnValue(new ContainerPreviewTooltip(preview));
    }
}
