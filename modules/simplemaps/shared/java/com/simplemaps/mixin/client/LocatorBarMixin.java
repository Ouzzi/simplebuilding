package com.simplemaps.mixin.client;

import com.simplemaps.client.MapsClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import net.minecraft.client.gui.contextualbar.LocatorBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** F7: the held wayfinder map's waypoints as dots (colour) or heads next to the player indicators. */
@Mixin(LocatorBar.class)
public abstract class LocatorBarMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void simplemaps$drawMapWaypoints(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        MapsClient.drawLocator(graphics, delta, ((ContextualBar) this).top(Minecraft.getInstance().getWindow()));
    }
}
