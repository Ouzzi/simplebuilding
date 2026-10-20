package com.simplestorage.mixin;

import com.simplestorage.bundle.PlacedBundles;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sneak + right-click with an item on a placed bundle puts it in (split stage 8c-1, from SimpleBuilding's
 * {@code ItemStackUseOnMixin}): Vanilla does not ask the block while sneaking with an item in hand
 * ({@link PlacedBundles#tryDeposit}).
 */
@Mixin(ItemStack.class)
public abstract class BundleDepositMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void simplestorage$depositIntoPlacedBundle(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult result = PlacedBundles.tryDeposit(context);
        if (result != null) cir.setReturnValue(result);
    }
}
