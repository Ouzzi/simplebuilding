package com.simplebuilding.gametest;

import com.simplebuilding.fletching.ArrowParts;
import com.simplebuilding.fletching.CraftedArrow;
import com.simplebuilding.fletching.FletchingMenu;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.version.McVersion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Befiederungstisch und Pfeile (B14, docs/ai/PLAN-B14-FLETCHING.md). Loader-neutral; auf Linien ohne
 * {@link McVersion#FLETCHING} gelingen die Tests sofort.
 */
public final class FletchingTests {
    private FletchingTests() {
    }

    private static ArrowParts.Parts parts(ArrowParts.Tip tip, ArrowParts.Shaft shaft, ArrowParts.Fletching fletching) {
        return new ArrowParts.Parts(tip, shaft, fletching);
    }

    private static CraftedArrow arrow(GameTestHelper helper, ArrowParts.Parts parts) {
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 3.0, 1.5));
        return new CraftedArrow(helper.getLevel(), at.x, at.y, at.z, ArrowParts.stack(parts, 1), null);
    }

    /** Spitzen: Grundbonus plus Bonus gegen ihre Zielgruppe, sonst nichts. */
    public static void eachTipAddsItsDamageAgainstItsTargets(GameTestHelper helper) {
        if (!McVersion.FLETCHING) {
            helper.succeed();
            return;
        }
        Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
        Mob drowned = helper.spawnWithNoFreeWill(EntityTypes.DROWNED, new BlockPos(1, 2, 1));
        Mob skeleton = helper.spawnWithNoFreeWill(EntityTypes.SKELETON, new BlockPos(1, 2, 1));
        Mob cow = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(1, 2, 1));
        check(helper, ArrowParts.Tip.FLINT, zombie, 0.0);
        check(helper, ArrowParts.Tip.COPPER, drowned, 2.0);
        check(helper, ArrowParts.Tip.COPPER, zombie, 0.0);
        check(helper, ArrowParts.Tip.IRON, zombie, 2.5);
        check(helper, ArrowParts.Tip.IRON, cow, 0.5);
        check(helper, ArrowParts.Tip.GOLD, skeleton, 2.0);
        check(helper, ArrowParts.Tip.GOLD, cow, 0.0);
        check(helper, ArrowParts.Tip.DIAMOND, cow, 1.0);
        check(helper, ArrowParts.Tip.NETHERITE, cow, 1.5);
        check(helper, ArrowParts.Tip.ENDERITE, cow, 1.5);
        helper.succeed();
    }

    private static void check(GameTestHelper helper, ArrowParts.Tip tip, Mob target, double expected) {
        helper.assertValueEqual(tip.bonusAgainst(target), expected, tip.getSerializedName() + " tip against " + target.getType());
    }

    /** Schaft und Befiederung aendern die Schwerkraft, Netherit durchbohrt, Enderit fliegt anfangs gerade. */
    public static void shaftsAndFletchingsChangeTheFlight(GameTestHelper helper) {
        if (!McVersion.FLETCHING) {
            helper.succeed();
            return;
        }
        double vanilla = arrow(helper, ArrowParts.Parts.VANILLA).getGravity();
        helper.assertValueEqual(vanilla, 0.05, "gravity of the plain crafted arrow");
        helper.assertValueEqual(arrow(helper, parts(ArrowParts.Tip.FLINT, ArrowParts.Shaft.END_ROD, ArrowParts.Fletching.FEATHER)).getGravity(),
                0.025, "gravity with an end rod shaft");
        helper.assertTrue(Math.abs(arrow(helper, parts(ArrowParts.Tip.FLINT, ArrowParts.Shaft.STICK, ArrowParts.Fletching.PHANTOM_MEMBRANE)).getGravity()
                - 0.035) < 1.0E-9, "gravity with phantom membrane fletching");
        helper.assertValueEqual(arrow(helper, parts(ArrowParts.Tip.ENDERITE, ArrowParts.Shaft.STICK, ArrowParts.Fletching.FEATHER)).getGravity(),
                0.0, "gravity of an enderite arrow in its first second");
        helper.assertValueEqual((int) arrow(helper, parts(ArrowParts.Tip.NETHERITE, ArrowParts.Shaft.STICK, ArrowParts.Fletching.FEATHER)).getPierceLevel(),
                ArrowParts.NETHERITE_EXTRA_PIERCE, "pierce level of a netherite arrow");
        helper.assertValueEqual((int) arrow(helper, ArrowParts.Parts.VANILLA).getPierceLevel(), 0, "pierce level of the plain arrow");
        helper.assertTrue(arrow(helper, parts(ArrowParts.Tip.PRISMARINE, ArrowParts.Shaft.STICK, ArrowParts.Fletching.FEATHER)).getWaterInertia()
                > arrow(helper, ArrowParts.Parts.VANILLA).getWaterInertia(), "prismarine arrows are not slowed down by water");
        helper.succeed();
    }

    /** Drei passende Teile ergeben vier Pfeile mit diesen Teilen; Nehmen verbraucht je ein Teil. */
    public static void theTableMakesFourArrowsFromThreeParts(GameTestHelper helper) {
        if (!McVersion.FLETCHING) {
            helper.succeed();
            return;
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        FletchingMenu menu = new FletchingMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
        helper.assertFalse(menu.getSlot(FletchingMenu.TIP_SLOT).mayPlace(new ItemStack(Items.STICK)), "a stick does not fit the tip slot");
        helper.assertFalse(menu.getSlot(FletchingMenu.SHAFT_SLOT).mayPlace(new ItemStack(Items.DIRT)), "dirt does not fit the shaft slot");
        menu.getSlot(FletchingMenu.TIP_SLOT).set(new ItemStack(Items.IRON_NUGGET, 2));
        menu.getSlot(FletchingMenu.SHAFT_SLOT).set(new ItemStack(Items.BLAZE_ROD, 2));
        helper.assertTrue(menu.getSlot(FletchingMenu.RESULT_SLOT).getItem().isEmpty(), "no result without fletching");
        menu.getSlot(FletchingMenu.FLETCHING_SLOT).set(new ItemStack(Items.FEATHER, 1));
        ItemStack result = menu.getSlot(FletchingMenu.RESULT_SLOT).getItem();
        helper.assertTrue(result.is(ModItems.CRAFTED_ARROW), "the result is a crafted arrow: " + result);
        helper.assertValueEqual(result.getCount(), ArrowParts.ARROWS_PER_CRAFT, "arrows per craft");
        helper.assertValueEqual(ArrowParts.of(result), parts(ArrowParts.Tip.IRON, ArrowParts.Shaft.BLAZE_ROD, ArrowParts.Fletching.FEATHER), "parts of the result");
        menu.quickMoveStack(player, FletchingMenu.RESULT_SLOT);
        helper.assertValueEqual(player.getInventory().countItem(ModItems.CRAFTED_ARROW), 4, "arrows moved into the inventory");
        helper.assertValueEqual(menu.getSlot(FletchingMenu.TIP_SLOT).getItem().getCount(), 1, "tips left");
        helper.assertValueEqual(menu.getSlot(FletchingMenu.SHAFT_SLOT).getItem().getCount(), 1, "shafts left");
        helper.assertTrue(menu.getSlot(FletchingMenu.FLETCHING_SLOT).getItem().isEmpty(), "the only feather is used up");
        helper.assertTrue(menu.getSlot(FletchingMenu.RESULT_SLOT).getItem().isEmpty(), "no result once a part is missing");
        helper.succeed();
    }

    /** Die Material-Knoepfe legen nur Items aus dem eigenen Inventar in ihren Slot, Shift-Klick sortiert Teile ein. */
    public static void materialButtonsOnlyMoveItemsTheyFind(GameTestHelper helper) {
        if (!McVersion.FLETCHING) {
            helper.succeed();
            return;
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().setItem(0, new ItemStack(Items.AMETHYST_SHARD, 5));
        player.getInventory().setItem(1, new ItemStack(Items.PHANTOM_MEMBRANE, 3));
        FletchingMenu menu = new FletchingMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
        helper.assertTrue(menu.clickMenuButton(player, FletchingMenu.TIP_BUTTON + ArrowParts.Tip.AMETHYST.ordinal()), "amethyst button");
        helper.assertValueEqual(menu.getSlot(FletchingMenu.TIP_SLOT).getItem().getCount(), 5, "amethyst moved into the tip slot");
        helper.assertTrue(player.getInventory().getItem(0).isEmpty(), "amethyst left the inventory");
        menu.clickMenuButton(player, FletchingMenu.TIP_BUTTON + ArrowParts.Tip.DIAMOND.ordinal());
        helper.assertTrue(menu.getSlot(FletchingMenu.TIP_SLOT).getItem().isEmpty(),
                "without diamond pebbles the button only clears the slot: " + menu.getSlot(FletchingMenu.TIP_SLOT).getItem());
        helper.assertValueEqual(player.getInventory().countItem(Items.AMETHYST_SHARD), 5, "the amethyst went back, nothing was made");
        helper.assertFalse(menu.clickMenuButton(player, 99), "unknown buttons are rejected");
        int membraneSlot = 4 + 27 + 1;
        menu.quickMoveStack(player, membraneSlot);
        helper.assertValueEqual(menu.getSlot(FletchingMenu.FLETCHING_SLOT).getItem().getCount(), 3, "shift click put the membranes into the fletching slot");
        helper.succeed();
    }

    /** Rechtsklick auf den Vanilla-Befiederungstisch oeffnet das Menue. */
    public static void rightClickingTheFletchingTableOpensTheMenu(GameTestHelper helper) {
        if (!McVersion.FLETCHING) {
            helper.succeed();
            return;
        }
        BlockPos table = new BlockPos(1, 2, 1);
        helper.setBlock(table, Blocks.FLETCHING_TABLE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos at = helper.absolutePos(table);
        helper.getLevel().getBlockState(at).useWithoutItem(helper.getLevel(), player,
                new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false));
        helper.assertTrue(player.containerMenu instanceof FletchingMenu, "the fletching table opened " + player.containerMenu);
        helper.succeed();
    }
}
