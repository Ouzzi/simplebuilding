package com.simplemaps.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simplemaps.client.MapsClient;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** F7: a wayfinder map with waypoints in hand shows the locator bar even without Vanilla waypoints. */
@Mixin(Hud.class)
public abstract class HudMixin {
    @ModifyExpressionValue(method = "nextContextualInfoState",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/waypoints/ClientWaypointManager;hasWaypoints()Z"))
    private boolean simplemaps$mapWaypoints(boolean original) {
        return original || MapsClient.hasLocatorMarks();
    }
}
