package com.simplestorage.mixin;

import com.simplestorage.bundle.PlacedBundles;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sneak + right-click with one of Vanilla's bundles (every colour) on a top face places it (split stage 8c-1, the bundle
 * part of SimpleBuilding's {@code ItemUseOnMixin}). Vanilla's {@code BundleItem} does not override {@code useOn}; the mod
 * bundles ask in {@code ReinforcedBundleItem#useOn} themselves.
 */
@Mixin(Item.class)
public abstract class BundlePlaceMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void simplestorage$placeVanillaBundle(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!((Object) this instanceof BundleItem)) return;
        InteractionResult result = PlacedBundles.tryPlace(context);
        if (result != null) cir.setReturnValue(result);
    }
}
