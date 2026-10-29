package com.simplebuilding.modules.simplechallenges.preset;

import java.util.Locale;

/** What unlocking a cell costs (R4: items are the default). */
public enum Currency {
    ITEMS, CREDITS, XP;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Currency parse(String s) {
        for (Currency c : values()) if (c.id().equals(s)) return c;
        return null;
    }
}
