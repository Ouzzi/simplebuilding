package com.simplebuilding.client;

/**
 * Client side of {@code util/LibServices} (split stage 8c-2). 26.2: no SimpleLib, the backpacks are SimpleBuilding's own
 * and draw the extra head slot themselves - nothing to register. The 26.3 twin lives in mc26_3/overlay.
 */
public final class LibClientServices {
    private LibClientServices() {
    }

    /** From each loader's client entry. */
    public static void init() {
    }

    /** Shift held (26.3: SimpleLib {@code UiShiftTooltip.held}). */
    public static boolean shiftHeld() {
        return net.minecraft.client.Minecraft.getInstance().hasShiftDown();
    }

    /** 26.3 folds long tooltips behind "Hold Shift for more" (SimpleLib); 26.2 keeps every line until the port run. */
    public static java.util.List<net.minecraft.network.chat.Component> moreOnShift(java.util.List<net.minecraft.network.chat.Component> lines,
                                                                                 int brief, boolean shift) {
        return lines;
    }
}
