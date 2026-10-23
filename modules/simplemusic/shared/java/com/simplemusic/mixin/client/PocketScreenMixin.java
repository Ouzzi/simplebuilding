package com.simplemusic.mixin.client;

import com.simplemusic.client.PocketMouseActions;
import net.minecraft.client.gui.ItemSlotMouseAction;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The mouse wheel over a Pocket Jukebox in a slot selects a disc, like Vanilla's bundle (PJ23-1). */
@Mixin(AbstractContainerScreen.class)
public abstract class PocketScreenMixin extends Screen {
    protected PocketScreenMixin(Component title) {
        super(title);
    }

    @Shadow
    protected abstract void addItemSlotMouseAction(ItemSlotMouseAction handler);

    @Inject(method = "init", at = @At("TAIL"))
    private void simplemusic$addPocketScroll(CallbackInfo ci) {
        if (this.minecraft != null) this.addItemSlotMouseAction(new PocketMouseActions(this.minecraft));
    }
}
