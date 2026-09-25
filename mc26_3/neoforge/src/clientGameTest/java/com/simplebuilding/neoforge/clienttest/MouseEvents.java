package com.simplebuilding.neoforge.clienttest;

import com.simplebuilding.neoforge.clienttest.mixin.MouseHandlerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonInfo;

/** Vanilla's mouse callbacks for the NeoForge client test driver, MC 26.3 side (see the 26.2 twin). */
final class MouseEvents {

    private MouseEvents() {
    }

    static void button(Minecraft client, MouseButtonInfo button, int action) {
        ((MouseHandlerAccessor) client.mouseHandler).simplebuilding$onButton(client.getWindow().handle(), button, action);
    }

    static void scroll(Minecraft client, double amount) {
        ((MouseHandlerAccessor) client.mouseHandler).simplebuilding$onScroll(client.getWindow().handle(), 0.0, amount);
    }

    /** SDL also reports the relative motion; for a jump to (x, y) that is the distance travelled. */
    static void move(Minecraft client, double x, double y) {
        double xRel = x - client.mouseHandler.xpos();
        double yRel = y - client.mouseHandler.ypos();
        ((MouseHandlerAccessor) client.mouseHandler).simplebuilding$onMove(client.getWindow().handle(), x, y, xRel, yRel);
    }
}
