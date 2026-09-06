package com.simplebuilding.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Assertions whose failure message says what it means on this Minecraft line.
 *
 * <p>{@code GameTestHelper#assertValueEqual} takes its two values in the opposite order on the
 * two lines this mod builds for, and nothing about the signature says so - both parameters are
 * {@code N}, so the compiler is happy either way and only the failure message gives it away:
 *
 * <ul>
 *   <li>MC 26.2: {@code assertValueEqual(value, expected, name)}, and the message is built as
 *       {@code (name, expected, value)}.</li>
 *   <li>MC 1.21.11: the message is built as {@code (name, first, second)} - so the FIRST
 *       argument is the expected value here.</li>
 * </ul>
 *
 * <p>The message template is the same on both lines, {@code "Expected %s to be %s: was %s"}.
 * The suite is written once and ported, so every call arrives here in 26.2 order - which made
 * all 673 call sites on this line report expected and actual the wrong way round. The check
 * itself was never affected ({@code equals} does not care), but a failing test sent the reader
 * looking for the opposite of the real problem, which is worse than no message at all.
 *
 * <p>So the calls go through here instead, in 26.2 order, and this class turns them around
 * once. If a later Minecraft version changes the order again, this is the only place to touch.
 */
final class Assertions {

    private Assertions() {
    }

    /**
     * Fails unless {@code actual} equals {@code expected}, naming them the right way round.
     *
     * <p>Argument order matches MC 26.2 - actual first - so a test body reads the same on both
     * lines and can be ported without thinking about it.
     */
    static <N> void valueEqual(GameTestHelper helper, N actual, N expected, String valueName) {
        // Reversed on purpose: on this line the first argument is what the message calls the
        // expected value. See the class javadoc.
        helper.assertValueEqual(expected, actual, valueName);
    }
}
