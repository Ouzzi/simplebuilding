package com.simplebuilding.modules.simpleinterfaces.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simplebuilding.modules.simpleinterfaces.client.BundleViews;
import com.simplebuilding.modules.simpleinterfaces.tooltip.BundleView;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.BundleContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client: Vanilla's bundle tooltip shows the page of {@link BundleView} instead of its first eleven/twelve entries.
 * Vanilla still draws everything (its sprites, the "+n" cell, the selected name, the bar), so SimpleBuilding's tint
 * and bar mixins on the same class keep working. Without a view (switch off, <= 12 entries) nothing changes.
 */
@Mixin(ClientBundleTooltip.class)
public abstract class BundleTooltipViewMixin {
    @Shadow @Final private BundleContents contents;

    @Inject(method = "slotCount", at = @At("HEAD"), cancellable = true)
    private void simpleinterfaces$cells(CallbackInfoReturnable<Integer> ci) {
        BundleView view = BundleViews.of(contents);
        if (view != null) ci.setReturnValue(view.cells());
    }

    /** {@code contents.size() > 12}: overflowing only when entries are off the page. */
    @ModifyConstant(method = "extractBundleWithItemsTooltip", constant = @Constant(intValue = 12))
    private int simpleinterfaces$overflow(int vanilla) {
        BundleView view = BundleViews.of(contents);
        return view == null ? vanilla : view.overflow() ? 0 : Integer.MAX_VALUE;
    }

    @Inject(method = "getShownItems", at = @At("HEAD"), cancellable = true)
    private void simpleinterfaces$page(int amount, CallbackInfoReturnable<List<ItemStackTemplate>> ci) {
        BundleView view = BundleViews.of(contents);
        if (view != null) ci.setReturnValue(contents.items().subList(view.start(), view.start() + view.shown()));
    }

    @Inject(method = "getAmountOfHiddenItems", at = @At("HEAD"), cancellable = true)
    private void simpleinterfaces$hidden(List<ItemStackTemplate> shown, CallbackInfoReturnable<Integer> ci) {
        BundleView view = BundleViews.of(contents);
        if (view != null) ci.setReturnValue(BundleView.hidden(contents, view));
    }

    /** The highlight compares the position on the page with the selected index: shift it onto the page. */
    @WrapOperation(method = "extractSlot", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/component/BundleContents;getSelectedItemIndex()I"))
    private int simpleinterfaces$selectedOnPage(BundleContents self, Operation<Integer> original) {
        int selected = original.call(self);
        BundleView view = BundleViews.of(self);
        return view == null || selected < 0 ? selected : selected - view.start();
    }
}
