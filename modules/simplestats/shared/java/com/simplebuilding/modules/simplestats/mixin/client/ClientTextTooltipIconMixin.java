package com.simplebuilding.modules.simplestats.mixin.client;

import com.simplebuilding.modules.simplestats.client.StatsIconText;
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

/** N78: item icons in statistics tooltip lines ({@link StatsIconText}); every loader draws item tooltip text through this Vanilla class. */
@Mixin(ClientTextTooltip.class)
public abstract class ClientTextTooltipIconMixin {
    @Shadow @Final private FormattedCharSequence text;

    @Inject(method = "extractText", at = @At("TAIL"))
    private void simplestats$icons(GuiGraphicsExtractor graphics, Font font, int x, int y, CallbackInfo ci) {
        StatsIconText.paint(graphics, font, text, x, y);
    }
}
