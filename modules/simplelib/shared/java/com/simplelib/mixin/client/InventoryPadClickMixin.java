package com.simplelib.mixin.client;

import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiStyleToggle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * N57-5 (owner 14.11.): the styled inventory box keeps {@link UiBoxes#INVENTORY_PAD} px under the hotbar and so hangs
 * that far below a screen's image. A click on those rows counts as inside the screen - otherwise it would throw the
 * carried stack. Only while the box style is on; the recipe book screens decide on their own, so both are targeted.
 */
@Mixin({AbstractContainerScreen.class, AbstractRecipeBookScreen.class})
public abstract class InventoryPadClickMixin {
    @Inject(method = "hasClickedOutside", at = @At("RETURN"), cancellable = true)
    private void simplelib$padInside(double mx, double my, int xo, int yo, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || !UiStyleToggle.isEnabled()) return;
        ContainerScreenAccessor screen = (ContainerScreenAccessor) (Object) this;
        int bottom = yo + screen.simplelib$imageHeight();
        if (mx >= xo && mx < xo + screen.simplelib$imageWidth() && my >= bottom && my < bottom + UiBoxes.INVENTORY_PAD) {
            cir.setReturnValue(false);
        }
    }
}
