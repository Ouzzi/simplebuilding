package com.simplebuildingtools.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every server tick of an open cartography table: the blueprint scan goes on ({@link com.simplebuildingtools.blueprint.CartographyScanTicker}). */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
    @Inject(method = "broadcastChanges", at = @At("HEAD"))
    private void simplebuildingtools$cartographyTick(CallbackInfo ci) {
        if ((Object) this instanceof com.simplebuildingtools.blueprint.CartographyScanTicker ticker) {
            ticker.simplebuildingtools$tickScan();
        }
    }
}
