package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.MaterialOctetBlock;
import com.simplebuilding.blocks.custom.OctetCellBlock;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.MaterialOctetItem;
import com.simplebuilding.items.custom.SledgehammerItem;
import com.simplebuilding.items.custom.SledgehammerItem.ReshapeMode;
import com.simplebuilding.util.HammerCorners;
import com.simplebuilding.util.MaterialOctets;
import com.simplebuilding.version.McVersion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Queue Nachtrag 24 (docs/ai/PLAN-Q-HAMMER-OCTETS-2026-10-09.md): the sledgehammer reshapes only while sneaking and
 * then removes the aimed eighth; shapes that are neither slab nor stairs fall apart into the material's octet cell.
 * Wood and melon have octets; the glistering melon slice is edible.
 */
public final class SledgehammerOctetTests {
    private static final BlockPos CELL = new BlockPos(2, 1, 2);

    private SledgehammerOctetTests() {
    }

    /** The pure rule behind charging and the hand tilt: nothing without sneaking where carving exists. */
    public static void reshapeModeNeedsSneaking(GameTestHelper helper) {
        helper.assertValueEqual(SledgehammerItem.reshapeMode(false, false, true), ReshapeMode.NONE, "plain, no touch");
        helper.assertValueEqual(SledgehammerItem.reshapeMode(false, true, true), ReshapeMode.NONE, "plain, touch");
        helper.assertValueEqual(SledgehammerItem.reshapeMode(true, false, true), ReshapeMode.CARVE, "sneak, no touch");
        helper.assertValueEqual(SledgehammerItem.reshapeMode(true, true, true), ReshapeMode.REVERSE, "sneak, touch");
        // 26.2 has no carving: the old forward ladder stays, the plain sneaking hammer does nothing.
        helper.assertValueEqual(SledgehammerItem.reshapeMode(false, false, false), ReshapeMode.FORWARD, "26.2 plain");
        helper.assertValueEqual(SledgehammerItem.reshapeMode(false, true, false), ReshapeMode.FORWARD, "26.2 plain, touch");
        helper.assertValueEqual(SledgehammerItem.reshapeMode(true, false, false), ReshapeMode.NONE, "26.2 sneak");
        helper.assertValueEqual(SledgehammerItem.reshapeMode(true, true, false), ReshapeMode.REVERSE, "26.2 sneak, touch");
        helper.succeed();
    }

