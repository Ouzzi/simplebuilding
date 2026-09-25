package com.simplebuilding.tweaks.client;

/** Traegt den XP-Wert einer Kugel im Render-State (Simple Tweaks: IOrbValue). */
public interface OrbValueHolder {
    int simplebuilding$getOrbValue();

    void simplebuilding$setOrbValue(int value);

    /** Groesse nach Wert: 1 + Wert/500, hoechstens dreifach (Simple Tweaks, Config scaleXpOrbs). */
    static float scaleFor(int value) {
        return Math.min(3.0f, 1.0f + value / 500.0f);
    }
}
