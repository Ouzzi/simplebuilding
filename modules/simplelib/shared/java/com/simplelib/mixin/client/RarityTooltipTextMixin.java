package com.simplelib.mixin.client;

import com.simplelib.client.RarityText;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tooltip lines (every loader draws item tooltips through this Vanilla class): after the line is drawn, letters of
 * cursed/mythic names are painted again pixel row by pixel row ({@link RarityText}, owner 21.10., queue b-rarity21b).
 */
@Mixin(ClientTextTooltip.class)
public abstract class RarityTooltipTextMixin {
    @Shadow @Final private FormattedCharSequence text;

    @Inject(method = "extractText", at = @At("TAIL"))
    private void simplelib$rarityRows(GuiGraphicsExtractor graphics, Font font, int x, int y, CallbackInfo ci) {
        RarityText.paint(graphics, font, text, x, y, System.currentTimeMillis());
    }
}
