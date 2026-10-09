package com.simplelib.api.client.ui;

/** Client-wide comparison switch shared by SimpleLib-based screen styles. */
public final class UiStyleToggle {
    private static boolean enabled = true;

    private UiStyleToggle() {}

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean toggle() {
        enabled = !enabled;
        return enabled;
    }
}
