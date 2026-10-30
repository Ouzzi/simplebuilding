package com.simplebuilding.modules.simplechallenges.preset;

import java.util.Locale;

/** Price tiers (concept 3): easy = what the start area offers, medium = what the cell's own biome offers, hard = progression ladder. */
public enum Difficulty {
    EASY, MEDIUM, HARD;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Difficulty parse(String s) {
        for (Difficulty d : values()) if (d.id().equals(s)) return d;
        return null;
    }
}
