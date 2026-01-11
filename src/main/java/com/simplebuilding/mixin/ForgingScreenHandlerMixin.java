package com.simplebuilding.mixin;

import com.simplebuilding.items.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.SmithingScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ForgingScreenHandler.class)
public abstract class ForgingScreenHandlerMixin {

    /**
     * Zwingt den Schmiedetisch, unsere Items zu akzeptieren.
     * Dies behebt das Problem, dass man die Items nicht reinlegen kann
     * und dass Shift-Klick nicht funktioniert.
     */
    @Inject(method = "isValidIngredient", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$allowGlowingIngredients(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        // Prüfen, ob wir im Schmiedetisch sind (denn ForgingScreenHandler ist auch für Amboss etc.)
        if ((Object) this instanceof SmithingScreenHandler screenHandler) {
            
            // Slot 0: Template
            if (stack.isOf(ModItems.GLOWING_TRIM_TEMPLATE)) {
                // Erlauben, wenn der Slot leer ist oder wir das gleiche Item halten
                if (!screenHandler.getSlot(0).hasStack()) {
                    cir.setReturnValue(true);
                }
            }
            
            // Slot 2: Material (Glow Ink)
            if (stack.isOf(Items.GLOW_INK_SAC)) {
                if (!screenHandler.getSlot(2).hasStack()) {
                    cir.setReturnValue(true);
                }
            }
        }
    }
}