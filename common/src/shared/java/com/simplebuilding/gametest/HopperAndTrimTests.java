package com.simplebuilding.gametest;

import com.simplebuilding.util.SurvivalTracerAccessor;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.networking.ModMessageHandlers;
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
     * whole stack including components; Type Match only compares the item. The filter of a slot is
     * the real item lying in it (filter principle); an empty slot must reject everything once a filter is on - otherwise an unconfigured slot would
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
        if (!com.simplebuilding.version.McVersion.HOPPERS_IN_SB) { helper.succeed(); return; } // MACH32-MOD stage 3: Simple Machines tests its hoppers on 26.3
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

        // --- Exact Match with an empty slot: nothing goes in ---
        hopper.toggleFilterMode();
        helper.assertTrue(hopper.getFilterMode() == HopperFilterMode.WHITELIST,
                "the first toggle did not reach Exact Match, it is " + hopper.getFilterMode());
        helper.assertTrue(!hopper.canPlaceItem(0, stone),
                "an unconfigured slot accepted an item while the filter was on");

        // --- Exact Match with a stone lying in slot 0: only the very same stack ---
        hopper.setItem(0, stone.copy());
        helper.assertTrue(hopper.canPlaceItem(0, stone), "Exact Match rejected the item it was set to");
        // The filter compares item and components, never the count. Nothing in the game asks about a
        // single item: HopperBlockEntity#addItem hands canPlaceItem the whole stack it just picked up
        // and a slot asks with the whole cursor stack, so a filter that also compared counts would
        // refuse every stack larger than the one item lying there while every count-1 probe still passed.
        helper.assertTrue(hopper.canPlaceItem(0, new ItemStack(Items.STONE, 16)),
                "Exact Match rejected a stack of 16 of the very item it was set to; the count must not "
                        + "take part in the match");
        helper.assertTrue(!hopper.canPlaceItem(0, dirt), "Exact Match accepted a different item");
        helper.assertTrue(!hopper.canPlaceItem(0, namedStone),
                "Exact Match ignored the components and accepted a renamed stone");
        helper.assertTrue(!hopper.canPlaceItem(1, stone),
                "an empty slot accepted an item in Exact Match");

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
     * The filter key's payload is only allowed to act while that hopper's screen is actually open. Without the guard
     * any client could retune a hopper it is not looking at, from anywhere in the world. (The ghost item payload of
     * the old filter is gone with the filter principle, 2026-10-09.)
     */
    public static void hopperPayloadsOnlyActOnAnOpenHopperMenu(GameTestHelper helper) {
        if (!com.simplebuilding.version.McVersion.HOPPERS_IN_SB) { helper.succeed(); return; } // MACH32-MOD stage 3: Simple Machines tests its hoppers on 26.3
        ServerPlayer player = mockPlayer(helper);
        ModHopperBlockEntity hopper = placeHopper(helper);

        // --- no menu open: the payload has to be ignored ---
        player.containerMenu = player.inventoryMenu;
        ModMessageHandlers.handleToggleHopperFilter(new ToggleHopperFilterPayload(), player);
        helper.assertTrue(hopper.getFilterMode() == HopperFilterMode.NONE,
                "the filter was toggled without the hopper screen being open");

        // --- with the hopper menu open: it takes effect ---
        player.containerMenu = new ModHopperScreenHandler(1, player.getInventory(), hopper, hopper);
        ModMessageHandlers.handleToggleHopperFilter(new ToggleHopperFilterPayload(), player);
        helper.assertTrue(hopper.getFilterMode() == HopperFilterMode.WHITELIST,
                "the toggle payload did not reach the open hopper, mode is " + hopper.getFilterMode());

        player.containerMenu = player.inventoryMenu;
        helper.succeed();
    }

    // =====================================================================================
    // ARMOUR TRIM BENEFIT MULTIPLIER
    // =====================================================================================

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
