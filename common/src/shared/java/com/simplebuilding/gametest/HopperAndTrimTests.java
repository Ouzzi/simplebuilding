package com.simplebuilding.gametest;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.networking.ModMessageHandlers;
import com.simplebuilding.networking.SetHopperGhostItemPayload;
import com.simplebuilding.networking.ToggleHopperFilterPayload;
import com.simplebuilding.screen.ModHopperScreenHandler;
import com.simplebuilding.util.HopperFilterMode;
import com.simplebuilding.util.TrimMultiplierLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;

/**
 * The netherite hopper's item filter and the armour trim benefit multiplier.
 *
 * <p>Both were only covered by "the block is registered" and "the payload class exists" before.
 * The hopper filter decides which items may enter a container, and the trim multiplier scales a
 * gameplay bonus - silent breakage in either is the kind a player notices long before a test
 * suite does.
 */
public final class HopperAndTrimTests {

    private HopperAndTrimTests() {
    }

    private static final BlockPos HOPPER_POS = new BlockPos(2, 1, 2);

    /** Second hopper: the other block that has to be covered by the same block entity type. */
    private static final BlockPos REINFORCED_POS = new BlockPos(4, 1, 2);

    // =====================================================================================
    // HOPPER FILTER
    // =====================================================================================

    /**
     * The three filter modes in turn. Disabled lets everything through; Exact Match compares the
     * whole stack including components; Type Match only compares the item. A slot whose ghost is
     * empty must reject everything once a filter is on - otherwise an unconfigured slot would
     * quietly behave as if the filter were off.
     *
     * <p>Before any of that, the block entity itself: its registered id, and the two blocks the
     * type accepts. Nothing else in the suite reads either. The id is not a cosmetic name - it is
     * the key every placed hopper is saved under, and the valid block set is what
     * {@code BlockEntityType#create} checks before it hands a saved block entity back. Change
     * either and every hopper in an existing world loses its block entity the next time the world
     * loads, taking its inventory and its filter settings with it, while a fresh test world - where
     * every hopper is placed, never loaded - notices nothing at all.
     */
    public static void hopperFilterModesGateWhatMayEnter(GameTestHelper helper) {
        ModHopperBlockEntity hopper = placeHopper(helper);

        // --- one block entity type, under the id the save format uses ---
        Identifier savedAs = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(hopper.getType());
        Identifier expected = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "mod_hopper");
        helper.assertTrue(expected.equals(savedAs),
                "the mod hopper block entity is registered as " + savedAs + " instead of " + expected
                        + "; every hopper already built in a world is saved under that id and loses its "
                        + "contents when it changes");

        // The reinforced hopper builds the same block entity - getBlockEntity fails the test if it
        // builds none or a different one - and the type has to accept both blocks.
        helper.setBlock(REINFORCED_POS, ModBlocks.REINFORCED_HOPPER);
        BlockEntityType<?> type = helper.getBlockEntity(REINFORCED_POS, ModHopperBlockEntity.class).getType();
        helper.assertTrue(type == hopper.getType(), "the two mod hoppers use different block entity types");
        helper.assertTrue(type.isValid(helper.getBlockState(REINFORCED_POS)),
                savedAs + " does not accept the reinforced hopper, so its block entity is dropped on load");
        helper.assertTrue(type.isValid(helper.getBlockState(HOPPER_POS)),
                savedAs + " does not accept the netherite hopper, so its block entity is dropped on load");

