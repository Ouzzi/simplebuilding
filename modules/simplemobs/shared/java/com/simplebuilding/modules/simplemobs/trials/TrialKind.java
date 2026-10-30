package com.simplebuilding.modules.simplemobs.trials;

import net.minecraft.util.StringRepresentable;

/** The three tiers of the trial chain: the Weathered Trial (Vanilla-like), the Evil Trial (Evil Omen) and the Void Trial (Void Omen, config). */
public enum TrialKind implements StringRepresentable {
    WEATHERED("weathered"), EVIL("evil"), VOID("void");

    private final String id;

    TrialKind(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public String id() {
        return id;
    }
}
