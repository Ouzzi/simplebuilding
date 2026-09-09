package com.simplebuilding.neoforge.clienttest;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Keyboard and mouse for the NeoForge client tests.
 *
 * <p>Fabric drives input by calling {@code KeyboardHandler.onKey} through an accessor mixin, which
 * walks the whole vanilla path including open screens. Nothing like that is available here without
 * writing the same mixin, so this uses the public key mapping API instead:
 * {@link KeyMapping#set} and {@link KeyMapping#click} feed the binding layer, which is what the
 * game reads for movement, jumping, attacking, using and the hotbar.
 *
 * <p>The wheel and the cursor are not bindings, so those two go through an accessor onto vanilla's
 * private {@code MouseHandler} callbacks - the same route Fabric's client test API takes. Calling
 * the callbacks rather than setting fields matters: they are what GLFW calls, so an open screen,
 * which reads events and not bindings, sees them too.
 *
 * <p><b>What this still does not reach:</b> keyboard input inside a screen. A screen reads key
 * events; {@link KeyMapping#set} only moves the binding layer. A test that types into a text field
 * needs the same accessor treatment for {@code KeyboardHandler.onKey}, and it should be added the
 * day such a test is shared - not guessed at now.
 */
final class Input {

    private Input() {
    }

    static void holdKey(int glfwKeyCode) {
        KeyMapping.set(InputConstants.Type.KEYSYM.getOrCreate(glfwKeyCode), true);
    }

    static void releaseKey(int glfwKeyCode) {
        KeyMapping.set(InputConstants.Type.KEYSYM.getOrCreate(glfwKeyCode), false);
    }

    static void clickKey(int glfwKeyCode) {
        KeyMapping.click(InputConstants.Type.KEYSYM.getOrCreate(glfwKeyCode));
    }

    static void holdMouse(int button) {
        KeyMapping.set(InputConstants.Type.MOUSE.getOrCreate(button), true);
    }

    static void releaseMouse(int button) {
        KeyMapping.set(InputConstants.Type.MOUSE.getOrCreate(button), false);
    }

    static void clickMouse(int button) {
        KeyMapping.click(InputConstants.Type.MOUSE.getOrCreate(button));
    }

    static void scroll(double amount) {
        Minecraft client = Minecraft.getInstance();
        ((MouseHandlerAccessor) client.mouseHandler)
                .simplebuilding$onScroll(client.getWindow().handle(), 0.0, amount);
    }

    static void setCursorPos(double x, double y) {
        Minecraft client = Minecraft.getInstance();
        // onMove wants window pixels; the scripts speak in scaled screen coordinates, which is
        // what a screen's own click handling uses. The scale factor converts between them.
        double scale = client.getWindow().getGuiScale();
        ((MouseHandlerAccessor) client.mouseHandler)
                .simplebuilding$onMove(client.getWindow().handle(), x * scale, y * scale);
    }
}
