package com.simplebuilding.mixin.client;

import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrameRenderer.class)
public class ItemFrameEntityRendererMixin {

    // Wir nutzen jetzt 'updateRenderState' statt 'render'.
    // Hier werden die Daten vom Entity (Server) für den Renderer (Client) vorbereitet.
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void simplebuilding$forceVisibleIfEmpty(ItemFrame entity, ItemFrameRenderState state, float tickDelta, CallbackInfo ci) {
        // Wenn das Item Frame leer ist, erzwingen wir die Sichtbarkeit im Render-Status.
        // Das bedeutet: Das Entity ist technisch gesehen immer noch "unsichtbar" (Server-Daten),
        // aber der Spieler sieht den Rahmen trotzdem, solange er leer ist.
        if (entity.getItem().isEmpty()) {
            state.isInvisible = false;
        }
    }
}