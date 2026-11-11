package com.simplemachines.test;

import com.simplemachines.SimpleMachines;
import com.simplemachines.crafter.AutonomousCrafterBlock;
import com.simplemachines.crafter.AutonomousCrafterBlockEntity;
import com.simplemachines.crafter.AutonomousCrafterMenu;
import com.simplemachines.crafter.Crafters;
import com.simplemachines.hopper.HopperFilterMode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/**
 * MACH32-MOD stage 4b (docs/ai/PLAN-B-MACH32S4B-2026-11-10.md): the Autonomous Crafter in Simple Machines. SimpleBuilding's
 * {@code WorkstationTests} rows (crafts only above a hopper, redstone stops it, the filter keeps the recipe items) plus what the move
 * adds: registration, the old ids and saved data of a world from before 10.11., automation cannot pull ingredients out, and the
 * menu layout (filter key inside the box). Runs without SimpleBuilding.
 */
public final class AutonomousCrafterTests {
    private AutonomousCrafterTests() {}

    /** Block, item, block entity type, menu and recipe under simplemachines:autonomous_crafter; the block entity accepts the block. */
    public static void autonomousCrafterRegistered(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        Identifier id = SimpleMachines.id(Crafters.ID);
        if (BuiltInRegistries.BLOCK.getValue(id) != Crafters.BLOCK) problems.add("block");
        if (BuiltInRegistries.ITEM.getValue(id) != Crafters.ITEM) problems.add("item");
        if (BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(id) != Crafters.BLOCK_ENTITY) problems.add("block entity type");
        if (BuiltInRegistries.MENU.getValue(id) != Crafters.MENU) problems.add("menu");
        if (Crafters.BLOCK_ENTITY == null || !Crafters.BLOCK_ENTITY.isValid(Crafters.BLOCK.defaultBlockState())) problems.add("block entity refuses the block");
        if (helper.getLevel().recipeAccess().byKey(net.minecraft.resources.ResourceKey.create(Registries.RECIPE, id)).isEmpty()) problems.add("recipe");
        helper.assertTrue(problems.isEmpty(), "autonomous crafter registration: " + problems);
        helper.succeed();
    }

    /**
     * simplebuilding:autonomous_crafter (item, block, block entity, recipe) resolves here; a saved crafter loads with its grid,
     * switched-off slots and filter mode (same NBT as SimpleBuilding's).
     */
    public static void oldAutonomousCrafterIdsMove(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        Identifier old = Identifier.fromNamespaceAndPath(SimpleMachines.CORE_NAMESPACE, Crafters.ID);
        if (BuiltInRegistries.BLOCK.getValue(old) != Crafters.BLOCK) problems.add("block " + old);
        if (BuiltInRegistries.ITEM.getValue(old) != Crafters.ITEM) problems.add("item " + old);
        if (BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(old) != Crafters.BLOCK_ENTITY) problems.add("block entity type " + old);
        Identifier recipe = com.simplelib.api.IdMoves.target(Registries.RECIPE, old);
        if (recipe == null || !recipe.equals(SimpleMachines.id(Crafters.ID))) problems.add("recipe -> " + recipe);
        CompoundTag saved = new CompoundTag();
        saved.putString("id", old.toString());
        saved.putInt("x", 0);
        saved.putInt("y", 0);
        saved.putInt("z", 0);
        ListTag items = new ListTag();
        CompoundTag planks = new CompoundTag();
        planks.putString("id", "minecraft:oak_planks");
        planks.putInt("count", 5);
        planks.putByte("Slot", (byte) 3);
        items.add(planks);
        saved.put("Items", items);
        saved.putIntArray("disabled_slots", new int[] {8});
        saved.putInt("FilterMode", HopperFilterMode.TYPE.ordinal());
        BlockEntity loaded = BlockEntity.loadStatic(helper.absolutePos(new BlockPos(1, 1, 1)), Crafters.BLOCK.defaultBlockState(), saved,
                helper.getLevel().registryAccess());
        if (!(loaded instanceof AutonomousCrafterBlockEntity crafter)) {
            problems.add("the old block entity loaded as " + loaded);
        } else {
            if (!crafter.getItem(3).is(Items.OAK_PLANKS) || crafter.getItem(3).getCount() != 5) problems.add("contents " + crafter.getItem(3));
            if (!crafter.isSlotDisabled(8) || crafter.isSlotDisabled(0)) problems.add("switched-off slots");
            if (crafter.filterMode() != HopperFilterMode.TYPE) problems.add("filter mode " + crafter.filterMode());
        }
        helper.assertTrue(problems.isEmpty(), "old autonomous crafter ids and data: " + problems);
        helper.succeed();
    }