        ItemStack stone = new ItemStack(Items.STONE);
        ItemStack dirt = new ItemStack(Items.DIRT);
        ItemStack namedStone = new ItemStack(Items.STONE);
        namedStone.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                net.minecraft.network.chat.Component.literal("a different stone"));

        // --- Disabled: everything goes in ---
        helper.assertTrue(hopper.getFilterMode() == HopperFilterMode.NONE,
                "a fresh hopper did not start with the filter disabled, it is "
                        + hopper.getFilterMode());
        helper.assertTrue(hopper.canPlaceItem(0, stone) && hopper.canPlaceItem(0, dirt),
                "the disabled filter rejected an item");

        // --- Exact Match with no ghost set: nothing goes in ---
        hopper.toggleFilterMode();
        helper.assertTrue(hopper.getFilterMode() == HopperFilterMode.WHITELIST,
                "the first toggle did not reach Exact Match, it is " + hopper.getFilterMode());
        helper.assertTrue(!hopper.canPlaceItem(0, stone),
                "an unconfigured slot accepted an item while the filter was on");

        // --- Exact Match with a ghost: only the very same stack ---
        hopper.setGhostItem(0, stone.copy());
        helper.assertTrue(hopper.canPlaceItem(0, stone), "Exact Match rejected the item it was set to");
        helper.assertTrue(!hopper.canPlaceItem(0, dirt), "Exact Match accepted a different item");
        helper.assertTrue(!hopper.canPlaceItem(0, namedStone),
                "Exact Match ignored the components and accepted a renamed stone");
        helper.assertTrue(!hopper.canPlaceItem(1, stone),
                "a slot without a ghost accepted an item in Exact Match");

        // --- Type Match: same item, components irrelevant ---
        hopper.toggleFilterMode();
        helper.assertTrue(hopper.getFilterMode() == HopperFilterMode.TYPE,
                "the second toggle did not reach Type Match, it is " + hopper.getFilterMode());
        helper.assertTrue(hopper.canPlaceItem(0, namedStone),
                "Type Match rejected a renamed stone, which is what makes it different from Exact Match");
        helper.assertTrue(!hopper.canPlaceItem(0, dirt), "Type Match accepted a different item");

        // --- and back round to Disabled ---
        hopper.toggleFilterMode();
        helper.assertTrue(hopper.getFilterMode() == HopperFilterMode.NONE,
                "the filter mode did not cycle back to disabled, it is " + hopper.getFilterMode());
        helper.assertTrue(hopper.canPlaceItem(1, dirt), "the filter stayed on after cycling back");

        helper.succeed();
    }

    /**
     * Both hopper payloads are only allowed to act while that hopper's screen is actually open.
     * Without the guard any client could retune a hopper it is not looking at, from anywhere in
     * the world.
     *
     * <p><b>And a filter item is a placeholder, not the stack that arrived.</b> The payload is
     * client input: the stack inside it may hold any count, and the server object must not stay
     * connected to it. {@code ModHopperBlockEntity#setGhostItemInternal} therefore stores a copy
     * shrunk to one item. Both halves are checked here, because the earlier version of this test
     * sent a stack that already held exactly one item and only asked which item came back - which
     * is true of the incoming stack itself, so "store the payload as it is" passed. That would put
     * a stack of 64 into the filter row of the screen and leave the server holding an object the
     * network layer still owns.
     */
    public static void hopperPayloadsOnlyActOnAnOpenHopperMenu(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ModHopperBlockEntity hopper = placeHopper(helper);

        // --- no menu open: both payloads have to be ignored ---
        player.containerMenu = player.inventoryMenu;
        ModMessageHandlers.handleToggleHopperFilter(new ToggleHopperFilterPayload(), player);
        helper.assertTrue(hopper.getFilterMode() == HopperFilterMode.NONE,
                "the filter was toggled without the hopper screen being open");

        ModMessageHandlers.handleSetHopperGhostItem(
                new SetHopperGhostItemPayload(0, new ItemStack(Items.DIAMOND)), player);
        helper.assertTrue(hopper.getGhostItem(0).isEmpty(),
                "a ghost item was set without the hopper screen being open");

        // --- with the hopper menu open: both take effect ---
        player.containerMenu = new ModHopperScreenHandler(1, player.getInventory(), hopper, hopper);

        ModMessageHandlers.handleToggleHopperFilter(new ToggleHopperFilterPayload(), player);
        helper.assertTrue(hopper.getFilterMode() == HopperFilterMode.WHITELIST,
                "the toggle payload did not reach the open hopper, mode is " + hopper.getFilterMode());

        // A whole stack, so "count 1" cannot be inherited from the payload.
        ItemStack sent = new ItemStack(Items.DIAMOND, 16);
        ModMessageHandlers.handleSetHopperGhostItem(new SetHopperGhostItemPayload(2, sent), player);
        helper.assertTrue(hopper.getGhostItem(2).is(Items.DIAMOND),
                "the ghost item payload did not reach the open hopper, slot 2 holds "
                        + hopper.getGhostItem(2));
        helper.assertTrue(hopper.getGhostItem(2).getCount() == 1,
                "a filter item is a placeholder and has to be stored as a single item, but slot 2 holds "
                        + hopper.getGhostItem(2).getCount());
        helper.assertTrue(hopper.getGhostItem(0).isEmpty(),
                "the ghost item landed in the wrong slot");

        // The stored filter has to be the hopper's own object: changing the stack the payload
        // carried must not reach into the block entity.
        sent.setCount(64);
        helper.assertTrue(hopper.getGhostItem(2).getCount() == 1,
                "the hopper stored the payload's own stack instead of a copy, so writing to it from "
                        + "outside changed the filter to " + hopper.getGhostItem(2).getCount() + " items");

        player.containerMenu = player.inventoryMenu;
        helper.succeed();
    }

    // =====================================================================================
    // ARMOUR TRIM BENEFIT MULTIPLIER
    // =====================================================================================

    /**
     * The trim bonus is the product of four factors: a configured base, experience level, time
     * survived and combat. Only the first two are deterministic for a fresh mock player, so those
     * are pinned exactly; the other two are only required to stay inside their defined band.
     *
     * <p>The experience curve is the one a player feels directly - it is what makes the bonus
     * grow as they level - so it is checked at both ends and in the middle.
     */
    public static void trimMultiplierFollowsTheExperienceCurveAndTheConfiguredBase(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        // --- the experience curve: 0.1 at level 0, rising to 1.0 at level 100 and capped there ---
        player.experienceLevel = 0;
        assertClose(helper, TrimMultiplierLogic.calculateXPMultiplier(player), 0.1, "level 0");
        player.experienceLevel = 50;
        assertClose(helper, TrimMultiplierLogic.calculateXPMultiplier(player), 0.55, "level 50");
        player.experienceLevel = 100;
        assertClose(helper, TrimMultiplierLogic.calculateXPMultiplier(player), 1.0, "level 100");
        player.experienceLevel = 500;
        assertClose(helper, TrimMultiplierLogic.calculateXPMultiplier(player), 1.0,
                "level 500 (the curve has to stay capped)");

        // --- the other two factors stay inside their band ---
        double survival = TrimMultiplierLogic.calculateSurvivalMultiplier(player);
        double combat = TrimMultiplierLogic.calculateCombatMultiplier(player);
        helper.assertTrue(survival >= 0.1 && survival <= 1.0,
                "the survival factor left its 0.1..1.0 band: " + survival);
        helper.assertTrue(combat >= 0.1 && combat <= 1.0,
                "the combat factor left its 0.1..1.0 band: " + combat);

        // --- the whole product, and that the configured base really scales it ---
        player.experienceLevel = 100;
        double base = SimplebuildingConfig.trimBenefitBaseMultiplier;
        double expected = base * 1.0 * survival * combat;
        assertClose(helper, TrimMultiplierLogic.getMultiplier(player), expected, "the full multiplier");

        SimplebuildingConfig.trimBenefitBaseMultiplier = base * 2.0;
        try {
            assertClose(helper, TrimMultiplierLogic.getMultiplier(player), expected * 2.0,
                    "the full multiplier after doubling the configured base");
        } finally {
            SimplebuildingConfig.trimBenefitBaseMultiplier = base;
        }

        helper.succeed();
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    private static ModHopperBlockEntity placeHopper(GameTestHelper helper) {
        helper.setBlock(HOPPER_POS, ModBlocks.NETHERITE_HOPPER);
        return helper.getBlockEntity(HOPPER_POS, ModHopperBlockEntity.class);
    }

    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(5.5, 1.0, 5.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        // Hand the player back no matter how the test ends. A leaked mock player keeps the
        // player list non-empty and the gametest server then stalls on shutdown - a failing
        // test would cost minutes of wall clock instead of seconds.
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }


    private static void assertClose(GameTestHelper helper, double actual, double expected, String what) {
        helper.assertTrue(Math.abs(actual - expected) < 1.0e-6,
                what + ": expected " + expected + " but got " + actual);
    }
}
