package com.simpleblocks.saw;

/**
 * Text colours of the Shapecutter screen: it draws Vanilla's stonecutter background (referenced, see ShapecutterScreen), so
 * the title and inventory label are Vanilla's 404040 on Vanilla's container grey C6C6C6 (SimpleLib's {@code UiPalette.INVENTORY};
 * copied as constants so the test needs no client class). The GameTest {@code saw_ui_contrast} checks them (>= 4.5:1).
 */
public final class SawPalette {
    public static final int FILL = 0xFFC6C6C6;
    public static final int LABEL = 0xFF404040;
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
