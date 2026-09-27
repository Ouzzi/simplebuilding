package com.simplebuilding.tweaks;

import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

/**
 * Schmale Bruecke vom gemeinsamen Code zum Client, ohne dass Server-Klassen Client-Klassen laden:
 * der Client setzt die Haken in seinem Initialisierer, auf dem Server bleiben die Standardwerte.
 */
public final class TweaksClientHooks {
    private static Supplier<@Nullable UUID> localPlayer = () -> null;
    private static BooleanSupplier clientThread = () -> false;

    private TweaksClientHooks() {
    }

    public static void setLocalPlayer(Supplier<@Nullable UUID> supplier) {
        localPlayer = supplier != null ? supplier : () -> null;
    }

    public static void setClientThread(BooleanSupplier supplier) {
        clientThread = supplier != null ? supplier : () -> false;
    }

    /** true auf dem Client-(Render-)Thread; auf dem Server und dessen Thread immer false. */
    public static boolean onClientThread() {
        return clientThread.getAsBoolean();
    }

    /** UUID des Spielers am eigenen Client, sonst null (Server, Hauptmenue). */
    public static @Nullable UUID localPlayer() {
        return localPlayer.get();
    }
}
