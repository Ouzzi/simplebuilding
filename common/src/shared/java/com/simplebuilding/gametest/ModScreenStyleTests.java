package com.simplebuilding.gametest;

import com.simplebuilding.blocks.custom.ChestTier;
import com.simplebuilding.client.gui.ModScreenLayout;
import com.simplebuilding.client.gui.ModScreenLayout.Box;
import com.simplebuilding.fletching.FletchingMenu;
import com.simplebuilding.items.custom.BackpackTier;
import com.simplebuilding.screen.AutoSmitherMenu;
import com.simplebuilding.screen.BackpackLayout;
import com.simplebuilding.screen.BackpackMenu;
import com.simplebuilding.screen.BackpackOpenData;
import com.simplebuilding.screen.NetheriteHopperScreenHandler;
import com.simplebuilding.screen.TieredChestMenu;
import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.GameType;

/**
 * The boxes of the mod screens in the container style ({@link ModScreenLayout}, drawn on 26.3 by
 * {@code ModScreenStyle}; simplecontainers plan W1 G4): every slot sits inside the fill area of its box - never on a
 * frame, never in the divider - for every tier and size, so a menu that moves its slots or a tier with more rows
 * shows up here before it shows up as a slot on a frame. Loader-neutral; pure numbers, no client classes.
 */
public final class ModScreenStyleTests {
    private ModScreenStyleTests() {}

    /** Mod chests (single and double, every tier): chest slots in the tier box, player slots in the inventory box. */
    public static void tieredChestSlotsSitInsideTheirBoxes(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        List<String> problems = new ArrayList<>();
        for (ChestTier tier : ChestTier.values()) {
            for (boolean isDouble : new boolean[] {false, true}) {
                int size = tier.slots() * (isDouble ? 2 : 1);
                TieredChestMenu menu = TieredChestMenu.server(0, player.getInventory(), new SimpleContainer(size), tier, isDouble);
                twoBoxes(menu, menu.imageWidth(), menu.imageHeight(), tier + (isDouble ? " double" : " single"), problems);
            }
        }
        report(helper, problems);
    }

    /** Hopper, auto smither and fletching table: container slots (result as the big 24x24 slot) and the filter key. */
    public static void machineSlotsSitInsideTheirBoxes(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Inventory inv = player.getInventory();
        List<String> problems = new ArrayList<>();
        NetheriteHopperScreenHandler hopper = new NetheriteHopperScreenHandler(0, inv, BlockPos.ZERO);
        twoBoxes(hopper, 176, 133, "hopper", problems);
        int[] origin = ModScreenLayout.inventoryOrigin(hopper);
        int keyX = com.simplebuilding.screen.ModHopperScreenHandler.FILTER_BUTTON_X;
        if (!ModScreenLayout.container(176, origin[1]).holds(keyX, 19, 18, 18)) {
            problems.add("hopper: the filter key (" + keyX + ", 19) leaves the box");
        }
        // Owner N23: slots, caption gap and key form one row centred in the 176 px image.
        int rowLeft = hopper.getSlot(0).x - 1, rowRight = keyX + 18;
        if (McVersion.CRUCIBLE && Math.abs(rowLeft - (176 - rowRight)) > 1) {
            problems.add("hopper: the slot row with the filter key is not centred (" + rowLeft + " .. " + rowRight + ")");
        }
        if (McVersion.AUTONOMOUS_CRAFTER) {
            com.simplebuilding.screen.AutonomousCrafterMenu crafter = new com.simplebuilding.screen.AutonomousCrafterMenu(0, inv);
            twoBoxes(crafter, 176, 166, "autonomous crafter", problems);
            bigSlot(crafter, crafter.getSlot(com.simplebuilding.screen.AutonomousCrafterMenu.RESULT_SLOT), "autonomous crafter", problems);
            int[] o = ModScreenLayout.inventoryOrigin(crafter);
            if (!ModScreenLayout.container(176, o[1]).holds(com.simplebuilding.screen.AutonomousCrafterMenu.FILTER_BUTTON_X,
                    com.simplebuilding.screen.AutonomousCrafterMenu.FILTER_BUTTON_Y, 18, 18)) {
                problems.add("autonomous crafter: the filter key leaves the box");
            }
        }
        if (McVersion.ASTRAL_ENCHANTING) {
            // Astral Enchanting Table (N27): slots, slider panel and enchant button inside the violet box.
            com.simplebuilding.screen.AstralEnchantingMenu astral = new com.simplebuilding.screen.AstralEnchantingMenu(0, inv);
            twoBoxes(astral, 176, com.simplebuilding.screen.AstralEnchantingMenu.IMAGE_HEIGHT, "astral enchanting table", problems);
            Box box = ModScreenLayout.container(176, ModScreenLayout.inventoryOrigin(astral)[1]);
            if (!box.holds(com.simplebuilding.client.gui.AstralEnchantingScreen.PANEL_X - 1, com.simplebuilding.client.gui.AstralEnchantingScreen.PANEL_Y - 1,
                    com.simplebuilding.client.gui.AstralEnchantingScreen.PANEL_W + 2, com.simplebuilding.client.gui.AstralEnchantingScreen.PANEL_H + 2)) {
                problems.add("astral enchanting table: the slider panel leaves " + box);
            }
            if (!box.holds(com.simplebuilding.client.gui.AstralEnchantingScreen.BUTTON_X, com.simplebuilding.client.gui.AstralEnchantingScreen.BUTTON_Y,
                    com.simplebuilding.client.gui.AstralEnchantingScreen.BUTTON_W, com.simplebuilding.client.gui.AstralEnchantingScreen.BUTTON_H)) {
                problems.add("astral enchanting table: the enchant button leaves " + box);
            }
        }
        if (McVersion.AUTO_SMITHER) {
            AutoSmitherMenu smither = new AutoSmitherMenu(0, inv);
            twoBoxes(smither, 176, 166, "auto smither", problems);
            bigSlot(smither, smither.getSlot(AutoSmitherMenu.RESULT_SLOT), "auto smither", problems);
        }
        if (McVersion.FLETCHING) {
            FletchingMenu fletching = new FletchingMenu(0, inv);
            int y = fletching.getSlot(FletchingMenu.TIP_SLOT).y;
            int x = fletching.getSlot(FletchingMenu.TIP_SLOT).x;
            for (int index : new int[] {FletchingMenu.SHAFT_SLOT, FletchingMenu.FLETCHING_SLOT}) {
                Slot part = fletching.getSlot(index);
                if (part.y != y) problems.add("fletching: part slots are not on one row");
                // Left to right: feather, shaft, tip (the tip sits next to the arrow).
                if (x - part.x != 18 * (index - FletchingMenu.TIP_SLOT)) {
                    problems.add("fletching: part slots are not 18 px apart");
                }
            }
            twoBoxes(fletching, 176, 166, "fletching table", problems);
            bigSlot(fletching, fletching.getSlot(FletchingMenu.RESULT_SLOT), "fletching table", problems);
            fletchingBookButton(fletching, problems);
        }
        report(helper, problems);
    }

