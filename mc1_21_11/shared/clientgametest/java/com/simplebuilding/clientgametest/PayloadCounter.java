package com.simplebuilding.clientgametest;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Counts how often a server side payload handler ran.
 *
 * <p>The test that needs this is about bandwidth: the client is supposed to send a
 * {@code SpaceKeyPayload} only when the jump key CHANGES state, and every test before this one
 * observed the server side flag alone - which reads exactly the same whether the client sent two
 * packets per press or twenty per second. The count is the only thing that separates those.
 *
 * <p><b>The mixin that feeds this is not here and cannot be</b>, for the same reason
 * {@link BreakingStateRecorder} says: each loader's test source set carries its own mixin config.
 * Both inject at the head of {@code ModMessageHandlers.handleSpaceKey} and call
 * {@link #spaceKeyArrived()}; what is shared is the count and the two readers. The integrated
 * server runs in the test client's JVM, so a static is enough and no round trip is needed.
 *
 * <p>Atomic because the handler runs on the server thread and the test reads on the client
 * thread; a plain int would be visible late and read one press behind.
 */
public final class PayloadCounter {

    private static final AtomicInteger SPACE_KEY = new AtomicInteger();

    private PayloadCounter() {
    }

    /** Called by the loader specific mixin every time the server handles a space key payload. */
    public static void spaceKeyArrived() {
        SPACE_KEY.incrementAndGet();
    }

    /** How many space key payloads the server has handled since the client started. */
    public static int spaceKeyPayloads() {
        return SPACE_KEY.get();
    }
}
