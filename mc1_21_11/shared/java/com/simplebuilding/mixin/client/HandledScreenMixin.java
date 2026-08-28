package com.simplebuilding.mixin.client;

import com.simplebuilding.client.gui.tooltip.ReinforcedBundleTooltipSubmenuHandler;
import net.minecraft.client.gui.ItemSlotMouseAction;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin extends Screen {

    protected HandledScreenMixin(Component title) {
        super(title);
    }

    // Zugriff auf die geschützte Methode in AbstractContainerScreen, um Handler hinzuzufügen
    @Shadow
    protected abstract void addItemSlotMouseAction(ItemSlotMouseAction handler);

    /**
     * Wir injizieren uns in die 'init' Methode.
     * Dort fügt Vanilla seinen Bundle-Handler hinzu. Wir fügen einfach unseren dazu.
     * Minecraft geht dann beim Scrollen die Liste durch und prüft 'isApplicableTo'.
     */
    @Inject(method = "init", at = @At("TAIL"))
    private void simplebuilding$addCustomBundleHandler(CallbackInfo ci) {
        if (this.minecraft != null) {
            this.addItemSlotMouseAction(new ReinforcedBundleTooltipSubmenuHandler(this.minecraft));
        }
    }
}