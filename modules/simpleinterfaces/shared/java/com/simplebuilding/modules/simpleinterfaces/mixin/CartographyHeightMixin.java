package com.simplebuilding.modules.simpleinterfaces.mixin;

import com.simplebuilding.modules.simpleinterfaces.client.StationScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * WD25-19: the styled cartography table is {@code StationStyles.CARTO_EXTRA} px taller (room above and below the map
 * field like the other tables); set before {@code init} centres the screen, back to Vanilla's 166 without the style.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class CartographyHeightMixin {
    @Shadow @Final @Mutable protected int imageHeight;

    @Inject(method = "init", at = @At("HEAD"))
    private void simpleinterfaces$cartographyHeight(CallbackInfo ci) {
        if ((Object) this instanceof CartographyTableScreen screen) this.imageHeight = StationScreens.cartographyHeight(screen);
    }
}
