package com.simplebuilding.modules.simpleinterfaces.mixin;

import com.simplebuilding.modules.simpleinterfaces.client.StyledScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * N62/N64 (owner 15.11.): every screen in the Simple style keeps {@code UiBoxes.MARGIN} px round every slot group; where
 * Vanilla's rows are too tight the player's slots move down and the image grows - at every {@code init}, before it centres
 * the screen, computed from the Vanilla image height remembered at the first {@code init} ({@link StyledScreens#applyMargin}
 * is idempotent, so resizing or reopening with the style switched never moves twice). Replaces the former
 * {@code CartographyHeightMixin} (WD25-19), whose extra height the rule now gives.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class StorageMarginMixin {
    @Shadow @Final @Mutable protected int imageHeight;
    @Shadow @Final protected int imageWidth;
    @Shadow protected int titleLabelY;
    @Unique
    private int simpleinterfaces$vanillaHeight = -1;

    @Inject(method = "init", at = @At("HEAD"))
    private void simpleinterfaces$margin(CallbackInfo ci) {
        if (this.simpleinterfaces$vanillaHeight < 0) this.simpleinterfaces$vanillaHeight = this.imageHeight;
        this.imageHeight = StyledScreens.applyMargin((AbstractContainerScreen<?>) (Object) this, this.imageWidth,
                this.simpleinterfaces$vanillaHeight, this.titleLabelY);
    }
}
