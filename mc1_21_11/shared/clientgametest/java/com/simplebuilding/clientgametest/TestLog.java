package com.simplebuilding.clientgametest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The one place a shared client test writes a line to.
 *
 * <p><b>Why both a logger and stdout.</b> Every line goes through the logger, so it lands in the
 * run's {@code latest.log} next to everything else the client says, and straight to stdout with a
 * fixed prefix on top of that. The stdout copy is not redundant: the NeoForge run ends in a hard
 * {@code Runtime.halt} (it cannot disconnect - {@code Minecraft.disconnect} pumps client ticks and
 * re-enters {@code runTick}), and log4j's appenders are never flushed on halt. Without the stdout
 * copy the last measurements of a run - the interesting ones - would be the ones that disappear.
 * The explicit {@code flush} is part of that: a buffered stdout is lost to a halt just as well.
 *
 * <p><b>Why this exists rather than each helper printing.</b> The two loader copies of
 * {@link ScreenshotDiff} disagreed about exactly this and nothing else: Fabric printed to stdout
 * under {@code [simplebuilding-test]}, NeoForge logged through its own {@code Log} under
 * {@code [simplebuilding-clienttest]}. A shared helper has to pick one, and the prefix picked here
 * is NeoForge's, because that is the one the run's own log lines already carry.
 *
 * <p><b>Careful with what you put in a string here.</b> The test runner harvests screenshot names
 * out of these sources by their spelling - a whole string literal of lowercase words joined by
 * hyphens - from every file that mentions a screenshot call at all. A literal of that shape in a
 * helper becomes a checkpoint nobody ever takes, and the run then fails on a missing file that was
 * never promised. The logger id below has exactly that shape; it stays harmless because the runner
 * excludes it by where it stands ({@code getLogger}), not by its spelling. Do not rely on that for
 * anything else.
 */
public final class TestLog {

    /** Prefix on every stdout line, so a test line can be told from the client's own chatter. */
    public static final String PREFIX = "[simplebuilding-clienttest]";

    private static final Logger LOGGER = LoggerFactory.getLogger("simplebuilding-clienttest");

    private TestLog() {
    }

    /** One informational line, to the log and to stdout. */
    public static void info(String message) {
        LOGGER.info("{} {}", PREFIX, message);
        System.out.println(PREFIX + " " + message);
        System.out.flush();
    }

    /** One failure line with its cause, to the log and to stdout. */
    public static void error(String message, Throwable throwable) {
        LOGGER.error("{} {}", PREFIX, message, throwable);
        System.out.println(PREFIX + " " + message);

        if (throwable != null) {
            throwable.printStackTrace(System.out);
        }

        System.out.flush();
    }
}
