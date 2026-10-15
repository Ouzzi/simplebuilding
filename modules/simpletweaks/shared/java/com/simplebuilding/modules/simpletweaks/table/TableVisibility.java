package com.simplebuilding.modules.simpletweaks.table;

import java.util.Locale;

/**
 * Server config {@code claimVisibility}: what the claim table shows a viewer. Pure rules, decided on the server only.
 * An operator always sees everything. Member lists and codes of other people's land are never sent in any mode.
 */
public enum TableVisibility {
    ALL("all"), OWNERS_HIDDEN("owners_hidden"), OWN("own"), OFF("off");

    private final String key;
    TableVisibility(String key) { this.key = key; }
    public String key() { return key; }

    /** Parses the config value; null for anything unknown (the config then refuses to load). */
    public static TableVisibility parse(String text) {
        if (text == null) return null;
        for (var v : values()) if (v.key.equals(text.toLowerCase(Locale.ROOT))) return v;
        return null;
    }
    public static TableVisibility byOrdinal(int ordinal) { return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : OFF; }

    /** Whether a claim shows on the map. {@code involved}: the viewer owns it or holds any right in it. */
    public boolean shows(boolean operator, boolean involved) {
        if (operator) return true;
        return switch (this) { case ALL, OWNERS_HIDDEN -> true; case OWN -> involved; case OFF -> false; };
    }
    /** Whether the owner's name is sent for a claim that shows. */
    public boolean showsName(boolean operator, boolean involved) {
        if (operator) return true;
        return switch (this) { case ALL, OWN -> true; case OWNERS_HIDDEN -> involved; case OFF -> false; };
    }
}
