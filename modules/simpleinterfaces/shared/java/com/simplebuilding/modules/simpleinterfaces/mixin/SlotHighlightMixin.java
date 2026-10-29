package com.simplebuilding.modules.simpleinterfaces.mixin;

import com.simplebuilding.modules.simpleinterfaces.ContainersConfig;
import com.simplebuilding.modules.simpleinterfaces.client.ContainersClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Clearer slot highlight (BS29-4): only the back half of the highlight is drawn, so it stays behind the item. */
@Mixin(AbstractContainerScreen.class)
public abstract class SlotHighlightMixin {
    @Inject(method = "extractSlotHighlightFront", at = @At("HEAD"), cancellable = true)
    private void sb$behindItem(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (ContainersConfig.on() && ContainersClient.config().slotHighlightBehind) ci.cancel();
    }
}
