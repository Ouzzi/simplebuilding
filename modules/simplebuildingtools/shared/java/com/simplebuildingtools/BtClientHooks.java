package com.simplebuildingtools;

import java.util.function.BooleanSupplier;

/**
 * Narrow bridge from the shared code to the client without server classes loading client classes: the client sets the hook in
 * its initializer, the server keeps the default.
 */
public final class BtClientHooks {
    private static BooleanSupplier clientThread = () -> false;

    private BtClientHooks() {
    }

    public static void setClientThread(BooleanSupplier supplier) {
        clientThread = supplier != null ? supplier : () -> false;
    }

    /** True on the client (render) thread; on the server and its thread always false. */
    public static boolean onClientThread() {
        return clientThread.getAsBoolean();
    }
}
