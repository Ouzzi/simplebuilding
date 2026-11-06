package com.simplebuildingtools.client.gui;

import com.simplelib.api.client.GadgetHud;
import com.simplebuildingtools.SimpleBuildingTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The octant's HUD box in the look every Simple gadget readout shares: SimpleLib's {@link GadgetHud} (until 07.11.2026
 * SimpleBuilding's HudPanel, of which the octant box was the reference). Stacks with the Gauge and the Resonance Rod.
 */
public final class HudPanel {
    /** The box id (SimpleLib stacking order 0, as SimpleBuilding's octant box). */
    public static final String OCTANT_ID = SimpleBuildingTools.MOD_ID + ":octant";
    public static final int PADDING = GadgetHud.PADDING;
    public static final int TITLE_GAP = GadgetHud.TITLE_GAP;
    public static final int LINE_GAP = GadgetHud.LINE_GAP;
    public static final int COLOR_SECONDARY = GadgetHud.COLOR_SECONDARY;

    public enum Slot {
        OCTANT
    }

    private HudPanel() {
    }

    public static int ink(int rgb) {
        return GadgetHud.ink(rgb);
    }

    public static Component title(ItemStack stack) {
        return GadgetHud.title(stack);
    }

    public static int stackOffset(Slot slot, Minecraft client) {
        return GadgetHud.stackOffset(OCTANT_ID, client);
    }

    public static void background(GuiGraphicsExtractor context, int x, int y, int contentWidth, int contentHeight) {
        GadgetHud.background(context, x, y, contentWidth, contentHeight);
    }
}
