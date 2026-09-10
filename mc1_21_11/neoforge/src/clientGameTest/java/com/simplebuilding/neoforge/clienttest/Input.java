package com.simplebuilding.neoforge.clienttest;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import com.simplebuilding.neoforge.clienttest.mixin.MouseHandlerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonInfo;

/**
 * Keyboard and mouse for the NeoForge client tests.
 *
 * <p><b>The mouse goes through vanilla's own GLFW callbacks</b> - {@code MouseHandler.onButton},
 * {@code onScroll}, {@code onMove} - reached through an accessor mixin. That is the same route
 * Fabric's client test API takes, and it is the only one that is complete: {@code onButton} calls
 * {@code KeyMapping.set} and {@code KeyMapping.click} itself when no screen is open, and hands the
 * event to the screen when one is. Feeding the binding layer as well is therefore not belt and
 * braces but a double click, and pointing the cursor through anything other than {@code onMove}
 * leaves an open screen unaware that it moved at all.
 *
 * <p><b>The keyboard goes through the public key mapping API</b> instead: {@link KeyMapping#set}
 * and {@link KeyMapping#click} feed the binding layer, which is what the game reads for movement,
 * jumping, attacking, using and the hotbar. {@link HeldKeys} additionally makes a held key visible
 * to {@code InputConstants.isKeyDown}, which reads the window rather than the binding and which
 * the mod's own {@code MouseMixin} asks for Control and Alt.
 *
 * <p><b>What this still does not reach:</b> keyboard input inside a screen. A screen reads key
 * events; {@link KeyMapping#set} only moves the binding layer. A test that types into a text field
 * needs the same accessor treatment for {@code KeyboardHandler.onKey} - the mouse half of that
 * argument has since been paid for in failing tests, so the day such a test is shared this is the
 * first thing to do, not a guess.
 */
final class Input {

    /** GLFW's own action codes; the callback compares against these numbers. */
    private static final int GLFW_RELEASE = 0;
    private static final int GLFW_PRESS = 1;

    private Input() {
    }

    static void holdKey(int glfwKeyCode) {
        KeyMapping.set(InputConstants.Type.KEYSYM.getOrCreate(glfwKeyCode), true);
        // Also make the window state agree: InputConstants.isKeyDown goes straight to glfwGetKey,
        // and the mod's MouseMixin asks it for Control and Alt.
        HeldKeys.hold(glfwKeyCode);
    }

    static void releaseKey(int glfwKeyCode) {
        KeyMapping.set(InputConstants.Type.KEYSYM.getOrCreate(glfwKeyCode), false);
        HeldKeys.release(glfwKeyCode);
    }

    static void clickKey(int glfwKeyCode) {
        KeyMapping.click(InputConstants.Type.KEYSYM.getOrCreate(glfwKeyCode));
    }

    static void holdMouse(int button) {
        onButton(button, GLFW_PRESS);
    }

    static void releaseMouse(int button) {
        onButton(button, GLFW_RELEASE);
    }

    static void clickMouse(int button) {
        onButton(button, GLFW_PRESS);
        onButton(button, GLFW_RELEASE);
    }

    /**
     * A mouse button, delivered the way GLFW delivers one.
     *
     * <p>Deliberately NOT accompanied by {@code KeyMapping.set} / {@code KeyMapping.click}:
     * {@code MouseHandler.onButton} does exactly those two itself, and only when no screen is
     * open. Doing both means the world sees every click twice - which is not a theory either. It
     * cost the item frame case, where one right click locked the frame and the duplicate
     * immediately unlocked it again, and the mining cases behind it.
     *
     * <p>This is also the route Fabric's client test API takes for mouse buttons
     * ({@code TestInputImpl.pressOrReleaseKey} -> {@code MouseHandler.onButton}), and it is the
     * only one a SCREEN can see: a screen reads events, and the binding layer is invisible to it.
     */
    private static void onButton(int button, int action) {
        Minecraft client = Minecraft.getInstance();
        ((MouseHandlerAccessor) client.mouseHandler).simplebuilding$onButton(
                client.getWindow().handle(), new MouseButtonInfo(button, 0), action);
    }

    static void scroll(double amount) {
        Minecraft client = Minecraft.getInstance();
        ((MouseHandlerAccessor) client.mouseHandler)
                .simplebuilding$onScroll(client.getWindow().handle(), 0.0, amount);
    }

    static void setCursorPos(double x, double y) {
        Minecraft client = Minecraft.getInstance();
        // Raw window pixels, handed on unchanged. Two earlier versions scaled here - once by
        // getGuiScale(), once by the exact inverse of vanilla's own conversion - and both were
        // wrong for the same reason: the caller has already converted. HudAndTooltipClientTest's
        // moveCursorToGui does the GUI-to-window arithmetic itself, because that has to happen on
        // the client thread while the harness call does not. Scaling again put the cursor off the
        // inventory entirely and no slot ever reported itself hovered.
        ((MouseHandlerAccessor) client.mouseHandler)
                .simplebuilding$onMove(client.getWindow().handle(), x, y);
    }
}
