package com.simplebuilding.mixin;

import com.simplebuilding.util.PlacedBundles;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Schleichen + Rechtsklick mit einem Item auf ein abgestelltes Buendel legt das Item hinein
 * ({@link PlacedBundles#tryDeposit}). Vanilla fragt beim Schleichen mit Item in der Hand den Block
 * nicht ({@code ServerPlayerGameMode#useItemOn} bzw. {@code MultiPlayerGameMode#performUseItemOn}),
 * sondern geht direkt zu {@code ItemStack#useOn}; am Stapel statt an {@code Item#useOn}, damit auch
 * Block-Items und alle Items mit eigenem {@code useOn} ankommen. Beide Seiten laufen hier durch, der
 * Client liefert nur SUCCESS (Armschwung), veraendert wird nur auf dem Server.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackUseOnMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$depositIntoPlacedBundle(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult result = PlacedBundles.tryDeposit(context);
        if (result != null) {
            cir.setReturnValue(result);
        }
    }
}
