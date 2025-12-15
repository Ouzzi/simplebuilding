package com.simplebuilding.mixin;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Shadow public abstract Item getItem();

    // Erlaubt zur Laufzeit (im Spiel) mehr als 64 Items im Stack.
    @Inject(method = "getMaxCount", at = @At("HEAD"), cancellable = true)
    private void allowLargerStacks(CallbackInfoReturnable<Integer> cir) {
        if (this.getItem().getMaxCount() == 64) {
            cir.setReturnValue(1024); // Limit hochsetzen
        }
    }
}