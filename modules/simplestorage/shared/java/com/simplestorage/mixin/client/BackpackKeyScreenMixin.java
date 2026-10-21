package com.simplestorage.mixin.client;

import com.simplestorage.client.BackpackKeyHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The backpack key closes what it opened (backpack, inventory, creative inventory), the way E closes the inventory (split
 * stage 8c-2, from SimpleBuilding's {@code HandledScreenMixin}). At the method head rather than at E's {@code matches}
 * call: NeoForge replaces that with {@code isActiveAndMatches}.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class BackpackKeyScreenMixin extends Screen {
    protected BackpackKeyScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void simplestorage$backpackKeyCloses(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (BackpackKeyHandler.closesOnBackpackKey(this, event)) {
            this.onClose();
            cir.setReturnValue(true);
        }
    }
}
