package com.simpleresonance.client;

import com.simpleresonance.config.ResonanceConfig;
import com.simpleresonance.net.ResonanceStatsPayload;

/**
 * Client store of the last {@code simpleresonance:stats} payload (MOB27-3 R2). No client class is used here, so the server side of
 * {@link com.simpleresonance.ResonanceBridge} may name it. Cleared on disconnect, so another server never shows the old numbers.
 */
public final class ResonanceClient {
    private static volatile ResonanceStatsPayload last;

    private ResonanceClient() {}

    /** From the loader's payload receiver (client thread). */
    public static void receive(ResonanceStatsPayload payload) {
        last = payload;
    }

    /** On disconnect. */
    public static void clear() {
        last = null;
    }

    /** The last stats; before the first payload a fresh player with the local base. */
    public static ResonanceStatsPayload stats() {
        ResonanceStatsPayload s = last;
        return s != null ? s : new ResonanceStatsPayload(0, 0, 0, 0, 0, 0, ResonanceConfig.trimBenefitBaseMultiplier);
    }

    /** Whether the server sent anything yet. */
    public static boolean synced() {
        return last != null;
    }
}
