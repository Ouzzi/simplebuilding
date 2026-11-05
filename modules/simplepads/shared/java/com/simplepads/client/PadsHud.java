package com.simplepads.client;

/** Whether the mod HUD is shown: SimpleLib's shared gadget HUD settings (SimpleBuilding hands in its {@code showModHud}). */
public final class PadsHud {
    private PadsHud() {
    }

    public static boolean visible() {
        return com.simplelib.api.client.GadgetHud.settings().visible();
    }
}
