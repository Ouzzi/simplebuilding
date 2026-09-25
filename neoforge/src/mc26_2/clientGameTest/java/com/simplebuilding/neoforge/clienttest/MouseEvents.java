package com.simplebuilding.neoforge.clienttest;

import com.simplebuilding.neoforge.clienttest.mixin.MouseHandlerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonInfo;

/**
 * Vanilla's mouse callbacks for the NeoForge client test driver, MC 26.2 side (twin in
 * mc26_3/neoforge/src/clientGameTest/java). 26.3 changed the onMove signature (SDL adds the relative
 * motion), so {@link Input} goes through this instead of the accessor.
 */
final class MouseEvents {

    private MouseEvents() {
    }

    static void button(Minecraft client, MouseButtonInfo button, int action) {
        ((MouseHandlerAccessor) client.mouseHandler).simplebuilding$onButton(client.getWindow().handle(), button, action);
    }

    static void scroll(Minecraft client, double amount) {
        ((MouseHandlerAccessor) client.mouseHandler).simplebuilding$onScroll(client.getWindow().handle(), 0.0, amount);
    }

    static void move(Minecraft client, double x, double y) {
        ((MouseHandlerAccessor) client.mouseHandler).simplebuilding$onMove(client.getWindow().handle(), x, y);
    }
}