    /** Backpacks (every tier, extra columns included): one box; the upper part above the seam, the rows below it. */
    public static void backpackSlotsSitInsideTheOneBox(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        List<String> problems = new ArrayList<>();
        for (BackpackTier tier : BackpackTier.values()) {
            BackpackMenu menu = new BackpackMenu(0, player.getInventory(), BackpackOpenData.worn(tier, 1));
            BackpackLayout layout = menu.layout();
            Box box = ModScreenLayout.backpack(layout);
            int seam = ModScreenLayout.BACKPACK_SEAM_Y;
            for (Slot slot : menu.slots) {
                if (!slot.isActive()) continue;
                String where = tier + " slot " + slot.index + " (" + slot.x + ", " + slot.y + ")";
                if (!box.holds(slot.x, slot.y, 17, 17)) problems.add(where + " leaves the box " + box);
                boolean above = slot.y < seam;
                if (above && slot.y + 17 > seam) problems.add(where + " runs into the seam at " + seam);
                if (!above && slot.y < seam + 2) problems.add(where + " sits on the seam at " + seam);
            }
            int[] strip = ModScreenLayout.backpackStrip(layout);
            if (strip[0] < ModScreenLayout.FRAME || strip[2] > box.width() - ModScreenLayout.FRAME) {
                problems.add(tier + ": the tinted strip leaves the box");
            }
        }
        report(helper, problems);
    }

    /** Container slots in the full-width box from the top, player slots in the 176 wide inventory box below it. */
    private static void twoBoxes(AbstractContainerMenu menu, int imageWidth, int imageHeight, String what, List<String> problems) {
        int[] origin = ModScreenLayout.inventoryOrigin(menu);
        Box container = ModScreenLayout.container(imageWidth, origin[1]);
        Box inventory = ModScreenLayout.inventory(origin[0], origin[1]);
        if (container.bottom() + ModScreenLayout.GAP != inventory.y()) problems.add(what + ": divider is not " + ModScreenLayout.GAP + " px");
        if (inventory.x() < 0 || inventory.right() > imageWidth || inventory.bottom() > imageHeight) {
            problems.add(what + ": inventory box " + inventory + " leaves the image " + imageWidth + "x" + imageHeight);
        }
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) continue;
            Box box = slot.container instanceof Inventory ? inventory : container;
            if (!box.holds(slot.x, slot.y, 17, 17)) {
                problems.add(what + " slot " + slot.index + " (" + slot.x + ", " + slot.y + ") leaves " + box);
            }
        }
    }

    private static void bigSlot(AbstractContainerMenu menu, Slot slot, String what, List<String> problems) {
        Box container = ModScreenLayout.container(176, ModScreenLayout.inventoryOrigin(menu)[1]);
        if (!container.holds(slot.x - 4, slot.y - 4, 25, 25)) problems.add(what + ": the big result slot leaves " + container);
    }

    /** The recipe book button (left of the part row, crafting-table spot) lies in the container box and over no slot (round 2). */
    private static void fletchingBookButton(AbstractContainerMenu menu, List<String> problems) {
        int bx = ModScreenLayout.FLETCHING_BOOK_X, by = ModScreenLayout.FLETCHING_BOOK_Y;
        int bw = ModScreenLayout.FLETCHING_BOOK_W, bh = ModScreenLayout.FLETCHING_BOOK_H;
        Box container = ModScreenLayout.container(176, ModScreenLayout.inventoryOrigin(menu)[1]);
        if (!container.holds(bx, by, bw, bh)) problems.add("fletching: the recipe book button (" + bx + ", " + by + ") leaves " + container);
        for (Slot slot : menu.slots) {
            if (!slot.isActive() || slot.container instanceof Inventory) continue;
            if (bx < slot.x + 17 && slot.x < bx + bw && by < slot.y + 17 && slot.y < by + bh) {
                problems.add("fletching: the recipe book button overlaps slot " + slot.index + " (" + slot.x + ", " + slot.y + ")");
            }
        }
    }

    private static void report(GameTestHelper helper, List<String> problems) {
        helper.assertTrue(problems.isEmpty(), "Mod screen boxes: " + problems);
        helper.succeed();
    }
}
