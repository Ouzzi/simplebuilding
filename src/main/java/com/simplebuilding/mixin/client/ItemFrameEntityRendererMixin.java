package com.simplebuilding.mixin.client;

import net.minecraft.client.render.entity.ItemFrameEntityRenderer;
import net.minecraft.client.render.entity.state.ItemFrameEntityRenderState;
import net.minecraft.entity.decoration.ItemFrameEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrameEntityRenderer.class)
public class ItemFrameEntityRendererMixin {

    // Wir nutzen jetzt 'updateRenderState' statt 'render'.
    // Hier werden die Daten vom Entity (Server) für den Renderer (Client) vorbereitet.
    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void simplebuilding$forceVisibleIfEmpty(ItemFrameEntity entity, ItemFrameEntityRenderState state, float tickDelta, CallbackInfo ci) {
        // Wenn das Item Frame leer ist, erzwingen wir die Sichtbarkeit im Render-Status.
        // Das bedeutet: Das Entity ist technisch gesehen immer noch "unsichtbar" (Server-Daten),
        // aber der Spieler sieht den Rahmen trotzdem, solange er leer ist.
        if (entity.getHeldItemStack().isEmpty()) {
            state.invisible = false;
        }
    }
}