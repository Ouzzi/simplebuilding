package com.simplebuilding.neoforge.clienttest;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The keys and mouse buttons the test harness is holding down.
 *
 * <p>Setting a key mapping moves the binding layer, which is what the game reads for movement,
 * jumping and attacking. It does <em>not</em> move the window state, and some code asks that
 * directly - {@code InputConstants.isKeyDown} goes straight to {@code glfwGetKey}. The mod's own
 * {@code MouseMixin} uses exactly that call to see whether Control or Alt is held, so a test that
 * only set the binding would scroll without a modifier and then blame the mod for not reacting.
 *
 * <p>That is not a hypothesis: the converted client bootstrap test asserts it before it measures,
 * and it is what caught this - "the harness holds GLFW key 341 but InputConstants.isKeyDown says
 * it is up ... the mod would be blamed for it". Fabric's client gametest framework answers the
 * same question the same way; this is the NeoForge half.
 */
public final class HeldKeys {

    private static final Set<Integer> KEYS = ConcurrentHashMap.newKeySet();

    private HeldKeys() {
    }

    public static void hold(int glfwKeyCode) {
        KEYS.add(glfwKeyCode);
    }

    public static void release(int glfwKeyCode) {
        KEYS.remove(glfwKeyCode);
    }

    /** @return true if the harness is holding this key, so the window would report it down */
    public static boolean isHeld(int glfwKeyCode) {
        return KEYS.contains(glfwKeyCode);
    }

    /** @return true if the harness is holding any key at all - the cheap early out for the mixin */
    public static boolean anyHeld() {
        return !KEYS.isEmpty();
    }
}
