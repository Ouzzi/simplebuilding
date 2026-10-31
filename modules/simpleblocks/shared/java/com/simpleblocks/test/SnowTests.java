package com.simpleblocks.test;

import com.simpleblocks.snow.FrostGrassBlock;
import com.simpleblocks.snow.SnowBlocks;
import com.simpleblocks.snow.SnowConfig;
import com.simpleblocks.snow.SnowloggedBlock;
import com.simpleblocks.snow.SnowloggedBlockEntity;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SnowyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * SN30 (b-snow32, docs/ai/PLAN-B-SNOW32-2026-10-31.md): snowlogging (a snow layer used on a flower or fence shares the cell,
 * more snow adds layers, a fence next to it still connects, a shovel takes the snow first, any other tool drops the plant) and
 * frost grass (freezes under snow above ice or in the cold through Vanilla's grass tick, thaws without snow, drops and tags
 * like grass). Random ticks are called directly with the chances set to 1. All on the module's empty 8 x 8 x 8 structure.
 */
public final class SnowTests {
    private SnowTests() {}

    public static void register(Map<String, Consumer<GameTestHelper>> all, Map<String, Integer> maxTicks) {
        all.put("snowlogging_place_and_layers", SnowTests::placeAndLayers);
        all.put("snowlogged_fence_connects", SnowTests::fenceConnects);
        all.put("snowlogging_shovel_takes_snow_first", SnowTests::shovelTakesSnowFirst);
        all.put("snowlogging_break_without_shovel_drops_plant", SnowTests::breakWithoutShovelDropsPlant);
        all.put("frost_grass_freezes_with_ice_or_cold", SnowTests::frostGrassFreezes);
        all.put("frost_grass_thaws_without_snow", SnowTests::frostGrassThaws);
        all.put("frost_grass_drops_and_tags", SnowTests::frostGrassDropsAndTags);
        all.put("config_command", h -> com.simplelib.test.ConfigCommandTests.module(h, "simpleblocks", false));
    }

