package com.simplestorage.mixin.client;

import com.simplestorage.client.BundleScrollHandler;
import net.minecraft.client.gui.ItemSlotMouseAction;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The mouse wheel over a mod bundle in a slot selects an entry, like Vanilla's bundle (split stage 8c-1, from SimpleBuilding's {@code HandledScreenMixin}). */
@Mixin(AbstractContainerScreen.class)
public abstract class BundleScreenMixin extends Screen {
    protected BundleScreenMixin(Component title) {
        super(title);
    }

    @Shadow
    protected abstract void addItemSlotMouseAction(ItemSlotMouseAction handler);

    @Inject(method = "init", at = @At("TAIL"))
    private void simplestorage$addBundleScroll(CallbackInfo ci) {
        if (this.minecraft != null) this.addItemSlotMouseAction(new BundleScrollHandler(this.minecraft));
    }
}
