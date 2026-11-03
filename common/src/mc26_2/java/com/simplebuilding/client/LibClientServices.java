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

    /** 26.3 stacks the gadget readouts through SimpleLib; 26.2 keeps SimpleBuilding's own stack ({@code HudPanel}). */
    public static Integer gadgetHudOffset(com.simplebuilding.client.gui.HudPanel.Slot slot, net.minecraft.client.Minecraft client) {
        return null;
    }

    /** 26.3: the Simple Modpack guide's top bookmarks (SimpleLib); 26.2 has no SimpleLib - nothing drawn. */
    public static net.minecraft.network.chat.Component modpackBar(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int bx, int by,
                                                                  int mouseX, int mouseY) {
        return null;
    }

    public static boolean modpackBarClick(double mouseX, double mouseY, net.minecraft.world.InteractionHand hand) {
        return false;
    }

    public static boolean modpackMore() {
        return false;
    }

    public static void openModpackMore(net.minecraft.world.InteractionHand hand) {
    }

    public static void reportModpackMore(int[] rect) {
    }

    public static net.minecraft.world.item.ItemStack modpackIcon() {
        return net.minecraft.world.item.ItemStack.EMPTY;
    }

    public static int modpackBookY(int height) {
        return Math.max(0, (height - 180) / 2);
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