    private static ServerPlayer survivor(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    /** Uses the stack on the top face of {@code target} like a player (Fabric's use-block event included). */
    private static void use(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos target) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = helper.absolutePos(target);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND, hit);
    }

    private static BlockState held(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof SnowloggedBlockEntity be ? be.held() : null;
    }

    private static void placeFence(GameTestHelper helper, BlockPos pos, Block fence) {
        BlockPos abs = helper.absolutePos(pos);
        helper.getLevel().setBlock(abs, Block.updateFromNeighbourShapes(fence.defaultBlockState(), helper.getLevel(), abs), Block.UPDATE_ALL);
    }

    /** Items dropped around {@code pos}, as "count path" sorted; the item entities are removed. */
    private static String dropsAround(GameTestHelper helper, BlockPos pos) {
        BlockPos abs = helper.absolutePos(pos);
        Map<String, Integer> counts = new TreeMap<>();
        for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(1.5))) {
            counts.merge(BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).getPath(), item.getItem().getCount(), Integer::sum);
            item.discard();
        }
        StringBuilder out = new StringBuilder();
        counts.forEach((name, count) -> out.append(out.isEmpty() ? "" : ", ").append(count).append(' ').append(name));
        return out.toString();
    }

    /** A snow layer on a poppy and on a fern shares the cell; more snow adds layers; on a fence on a fence it does nothing; config off = Vanilla. */
    public static void placeAndLayers(GameTestHelper helper) {
        BlockPos flower = new BlockPos(1, 2, 1);
        BlockPos fern = new BlockPos(3, 2, 1);
        helper.setBlock(flower.below(), Blocks.GRASS_BLOCK);
        helper.setBlock(flower, Blocks.POPPY);
        helper.setBlock(fern.below(), Blocks.GRASS_BLOCK);
        helper.setBlock(fern, Blocks.FERN);
        ServerPlayer player = survivor(helper);
        ItemStack snow = new ItemStack(Items.SNOW, 8);
        use(helper, player, snow, flower);
        BlockState cell = helper.getBlockState(flower);
        helper.assertTrue(cell.is(SnowBlocks.SNOWLOGGED) && cell.getValue(SnowloggedBlock.LAYERS) == 1, "snow on a poppy gives a snowlogged cell, is " + cell);
        helper.assertTrue(held(helper, flower) == Blocks.POPPY.defaultBlockState(), "the cell holds the poppy, holds " + held(helper, flower));
        helper.assertValueEqual(snow.getCount(), 7, "snow layers left after one use");
        use(helper, player, snow, flower);
        helper.assertValueEqual(helper.getBlockState(flower).getValue(SnowloggedBlock.LAYERS), 2, "a second snow layer");
        helper.assertTrue(helper.getBlockState(flower.below()).getValue(SnowyBlock.SNOWY), "the grass below shows its snowy side");
        // a fence on a fence: no full face below, a snow layer could not stand there
        helper.setBlock(new BlockPos(5, 1, 1), Blocks.STONE);
        placeFence(helper, new BlockPos(5, 2, 1), Blocks.OAK_FENCE);
        placeFence(helper, new BlockPos(5, 3, 1), Blocks.OAK_FENCE);
        use(helper, player, snow, new BlockPos(5, 3, 1));
        helper.assertTrue(helper.getBlockState(new BlockPos(5, 3, 1)).is(Blocks.OAK_FENCE), "no snowlogging without support for the snow");
        boolean before = SnowConfig.snowlogging;
        SnowConfig.snowlogging = false;
        try {
            use(helper, player, snow, fern);
        } finally {
            SnowConfig.snowlogging = before;
        }
        helper.assertTrue(!helper.getBlockState(fern).is(SnowBlocks.SNOWLOGGED), "switched off: no snowlogged fern, is " + helper.getBlockState(fern));
        helper.setBlock(fern, Blocks.FERN);
        use(helper, player, snow, fern);
        helper.assertTrue(held(helper, fern) == Blocks.FERN.defaultBlockState(), "switched on again: the fern is snowlogged");
        helper.succeed();
    }

    /** A snowlogged oak fence keeps its connections and new oak fences connect to it; a nether brick fence does not. */
    public static void fenceConnects(GameTestHelper helper) {
        for (int x = 0; x <= 6; x++) {
            helper.setBlock(new BlockPos(x, 1, 2), Blocks.STONE);
        }
        BlockPos middle = new BlockPos(3, 2, 2);
        placeFence(helper, new BlockPos(4, 2, 2), Blocks.OAK_FENCE);
        placeFence(helper, middle, Blocks.OAK_FENCE);
        use(helper, survivor(helper), new ItemStack(Items.SNOW, 4), middle);
        BlockState cell = helper.getBlockState(middle);
        helper.assertTrue(cell.is(SnowBlocks.SNOWLOGGED) && cell.getValue(SnowloggedBlock.FENCE) == SnowloggedBlock.Fence.WOODEN, "snowlogged oak fence, is " + cell);
        helper.assertTrue(held(helper, middle).getValue(FenceBlock.EAST), "the held fence still connects east");
        helper.assertTrue(helper.getBlockState(new BlockPos(4, 2, 2)).getValue(FenceBlock.WEST), "the east fence still connects to the snowlogged one");
        placeFence(helper, new BlockPos(2, 2, 2), Blocks.SPRUCE_FENCE);
        helper.assertTrue(helper.getBlockState(new BlockPos(2, 2, 2)).getValue(FenceBlock.EAST), "a new wooden fence connects to the snowlogged fence");
        helper.assertTrue(held(helper, middle).getValue(FenceBlock.WEST), "the held fence connects to the new fence");
        placeFence(helper, new BlockPos(3, 2, 3), Blocks.NETHER_BRICK_FENCE);
        helper.assertTrue(!helper.getBlockState(new BlockPos(3, 2, 3)).getValue(FenceBlock.NORTH), "a nether brick fence does not connect to a wooden one");
        helper.assertTrue(helper.getBlockState(middle).getCollisionShape(helper.getLevel(), helper.absolutePos(middle)).max(Direction.Axis.Y) > 1.0,
                "the snowlogged fence stays 1.5 high");
        helper.succeed();
    }

    private static void snowlogged(GameTestHelper helper, BlockPos pos, BlockState held, int layers) {
        helper.setBlock(pos.below(), Blocks.GRASS_BLOCK);
        helper.setBlock(pos, held);
        com.simpleblocks.snow.Snowlogging.snowlog(helper.getLevel(), helper.absolutePos(pos), held);
        helper.setBlock(pos, helper.getBlockState(pos).setValue(SnowloggedBlock.LAYERS, layers));
    }

    /** A shovel takes the snow first (snowballs per layer like Vanilla) and leaves the poppy / the fence standing. */
    public static void shovelTakesSnowFirst(GameTestHelper helper) {
        BlockPos flower = new BlockPos(1, 2, 4);
        BlockPos fence = new BlockPos(5, 2, 4);
        snowlogged(helper, flower, Blocks.POPPY.defaultBlockState(), 2);
        snowlogged(helper, fence, Blocks.OAK_FENCE.defaultBlockState(), 1);
        ServerPlayer player = survivor(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SHOVEL));
        player.gameMode.destroyBlock(helper.absolutePos(flower));
        helper.assertTrue(helper.getBlockState(flower).is(Blocks.POPPY), "the poppy stays after the shovel, is " + helper.getBlockState(flower));
        helper.assertValueEqual(dropsAround(helper, flower), "2 snowball", "shovel on two layers");
        player.gameMode.destroyBlock(helper.absolutePos(fence));
        helper.assertTrue(helper.getBlockState(fence).is(Blocks.OAK_FENCE), "the fence stays after the shovel, is " + helper.getBlockState(fence));
        helper.assertValueEqual(dropsAround(helper, fence), "1 snowball", "shovel on one layer");
        helper.succeed();
    }

    /** Without a shovel the whole cell goes: the plant drops, the snow gives nothing (like Vanilla snow without a shovel). */
    public static void breakWithoutShovelDropsPlant(GameTestHelper helper) {
        BlockPos flower = new BlockPos(2, 2, 2);
        snowlogged(helper, flower, Blocks.POPPY.defaultBlockState(), 3);
        ServerPlayer player = survivor(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.gameMode.destroyBlock(helper.absolutePos(flower));
        helper.assertTrue(helper.getBlockState(flower).isAir(), "broken by hand the cell is gone, is " + helper.getBlockState(flower));
        helper.assertValueEqual(dropsAround(helper, flower), "1 poppy", "drops by hand");
        // the ground below goes: the snow cannot stand, the poppy cannot either -> the poppy drops
        BlockPos other = new BlockPos(5, 2, 2);
        snowlogged(helper, other, Blocks.POPPY.defaultBlockState(), 1);
        helper.setBlock(other.below(), Blocks.AIR);
        helper.assertTrue(helper.getBlockState(other).isAir(), "without ground the cell breaks, is " + helper.getBlockState(other));
        helper.assertValueEqual(dropsAround(helper, other), "1 poppy", "drops without ground");
        helper.succeed();
    }

    /** Grass under snow freezes above ice (any biome) and in the cold; without ice in a warm biome, without snow, or switched off it stays grass. */
    public static void frostGrassFreezes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RandomSource random = level.getRandom();
        BlockPos onIce = new BlockPos(1, 2, 1);
        BlockPos onStone = new BlockPos(3, 2, 1);
        BlockPos bare = new BlockPos(5, 2, 1);
        BlockPos off = new BlockPos(1, 2, 4);
        for (BlockPos grass : List.of(onIce, onStone, bare, off)) {
            helper.setBlock(grass.below(), grass == onStone ? Blocks.STONE : Blocks.ICE);
            helper.setBlock(grass, Blocks.GRASS_BLOCK);
            if (grass != bare) helper.setBlock(grass.above(), Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1));
        }
        int freeze = SnowConfig.freezeOneIn;
        boolean on = SnowConfig.frostGrass;
        try {
            SnowConfig.freezeOneIn = 1;
            for (BlockPos grass : List.of(onIce, onStone, bare)) {
                helper.getBlockState(grass).randomTick(level, helper.absolutePos(grass), random);
            }
            SnowConfig.frostGrass = false;
            helper.getBlockState(off).randomTick(level, helper.absolutePos(off), random);
        } finally {
            SnowConfig.freezeOneIn = freeze;
            SnowConfig.frostGrass = on;
        }
        BlockState frozen = helper.getBlockState(onIce);
        helper.assertTrue(frozen.is(SnowBlocks.FROST_GRASS) && frozen.getValue(SnowyBlock.SNOWY), "grass under snow above ice froze (snowy kept), is " + frozen);
        boolean cold = FrostGrassBlock.cold(level, helper.absolutePos(onStone));
        helper.assertTrue(helper.getBlockState(onStone).is(SnowBlocks.FROST_GRASS) == cold,
                "grass under snow on stone freezes only in a cold biome (cold=" + cold + "), is " + helper.getBlockState(onStone));
        helper.assertTrue(helper.getBlockState(bare).is(Blocks.GRASS_BLOCK), "grass above ice without snow stays grass, is " + helper.getBlockState(bare));
        helper.assertTrue(helper.getBlockState(off).is(Blocks.GRASS_BLOCK), "switched off: grass stays grass, is " + helper.getBlockState(off));
        helper.succeed();
    }

    /** Frost grass without snow thaws to grass, under snow it stays, covered by stone it dies to dirt. */
    public static void frostGrassThaws(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RandomSource random = level.getRandom();
        BlockPos open = new BlockPos(1, 2, 1);
        BlockPos snowy = new BlockPos(3, 2, 1);
        BlockPos covered = new BlockPos(5, 2, 1);
        for (BlockPos pos : List.of(open, snowy, covered)) {
            helper.setBlock(pos.below(), Blocks.STONE);
            helper.setBlock(pos, SnowBlocks.FROST_GRASS);
        }
        helper.setBlock(snowy.above(), Blocks.SNOW_BLOCK);
        helper.setBlock(covered.above(), Blocks.STONE);
        int thaw = SnowConfig.thawOneIn;
        try {
            SnowConfig.thawOneIn = 1;
            for (int i = 0; i < 5; i++) {
                for (BlockPos pos : List.of(open, snowy, covered)) {
                    BlockState state = helper.getBlockState(pos);
                    if (state.is(SnowBlocks.FROST_GRASS)) state.randomTick(level, helper.absolutePos(pos), random);
                }
            }
        } finally {
            SnowConfig.thawOneIn = thaw;
        }
        helper.assertTrue(helper.getBlockState(open).is(Blocks.GRASS_BLOCK), "frost grass without snow thawed, is " + helper.getBlockState(open));
        helper.assertTrue(helper.getBlockState(snowy).is(SnowBlocks.FROST_GRASS), "frost grass under a snow block stays, is " + helper.getBlockState(snowy));
        helper.assertTrue(helper.getBlockState(covered).is(Blocks.DIRT), "frost grass under stone died to dirt, is " + helper.getBlockState(covered));
        helper.succeed();
    }

    /** Loot like the grass block (dirt, itself with Silk Touch), grass tags (plants grow on it, shovel), the cell counts as snow, the tab has it. */
    public static void frostGrassDropsAndTags(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
        ItemStack silk = new ItemStack(Items.IRON_SHOVEL);
        silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
        BlockState frost = SnowBlocks.FROST_GRASS.defaultBlockState();
        helper.assertValueEqual(names(Block.getDrops(frost, level, pos, null, null, shovel)), "1 dirt", "frost grass without silk touch");
        helper.assertValueEqual(names(Block.getDrops(frost, level, pos, null, null, silk)), "1 frost_grass", "frost grass with silk touch");
        helper.assertTrue(frost.is(BlockTags.GRASS_BLOCKS) && frost.is(BlockTags.MINEABLE_WITH_SHOVEL) && frost.is(BlockTags.SUPPORTS_VEGETATION)
                && frost.is(BlockTags.ANIMALS_SPAWNABLE_ON), "frost grass has the grass block's tags");
        helper.setBlock(new BlockPos(2, 1, 2), SnowBlocks.FROST_GRASS);
        helper.assertTrue(Blocks.POPPY.defaultBlockState().canSurvive(level, helper.absolutePos(new BlockPos(2, 2, 2))), "a poppy grows on frost grass");
        helper.assertTrue(SnowBlocks.SNOWLOGGED.defaultBlockState().is(BlockTags.SNOW) && SnowBlocks.SNOWLOGGED.defaultBlockState().is(BlockTags.MINEABLE_WITH_SHOVEL),
                "the snowlogged cell counts as snow and digs with a shovel");
        helper.assertTrue(com.simpleblocks.BlocksRegistry.tabStacks().stream().anyMatch(s -> s.is(SnowBlocks.FROST_GRASS_ITEM)), "frost grass in the Simple Blocks tab");
        helper.succeed();
    }

    private static String names(List<ItemStack> drops) {
        StringBuilder out = new StringBuilder();
        for (ItemStack stack : drops) {
            out.append(out.isEmpty() ? "" : ", ").append(stack.getCount()).append(' ').append(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
        }
        return out.toString();
    }
}
