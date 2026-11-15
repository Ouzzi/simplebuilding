package com.simplemachines.test;

import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiMargin;
import com.simplemachines.client.MachineMargins;
import com.simplemachines.crafter.AutonomousCrafterMenu;
import com.simplemachines.hopper.HopperTier;
import com.simplemachines.hopper.TierHopperMenu;
import com.simplemachines.smither.AutoSmitherMenu;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * N62 (owner 15.11., docs/ai/PRINZIPIEN.md A7 "Container-Margin"): the boxes of simplemachines' own screens
 * ({@link MachineMargins}, drawn by the screens) keep {@code UiBoxes.MARGIN} = 3 px of fill round their outermost slots and
 * elements on every side (container top: the title), the divider is at least 2 px; the player's slots move down by the
 * missing rows and the image grows. The container bounds are written out here from the menus' layout.
 */
public final class ScreenMarginTests {
    private ScreenMarginTests() {}

    public static void screenMargins(GameTestHelper h) {
        Inventory inventory = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL).getInventory();
        // five slots 24..96 at y 20, the raised filter key 135..152 x 19..36 (= a slot at 136/20); the menu's own 6 px suffice
        for (HopperTier tier : HopperTier.values()) {
            TierHopperMenu hopper = new TierHopperMenu(tier, 0, inventory);
            check(h, tier + " hopper", MachineMargins.tierHopper(hopper), hopper.slots, new int[] {24, 19, 136, 20}, 0, 142);
        }
        // grid 26..62 x 17..53, filter key from y 16, result slot 24 x 24 round 134/35 (= a slot at 138/39)
        AutonomousCrafterMenu crafter = new AutonomousCrafterMenu(0, inventory);
        check(h, "autonomous crafter", MachineMargins.autonomousCrafter(crafter), crafter.slots, new int[] {26, 16, 138, 53}, 6, 175);
        // inputs 26..62 at y 35, result slot 24 x 24 round 134/35 (130..154 x 31..55)
        AutoSmitherMenu smither = new AutoSmitherMenu(0, inventory);
        check(h, "auto smither", MachineMargins.autoSmither(smither), smither.slots, new int[] {26, 31, 138, 39}, 0, 169);
        h.succeed();
    }

    /** Every side of both boxes keeps MARGIN, the divider >= GAP, the container box starts at the image top. */
    static void check(GameTestHelper h, String what, UiMargin.Boxes b, List<Slot> slots, int[] bounds, int shift, int height) {
        int[] m = UiMargin.margins(b, slots, bounds);
        String[] side = {"container left", "container right", "container bottom", "inventory left", "inventory right", "inventory top", "inventory bottom"};
        for (int k = 0; k < side.length; k++) h.assertValueEqual(m[k], UiBoxes.MARGIN, what + ": " + side[k]);
        h.assertTrue(m[7] >= UiMargin.GAP, what + ": divider " + m[7] + " < " + UiMargin.GAP);
        h.assertValueEqual(b.container().y(), 0, what + ": container box from the image top (title inside)");
        h.assertValueEqual(b.shift(), shift, what + ": the player's slots move down");
        h.assertValueEqual(b.height(), height, what + ": image height");
    }
}
