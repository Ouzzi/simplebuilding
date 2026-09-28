package com.simplebuilding.tweaks.block.entity;

/**
 * Block-Entity eines Pads, das ein Komparator-Signal liefert (Besitzer 2026-09-28). Je Familie, was
 * sinnvoll ist: Launchpad = Fuellstand der Ladungen, Trank-Pad = bereit (15) bzw. wie weit die
 * Abklingzeit ist, Flypad/Elytra-Pad = Zahl der versorgten Spieler, Spawn-Teleporter = Fortschritt
 * der Wartezeit. Gelesen von {@code PadBlock#getAnalogOutputSignal}.
 */
public interface PadSignalSource {
    /** Signalstaerke 0..15 fuer einen Komparator. */
    int comparatorSignal();

    /** Signal fuer einen Fuellstand {@code amount} von {@code max}: leer 0, sonst 1..15 wie bei Vanillas Behaeltern. */
    static int fillSignal(int amount, int max) {
        if (amount <= 0 || max <= 0) {
            return 0;
        }
        return Math.min(15, 1 + (int) (14L * Math.min(amount, max) / max));
    }
}
