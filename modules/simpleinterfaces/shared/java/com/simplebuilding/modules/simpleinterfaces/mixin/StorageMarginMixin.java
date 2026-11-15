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
 * N62 (owner 15.11.): the storage screens in the Simple style keep {@code UiBoxes.MARGIN} px round every slot group; where
 * Vanilla's rows are too tight the player's slots move down and the image grows - once, before the first {@code init}
 * centres the screen (pattern of {@link CartographyHeightMixin}). Resizing re-runs {@code init}, not the move. Style
 * switched off while the screen is open: the moved slots stay until it is reopened.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class StorageMarginMixin {
    @Shadow @Final @Mutable protected int imageHeight;
    @Shadow @Final protected int imageWidth;
    @Shadow protected int titleLabelY;
    @Unique
    private boolean simpleinterfaces$margin;

    @Inject(method = "init", at = @At("HEAD"))
    private void simpleinterfaces$margin(CallbackInfo ci) {
        if (this.simpleinterfaces$margin) return;
        this.simpleinterfaces$margin = true;
        this.imageHeight = StyledScreens.applyMargin((AbstractContainerScreen<?>) (Object) this, this.imageWidth, this.imageHeight, this.titleLabelY);
    }
}
