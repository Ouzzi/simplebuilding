package com.simpleoctets.mixin;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sneak + right-click with a melon slice places it as a melon octet ({@link com.simpleoctets.MelonSlices#tryPlace}); at the
 * base {@code Item#useOn}, which the melon slice does not override. Every other item gets null at once.
 */
@Mixin(Item.class)
public abstract class MelonSliceUseMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void simpleoctets$placeMelonSlice(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult result = com.simpleoctets.MelonSlices.tryPlace(context);
        if (result != null) {
            cir.setReturnValue(result);
        }
    }
}
