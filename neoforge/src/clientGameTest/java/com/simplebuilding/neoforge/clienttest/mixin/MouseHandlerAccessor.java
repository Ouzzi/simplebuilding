package com.simplebuilding.neoforge.clienttest.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Reaches vanilla's two private mouse callbacks so a client test can turn the wheel and move the
 * cursor.
 *
 * <p>Everything else the tests need goes through the key mapping layer, which is public. These two
 * do not exist there: a scroll is not a binding, and neither is a cursor position. Fabric's client
 * test API solves it with the same kind of accessor; this is the NeoForge half.
 *
 * <p>It lives in its own {@code .mixin} sub package on purpose. A mixin config claims a whole
 * package, and every class under it becomes mixin-owned - which means it can no longer be loaded
 * directly. Pointing the config at {@code ...clienttest} took the test mod's own entry point down
 * with it and the game refused to start with "is in a defined mixin package and cannot be
 * referenced directly".
 *
 * <p>Calling the callbacks rather than setting fields is deliberate. They are what GLFW calls, so
 * a test that goes through them exercises the same path a player does - including the open screen,
 * which reads events and not bindings.
 */
@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {

    @Invoker("onScroll")
    void simplebuilding$onScroll(long windowHandle, double xOffset, double yOffset);

    @Invoker("onMove")
    void simplebuilding$onMove(long windowHandle, double x, double y);
}
