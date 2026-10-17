package com.simpleblocks.saw;

/**
 * Colours of the saw screen: SimpleLib's {@code UiPalette.INVENTORY} (the Vanilla inventory grey), copied as constants
 * because Simple Blocks runs without SimpleLib. Nothing new is invented (AGENTS.md rule 15). The GameTest
 * {@code saw_ui_contrast} checks label and search text against their backgrounds (>= 4.5:1).
 */
public final class SawPalette {
    public static final int FILL = 0xFFC6C6C6;
    public static final int LIGHT = 0xFFFFFFFF;
    public static final int SHADOW = 0xFF555555;
    public static final int SLOT = 0xFF8B8B8B;
    public static final int DARK = 0xFF373737;
    public static final int LABEL = 0xFF404040;
    public static final int BORDER = 0xFF000000;
    /** Search field: Vanilla's white EditBox text on its black box. */
    public static final int FIELD = 0xFF000000;
    public static final int FIELD_TEXT = 0xFFE0E0E0;
    public static final double MIN_CONTRAST = 4.5;

    private SawPalette() {}

    /** WCAG contrast ratio of two opaque ARGB colours. */
    public static double contrast(int a, int b) {
        double la = luminance(a), lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminance(int argb) {
        return 0.2126 * channel(argb >> 16) + 0.7152 * channel(argb >> 8) + 0.0722 * channel(argb);
    }

    private static double channel(int value) {
        double c = (value & 0xFF) / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}
