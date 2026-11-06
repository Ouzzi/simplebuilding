package com.simplelib.mixin.client;

import com.simplelib.api.client.SlotAlert;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** {@link SlotAlert}: the red lighthouse beam behind an item whose rule is active, in every container screen. */
@Mixin(AbstractContainerScreen.class)
public abstract class SlotAlertMixin {
    @Inject(method = "extractSlot", at = @At("HEAD"))
    private void simplelib$alert(GuiGraphicsExtractor g, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        if (SlotAlert.active(slot.getItem())) SlotAlert.draw(g, slot.x, slot.y);
    }
}
