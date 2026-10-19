package com.simplebuilding.modules.simpleinterfaces.mixin;

import com.simplebuilding.modules.simpleinterfaces.client.BundleViews;
import net.minecraft.world.item.component.BundleContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client: the mouse wheel reaches every bundle entry, not only the twelve Vanilla shows. Vanilla's
 * {@code BundleMouseActions} and mod bundle handlers (SimpleBuilding's reinforced bundle) both read this count; the
 * server already accepts any index below the entry count ({@code BundleContents.Mutable.toggleSelectedItem}).
 */
@Mixin(BundleContents.class)
public abstract class BundleSelectionMixin {
    @Inject(method = "getNumberOfItemsToShow", at = @At("RETURN"), cancellable = true)
    private void simpleinterfaces$all(CallbackInfoReturnable<Integer> ci) {
        ci.setReturnValue(BundleViews.selectable((BundleContents) (Object) this, ci.getReturnValueI()));
    }
}
