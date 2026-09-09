package com.simplebuilding.neoforge.clienttest;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

/**
 * Keyboard and mouse for the NeoForge client tests.
 *
 * <p>Fabric drives input by calling {@code KeyboardHandler.onKey} through an accessor mixin, which
 * walks the whole vanilla path including open screens. Nothing like that is available here without
 * writing the same mixin, so this uses the public key mapping API instead:
 * {@link KeyMapping#set} and {@link KeyMapping#click} feed the binding layer, which is what the
 * game reads for movement, jumping, attacking, using and the hotbar.
 *
 * <p><b>What that does not reach:</b> anything a <em>screen</em> handles, because a screen reads
 * key events, not bindings. A test that types into a text field or clicks a button in a menu needs
 * the real event path, and the methods that would carry it say so loudly rather than doing
 * something almost right - a silently ineffective key press is the kind of thing that leaves a
 * client test green while proving nothing, which is the failure this whole suite is being rebuilt
 * to avoid.
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
        throw new UnsupportedOperationException(
                "Scrolling is not wired up on the NeoForge client tests yet. The binding layer has "
                        + "no scroll concept - vanilla handles the wheel in MouseHandler.onScroll, "
                        + "which is private, so this needs an accessor mixin in this source set. "
                        + "Add it together with the first shared test that scrolls, and give it a "
                        + "screenshot that proves the wheel arrived.");
    }

    static void setCursorPos(double x, double y) {
        throw new UnsupportedOperationException(
                "Moving the cursor is not wired up on the NeoForge client tests yet. It goes "
                        + "through MouseHandler.onMove, which is private, so this needs an "
                        + "accessor mixin in this source set. Add it together with the first "
                        + "shared test that needs a cursor position.");
    }
}