    /** Two planks over each other: four sticks per craft. */
    private static AutonomousCrafterBlockEntity plankCrafter(GameTestHelper helper, BlockPos pos, int planks) {
        helper.setBlock(pos, Crafters.BLOCK);
        AutonomousCrafterBlockEntity crafter = helper.getBlockEntity(pos, AutonomousCrafterBlockEntity.class);
        crafter.setItem(0, new ItemStack(Items.OAK_PLANKS, planks));
        crafter.setItem(3, new ItemStack(Items.OAK_PLANKS, planks));
        return crafter;
    }

    private static int countIn(Container container, Item item) {
        int n = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).is(item)) n += container.getItem(i).getCount();
        }
        return n;
    }

    /**
     * Crafting automation (owner N26): it crafts only while a hopper stands below. Without one the planks stay; once a Vanilla hopper
     * is placed below, the sticks arrive in it and the hopper below cannot pull the planks out of the crafter.
     */
    public static void autonomousCrafterCraftsOnlyAboveHoppers(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        AutonomousCrafterBlockEntity crafter = plankCrafter(helper, pos, 2);
        helper.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    helper.assertValueEqual(crafter.getItem(0).getCount(), 2, "planks used up without a hopper below");
                    helper.setBlock(pos.below(), Blocks.HOPPER);
                })
                .thenWaitUntil(() -> helper.assertValueEqual(countIn(helper.getBlockEntity(pos.below(), HopperBlockEntity.class), Items.STICK), 8,
                        "sticks in the hopper below after two crafts"))
                .thenExecute(() -> {
                    helper.assertTrue(crafter.isEmpty(), "the crafter kept " + crafter.getItem(0) + " / " + crafter.getItem(3));
                    helper.assertValueEqual(countIn(helper.getBlockEntity(pos.below(), HopperBlockEntity.class), Items.OAK_PLANKS), 0,
                            "planks the hopper below pulled out of the crafter");
                })
                .thenSucceed();
    }

    /** Redstone control: a signal stops it (TRIGGERED), without it it crafts again. */
    public static void autonomousCrafterStopsOnRedstone(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        BlockPos power = pos.east();
        helper.setBlock(pos.below(), Blocks.HOPPER);
        helper.setBlock(power, Blocks.REDSTONE_BLOCK);
        AutonomousCrafterBlockEntity crafter = plankCrafter(helper, pos, 2);
        helper.assertTrue(crafter.isTriggered(), "the redstone block next to the crafter did not trigger it");
        helper.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    helper.assertValueEqual(crafter.getItem(0).getCount(), 2, "planks used up while powered");
                    helper.assertTrue(helper.getBlockState(pos).getValue(AutonomousCrafterBlock.TRIGGERED), "TRIGGERED off while powered");
                    helper.setBlock(power, Blocks.AIR);
                })
                .thenWaitUntil(() -> helper.assertTrue(crafter.isEmpty(), "the crafter did not craft once the signal was gone"))
                .thenSucceed();
    }

    /**
     * Filter principle: with the filter key on, every filled slot keeps one plank as its recipe item. Three planks per slot give
     * two crafts, then it stops with one plank in each slot; automation only tops up a slot holding a match, an empty slot takes nothing.
     */
    public static void autonomousCrafterFilterKeepsTheRecipeItems(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos.below(), Blocks.HOPPER);
        AutonomousCrafterBlockEntity crafter = plankCrafter(helper, pos, 3);
        crafter.cycleFilterMode();
        helper.assertTrue(crafter.canPlaceItem(0, new ItemStack(Items.OAK_PLANKS)) || crafter.canPlaceItem(3, new ItemStack(Items.OAK_PLANKS)),
                "a slot holding planks refuses planks with the filter on");
        helper.assertFalse(crafter.canPlaceItem(0, new ItemStack(Items.STONE)), "a plank slot takes stone with the filter on");
        helper.assertFalse(crafter.canPlaceItem(1, new ItemStack(Items.OAK_PLANKS)), "an empty slot takes planks with the filter on");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertValueEqual(countIn(helper.getBlockEntity(pos.below(), HopperBlockEntity.class), Items.STICK), 8,
                        "sticks after two crafts"))
                .thenIdle(20)
                .thenExecute(() -> {
                    helper.assertValueEqual(countIn(helper.getBlockEntity(pos.below(), HopperBlockEntity.class), Items.STICK), 8,
                            "sticks a while later - the recipe items must stay");
                    helper.assertValueEqual(crafter.getItem(0).getCount(), 1, "planks left in slot 0 (its recipe item)");
                    helper.assertValueEqual(crafter.getItem(3).getCount(), 1, "planks left in slot 3 (its recipe item)");
                })
                .thenSucceed();
    }

    /** Nothing leaves through any face; the bottom offers no slots; the comparator counts filled and switched-off slots. */
    public static void autonomousCrafterKeepsItsIngredients(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        AutonomousCrafterBlockEntity crafter = plankCrafter(helper, pos, 2);
        List<String> problems = new ArrayList<>();
        for (Direction side : Direction.values()) {
            if (crafter.canTakeItemThroughFace(0, crafter.getItem(0), side)) problems.add("takes out through " + side);
        }
        if (crafter.getSlotsForFace(Direction.DOWN).length != 0) problems.add("bottom offers slots");
        if (crafter.canPlaceItemThroughFace(1, new ItemStack(Items.OAK_PLANKS), Direction.DOWN)) problems.add("accepts from below");
        crafter.setSlotDisabled(8, true);
        if (crafter.redstoneSignal() != 3) problems.add("comparator " + crafter.redstoneSignal() + " instead of 3");
        if (crafter.canPlaceItem(8, new ItemStack(Items.OAK_PLANKS))) problems.add("a switched-off slot takes items");
        helper.assertTrue(problems.isEmpty(), "autonomous crafter automation: " + problems);
        helper.succeed();
    }

    /** The menu (SimpleBuilding's layout): 9 grid slots, the result preview, and the filter key inside the container box. */
    public static void autonomousCrafterMenuLayout(GameTestHelper helper) {
        Inventory inv = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL).getInventory();
        AutonomousCrafterMenu menu = new AutonomousCrafterMenu(0, inv);
        List<String> problems = new ArrayList<>();
        if (menu.slots.size() != AutonomousCrafterMenu.RESULT_SLOT + 1) problems.add("slot count " + menu.slots.size());
        var result = menu.getSlot(AutonomousCrafterMenu.RESULT_SLOT);
        if (result.x != 134 || result.y != 35) problems.add("result slot at " + result.x + "," + result.y);
        int inventoryY = menu.getSlot(AutonomousCrafterMenu.INV_START).y;
        // container box: from the top to the divider above the inventory (frame 5, gap 2)
        int boxBottom = inventoryY - 5 - 2;
        if (AutonomousCrafterMenu.FILTER_BUTTON_X + 18 > 176 || AutonomousCrafterMenu.FILTER_BUTTON_Y + 18 > boxBottom) problems.add("filter key leaves the box");
        if (menu.filterMode() != HopperFilterMode.NONE) problems.add("filter mode " + menu.filterMode());
        helper.assertTrue(problems.isEmpty(), "autonomous crafter menu: " + problems);
        helper.succeed();
    }
}
