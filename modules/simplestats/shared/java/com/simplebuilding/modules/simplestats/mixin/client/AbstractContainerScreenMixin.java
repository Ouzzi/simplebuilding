package com.simplebuilding.modules.simplestats.mixin.client;

import com.simplebuilding.modules.simplestats.client.StatsClient;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The key over an item in any inventory, chest or trading screen opens that item's statistics (not while typing in a text field). */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Shadow protected @Nullable Slot hoveredSlot;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void simplestats$open(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        if (StatsClient.openKey == null || !StatsClient.openKey.matches(event) || self.getFocused() instanceof EditBox) return;
        if (hoveredSlot != null && hoveredSlot.hasItem()) {
            StatsClient.open(hoveredSlot.getItem(), self);
            cir.setReturnValue(true);
        }
    }
}
