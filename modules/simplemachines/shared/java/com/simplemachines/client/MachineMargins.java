package com.simplemachines.client;

import com.simplelib.api.client.ui.UiFilterButton;
import com.simplelib.api.client.ui.UiMargin;
import com.simplemachines.crafter.AutonomousCrafterMenu;
import com.simplemachines.hopper.TierHopperMenu;
import com.simplemachines.smither.AutoSmitherMenu;
import java.util.List;
import net.minecraft.world.inventory.Slot;

/**
 * N62 (owner 15.11., docs/ai/PRINZIPIEN.md A7 "Container-Margin"): the two boxes of simplemachines' own container screens - 3 px
 * of fill between each box's light inner line and its outermost slots/elements, both boxes shadowed, the player's slots moved
 * down on the client where the rows are too tight ({@link UiMargin}). Pure geometry from the menu (no screen class), so the
 * gametests check the same numbers the screens draw.
 */
public final class MachineMargins {
    /** The machines' arrow (redstone dust / error arrow) between the inputs and the result. */
    public static final int ARROW_X = 99, ARROW_Y = 36, ARROW_WIDTH = 22, ARROW_HEIGHT = 16;

    private MachineMargins() {}

    /** Mod hoppers: the five slots and the filter key (raised, 18 x 18); the box as wide as the inventory, the row stays centred. */
    public static UiMargin.Boxes tierHopper(TierHopperMenu menu) {
        int[] b = UiMargin.grow(UiMargin.bounds(menu.slots), TierHopperMenu.FILTER_BUTTON_X, TierHopperMenu.FILTER_BUTTON_Y,
                UiFilterButton.SIZE, UiFilterButton.SIZE);
        return UiMargin.of(menu.slots, b, 176, TierHopperMenu.IMAGE_HEIGHT, true);
    }

    /** Autonomous Crafter: the 3x3 grid, the big result slot, the arrow and the filter key above it. */
    public static UiMargin.Boxes autonomousCrafter(AutonomousCrafterMenu menu) {
        int[] b = withResult(menu.slots, menu.getSlot(AutonomousCrafterMenu.RESULT_SLOT));
        b = UiMargin.grow(b, AutonomousCrafterMenu.FILTER_BUTTON_X, AutonomousCrafterMenu.FILTER_BUTTON_Y, UiFilterButton.SIZE, UiFilterButton.SIZE);
        return UiMargin.of(menu.slots, b, 176, 166, true);
    }

    /** Auto Smither: the three inputs, the arrow and the big result slot. */
    public static UiMargin.Boxes autoSmither(AutoSmitherMenu menu) {
        return UiMargin.of(menu.slots, withResult(menu.slots, menu.getSlot(AutoSmitherMenu.RESULT_SLOT)), 176, 166, true);
    }

    /** Container slot bounds grown by the arrow and the big result slot (24 x 24 around its item, light edge right/below). */
    private static int[] withResult(List<Slot> slots, Slot result) {
        int[] b = UiMargin.grow(UiMargin.bounds(slots), ARROW_X, ARROW_Y, ARROW_WIDTH, ARROW_HEIGHT);
        return UiMargin.grow(b, result.x - 4, result.y - 4, 25, 25);
    }
}