    /** The hammer tilts (hint "transformable") on a carvable block only while sneaking; never on dirt. */
    public static void hammerTiltsOnlyWhileSneaking(GameTestHelper helper) {
        if (!McVersion.TRANSFORM_HINTS_AND_CORNERS) { helper.succeed(); return; }
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(CELL);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER));
        // the bottom face: its lower north-west eighth is there in every block tried (stairs: the solid base)
        BlockHitResult hit = new BlockHitResult(Vec3.atLowerCornerOf(pos).add(0.25, 0.0, 0.25), Direction.DOWN, pos, false);
        for (Block block : new Block[]{Blocks.OAK_PLANKS, Blocks.STONE, Blocks.OAK_STAIRS, Blocks.MELON}) {
            level.setBlockAndUpdate(pos, block.defaultBlockState());
            player.setShiftKeyDown(false);
            helper.assertFalse(com.simplebuilding.util.TransformTargets.canTransformTarget(level, hit, player, InteractionHand.MAIN_HAND),
                    "the hammer tilts without sneaking on " + block);
            player.setShiftKeyDown(true);
            helper.assertTrue(com.simplebuilding.util.TransformTargets.canTransformTarget(level, hit, player, InteractionHand.MAIN_HAND),
                    "the sneaking hammer does not tilt on " + block);
        }
        level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        helper.assertFalse(com.simplebuilding.util.TransformTargets.canTransformTarget(level, hit, player, InteractionHand.MAIN_HAND),
                "the sneaking hammer tilts on dirt");
        player.setShiftKeyDown(false);
        helper.succeed();
    }

    /** A survival player standing well clear of the cells, so it never obstructs an octet. */
    private static ServerPlayer awayPlayer(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        Vec3 away = helper.absoluteVec(new Vec3(0.5, 6.0, 0.5));
        player.snapTo(away.x, away.y, away.z, 0.0F, 0.0F);
        return player;
    }

    private static int bit(int x, int y, int z) {
        return 1 << OctetCellBlock.index(x, y, z);
    }

    /** Aim at the given octet from its outer face (top for upper octets, bottom for lower ones). */
    private static BlockState carve(SledgehammerItem hammer, ServerPlayer player, ItemStack stack, BlockState state,
                                    BlockPos pos, int x, int y, int z) {
        Direction side = y == 1 ? Direction.UP : Direction.DOWN;
        Vec3 aim = new Vec3(x * 0.5 + 0.25, y, z * 0.5 + 0.25);
        return hammer.getTransformationState(state, pos, side, aim, player, stack);
    }

    /**
     * One eighth above and one below removed is no vanilla shape: oak falls apart into oak octets (6/8), a diagonal
     * too, a slab missing a quarter too; stone has no octets and refuses; the melon block becomes a melon cell.
     */
    public static void sledgehammerSplitsUnsupportedShapesIntoOctets(GameTestHelper helper) {
        if (!McVersion.TRANSFORM_HINTS_AND_CORNERS || !McVersion.CHESS) { helper.succeed(); return; }
        BlockPos pos = helper.absolutePos(CELL);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setShiftKeyDown(true);
        ItemStack stack = new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        SledgehammerItem hammer = (SledgehammerItem) stack.getItem();
        Block oakCell = MaterialOctets.cellFor(Blocks.OAK_PLANKS);
        helper.assertTrue(oakCell != null, "oak planks have no octet cell");

        // top octet off: vanilla stairs (no octets yet)
        BlockState stairs = carve(hammer, player, stack, Blocks.OAK_PLANKS.defaultBlockState(), pos, 1, 1, 1);
        helper.assertTrue(stairs != null && stairs.is(Blocks.OAK_STAIRS), "first eighth did not leave oak stairs: " + stairs);
        helper.assertValueEqual(HammerCorners.octets(stairs), OctetCellBlock.FULL & ~bit(1, 1, 1), "stairs octets");
        // and one from the solid base: unsupported -> oak octets
        BlockState split = carve(hammer, player, stack, stairs, pos, 0, 0, 0);
        helper.assertTrue(split != null && split.is(oakCell), "top + bottom eighth did not fall apart into oak octets: " + split);
        helper.assertValueEqual(OctetCellBlock.mask(split), OctetCellBlock.FULL & ~bit(1, 1, 1) & ~bit(0, 0, 0), "octets left");
        helper.assertValueEqual(OctetCellBlock.count(split), 6, "six octets left");
        // diagonal quarters on top
        BlockState diagonal = carve(hammer, player, stack, stairs, pos, 0, 1, 0);
        helper.assertTrue(diagonal != null && diagonal.is(oakCell), "a diagonal did not fall apart into octets: " + diagonal);
        // a straight stair stays a stair, the last quarter makes a slab
        BlockState straight = carve(hammer, player, stack, stairs, pos, 1, 1, 0);
        helper.assertTrue(straight != null && straight.is(Blocks.OAK_STAIRS), "a straight stair became " + straight);
        BlockState outer = carve(hammer, player, stack, straight, pos, 0, 1, 1);
        helper.assertTrue(outer != null && outer.is(Blocks.OAK_STAIRS), "an outer corner became " + outer);
        BlockState slab = carve(hammer, player, stack, outer, pos, 0, 1, 0);
        helper.assertTrue(slab != null && slab.is(Blocks.OAK_SLAB) && slab.getValue(SlabBlock.TYPE) == SlabType.BOTTOM,
                "the last top eighth did not leave a bottom slab: " + slab);
        // a slab missing a quarter
        BlockState slabCut = carve(hammer, player, stack, Blocks.OAK_SLAB.defaultBlockState(), pos, 1, 0, 1);
        helper.assertTrue(slabCut != null && slabCut.is(oakCell) && OctetCellBlock.count(slabCut) == 3,
                "a slab missing a quarter did not fall apart into three octets: " + slabCut);
        // waterlogging carries over into the cell
        BlockState wet = carve(hammer, player, stack, stairs.setValue(StairBlock.WATERLOGGED, true), pos, 0, 0, 0);
        helper.assertTrue(wet.getValue(OctetCellBlock.WATERLOGGED), "the octet cell lost the water of the stairs");
        // stone has no octets: the unsupported carve is refused (no charge, no tilt)
        BlockState stoneStairs = carve(hammer, player, stack, Blocks.STONE.defaultBlockState(), pos, 1, 1, 1);
        helper.assertTrue(stoneStairs != null && stoneStairs.is(Blocks.STONE_STAIRS), "stone did not carve into stairs");
        helper.assertTrue(carve(hammer, player, stack, stoneStairs, pos, 0, 0, 0) == null, "stone fell apart without octets");
        // melon block: no stairs, straight into a melon cell
        BlockState melon = carve(hammer, player, stack, Blocks.MELON.defaultBlockState(), pos, 1, 1, 1);
        helper.assertTrue(melon != null && melon.is(ModBlocks.MELON_OCTET) && OctetCellBlock.count(melon) == 7,
                "the melon block did not become seven melon octets: " + melon);
        // what falls out
        helper.assertValueEqual(HammerCorners.removedPiece(Blocks.OAK_PLANKS.defaultBlockState()),
                ((MaterialOctetBlock) oakCell).piece(), "removed piece of oak planks");
        helper.assertValueEqual(HammerCorners.removedPiece(stairs), ((MaterialOctetBlock) oakCell).piece(), "removed piece of oak stairs");
        helper.assertValueEqual(HammerCorners.removedPiece(Blocks.MELON.defaultBlockState()), Items.MELON_SLICE, "removed piece of melon");
        helper.assertTrue(HammerCorners.removedPiece(Blocks.STONE.defaultBlockState()) == null, "stone dropped a piece");
        player.setShiftKeyDown(false);
        helper.succeed();
    }

    /** A real sneaking charge on oak planks in survival: stairs, one oak octet falls out, one point of wear. */
    public static void sledgehammerCarveDropsTheRemovedOctet(GameTestHelper helper) {
        if (!McVersion.TRANSFORM_HINTS_AND_CORNERS || !McVersion.CHESS) { helper.succeed(); return; }
        helper.setBlock(CELL, Blocks.OAK_PLANKS);
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        Vec3 eye = helper.absoluteVec(new Vec3(CELL.getX() + 0.5, CELL.getY() + 2.0, CELL.getZ() + 0.5));
        player.snapTo(eye.x, eye.y, eye.z, 0.0F, 90.0F);
        ItemStack stack = new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        // without sneaking: nothing
        player.setShiftKeyDown(false);
        BlockPos pos = helper.absolutePos(CELL);
        BlockHitResult top = new BlockHitResult(Vec3.atCenterOf(pos).add(0.1, 0.5, 0.1), Direction.UP, pos, false);
        helper.assertValueEqual(stack.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, top)),
                InteractionResult.PASS, "a plain right click started a charge");
        stack.getItem().finishUsingItem(stack, helper.getLevel(), player);
        helper.assertBlockPresent(Blocks.OAK_PLANKS, CELL);
        // sneaking: carve
        player.setShiftKeyDown(true);
        helper.assertValueEqual(stack.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, top)),
                InteractionResult.CONSUME, "the sneaking right click did not start a charge");
        stack.getItem().finishUsingItem(stack, helper.getLevel(), player);
        player.stopUsingItem();
        player.setShiftKeyDown(false);
        helper.assertBlockPresent(Blocks.OAK_STAIRS, CELL);
        helper.assertValueEqual(stack.getDamageValue(), 1, "wear for one carve");
        Item oakOctet = ((MaterialOctetBlock) MaterialOctets.cellFor(Blocks.OAK_PLANKS)).piece();
        int octets = 0;
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2.0))) {
            if (entity.getItem().is(oakOctet)) octets += entity.getItem().getCount();
            entity.discard();
        }
        helper.assertValueEqual(octets, 1, "oak octets dropped by one carve");
        helper.succeed();
    }

    /** Every wood of this version has an octet item that places into the sub-grid, burns like planks and drops itself. */
    public static void woodOctetsPlaceBurnAndDrop(GameTestHelper helper) {
        if (!McVersion.CHESS) { helper.succeed(); return; }
        int expected = 0;
        for (String wood : ModBlocks.OCTET_WOODS) {
            if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(
                    net.minecraft.resources.Identifier.withDefaultNamespace(wood + "_planks")).isPresent()) expected++;
        }
        helper.assertValueEqual(ModBlocks.WOOD_OCTETS.size(), expected, "one octet cell per wood");
        helper.assertValueEqual(ModItems.WOOD_OCTETS.size(), expected, "one octet item per wood");
        var fire = (com.simplebuilding.tweaks.mixin.FireBlockInvoker) Blocks.FIRE;
        for (Block cell : ModBlocks.WOOD_OCTETS) {
            Block planks = ((MaterialOctetBlock) cell).source();
            helper.assertValueEqual(fire.simplebuilding$getIgniteOdds(cell.defaultBlockState()) > 0,
                    fire.simplebuilding$getIgniteOdds(planks.defaultBlockState()) > 0, "flammability of " + cell);
            helper.assertTrue(((MaterialOctetBlock) cell).piece() instanceof MaterialOctetItem, "octet item of " + cell);
        }
        // place two oak octets: on top of a block, then next to the first one
        Block oakCell = MaterialOctets.cellFor(Blocks.OAK_PLANKS);
        Item oakOctet = ((MaterialOctetBlock) oakCell).piece();
        BlockPos floor = helper.absolutePos(CELL);
        helper.setBlock(CELL, Blocks.STONE);
        ServerPlayer player = awayPlayer(helper);
        ItemStack stack = new ItemStack(oakOctet, 4);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockHitResult top = new BlockHitResult(new Vec3(floor.getX() + 0.25, floor.getY() + 1.0, floor.getZ() + 0.25), Direction.UP, floor, false);
        helper.assertValueEqual(oakOctet.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, top)), InteractionResult.SUCCESS, "placing an oak octet");
        BlockState placed = helper.getLevel().getBlockState(floor.above());
        helper.assertTrue(placed.is(oakCell) && OctetCellBlock.mask(placed) == bit(0, 0, 0), "oak octet not in the clicked corner: " + placed);
        helper.assertValueEqual(stack.getCount(), 3, "octet consumed");
        BlockHitResult second = new BlockHitResult(new Vec3(floor.getX() + 0.75, floor.getY() + 1.0, floor.getZ() + 0.25), Direction.UP, floor, false);
        oakOctet.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, second));
        helper.assertValueEqual(OctetCellBlock.count(helper.getLevel().getBlockState(floor.above())), 2, "second octet joins the cell");
        helper.getLevel().destroyBlock(floor.above(), true);
        int dropped = 0;
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(floor.above()).inflate(2.0))) {
            if (entity.getItem().is(oakOctet)) dropped += entity.getItem().getCount();
            entity.discard();
        }
        helper.assertValueEqual(dropped, 2, "a cell of two drops two octets");
        helper.succeed();
    }

    /** Sneak + right-click with a melon slice places a melon octet; without sneaking it is eaten; glistering is food. */
    public static void melonSlicePlacesAnOctetAndGlisteringMelonIsEdible(GameTestHelper helper) {
        FoodProperties food = Items.GLISTERING_MELON_SLICE.components().get(DataComponents.FOOD);
        helper.assertTrue(food != null && food.nutrition() == 6, "the glistering melon slice is not edible: " + food);
        helper.assertTrue(Items.GLISTERING_MELON_SLICE.components().has(DataComponents.CONSUMABLE), "the glistering melon slice cannot be consumed");
        if (!McVersion.CHESS) { helper.succeed(); return; }
        BlockPos floor = helper.absolutePos(CELL);
        helper.setBlock(CELL, Blocks.STONE);
        ServerPlayer player = awayPlayer(helper);
        ItemStack slices = new ItemStack(Items.MELON_SLICE, 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, slices);
        BlockHitResult top = new BlockHitResult(new Vec3(floor.getX() + 0.75, floor.getY() + 1.0, floor.getZ() + 0.75), Direction.UP, floor, false);
        player.setShiftKeyDown(false);
        helper.assertFalse(Items.MELON_SLICE.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, top)).consumesAction(),
                "a melon slice was placed without sneaking");
        helper.assertBlockPresent(Blocks.AIR, CELL.above());
        player.setShiftKeyDown(true);
        helper.assertTrue(Items.MELON_SLICE.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, top)).consumesAction(),
                "the sneaking melon slice was not placed");
        player.setShiftKeyDown(false);
        BlockState placed = helper.getLevel().getBlockState(floor.above());
        helper.assertTrue(placed.is(ModBlocks.MELON_OCTET) && OctetCellBlock.mask(placed) == bit(1, 0, 1),
                "the melon octet is not in the clicked corner: " + placed);
        helper.assertValueEqual(slices.getCount(), 2, "melon slice consumed");
        helper.getLevel().destroyBlock(floor.above(), true);
        int dropped = 0;
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(floor.above()).inflate(2.0))) {
            if (entity.getItem().is(Items.MELON_SLICE)) dropped += entity.getItem().getCount();
            entity.discard();
        }
        helper.assertValueEqual(dropped, 1, "the melon octet drops its slice");
        helper.succeed();
    }
}
