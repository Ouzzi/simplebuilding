package com.simplelib.mixin;

import com.simplelib.warm.Warm;
import com.simplelib.warm.WarmMerge;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bundles keep food warm longer (owner: two day-night cycles instead of half a cycle), every bundle
 * including mod bundles built on {@code BundleContents} (owner 38). Food entering a bundle has its
 * remaining time stretched, food leaving it is set back to normal cooling; whatever stays in the
 * player's hand after a partial insert is set back too.
 */
@Mixin(BundleContents.Mutable.class)
public abstract class WarmBundleMixin {
    @Inject(method = "tryInsert", at = @At("HEAD"))
    private void simplelib$insulate(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        Warm.insulate(stack, WarmMerge.now());
    }

    @Inject(method = "tryInsert", at = @At("RETURN"))
    private void simplelib$restoreRest(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (!stack.isEmpty()) Warm.uninsulate(stack, WarmMerge.now());
    }

    @Inject(method = "removeOne", at = @At("RETURN"))
    private void simplelib$uninsulate(CallbackInfoReturnable<ItemStack> cir) {
        ItemStack out = cir.getReturnValue();
        if (out != null && !out.isEmpty()) Warm.uninsulate(out, WarmMerge.now());
    }
}
