package com.simplebuilding.neoforge.clienttest.mixin;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * MC 26.3 twin of neoforge/src/mc26_2/clientGameTest/java/.../MouseHandlerAccessor.java (listed in
 * the shared simplebuildingclienttest.mixins.json, so both lines need the class). 26.3's SDL mouse
 * callbacks are public and onMove carries the relative motion as well; the invokers follow the 26.3
 * signatures, and {@code MouseEvents} hides the difference from the test driver.
 */
@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {

    @Invoker("onScroll")
    void simplebuilding$onScroll(long windowHandle, double xOffset, double yOffset);

    @Invoker("onMove")
    void simplebuilding$onMove(long windowHandle, double x, double y, double xRel, double yRel);

    @Invoker("onButton")
    void simplebuilding$onButton(long windowHandle, MouseButtonInfo button, int action);
}
