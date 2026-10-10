package com.simplebuilding.modules.simpleinterfaces.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.simplebuilding.modules.simpleinterfaces.client.OfferChoice;
import com.simplebuilding.modules.simpleinterfaces.client.StationScreens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The offer buttons of the villager screen ({@code MerchantScreen$TradeOfferButton}, a private {@link Button.Plain})
 * in the Simple style: a raised tile in the box's colours instead of the Vanilla button sprite, sunk for the chosen
 * offer. Every other plain button is untouched.
 */
@Mixin(Button.Plain.class)
public abstract class TradeOfferButtonMixin {
    @WrapWithCondition(method = "extractContents", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/Button$Plain;extractDefaultSprite(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
    private boolean simpleinterfaces$offerTile(Button.Plain button, GuiGraphicsExtractor graphics) {
        if (!button.getClass().getName().equals("net.minecraft.client.gui.screens.inventory.MerchantScreen$TradeOfferButton")
                || !(Minecraft.getInstance().gui.screen() instanceof MerchantScreen screen) || !StationScreens.active(screen)) {
            return true;
        }
        StationScreens.offer(screen, graphics, button.getX(), button.getY(), button.getWidth(), button.getHeight(),
                ((OfferChoice) screen).simpleinterfaces$chosen(button.getY()), button.isHoveredOrFocused());
        return false;
    }
}
