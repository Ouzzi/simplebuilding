package com.simpleredstone.test;

import com.simpleredstone.RedstoneRegistry;
import com.simpleredstone.SimpleRedstone;
import com.simpleredstone.block.SignalDampenerBlock;
import com.simpleredstone.config.RedstoneConfig;
import com.simpleredstone.piston.DoublePistons;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Simple Redstone (WP-17): the Double Piston (two cells, signal 1-7 one block, 8-15 two, pushes, retracts, falls as a unit, cannot
 * be pushed) and the Signal Dampener (stages 0-3, one redstone tick, side inputs ignored). Loader-neutral; the Fabric adapter
 * registers {@link #ALL} as module_game_test_<name> on {@code simpleredstone:empty} (8x8x8), every test runs standalone.
 */
public final class RedstoneTests {
    /** Test name -> body, in catalogue order (the Fabric adapter names them module_game_test_<name>). */
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();

    static {
        ALL.put("registered", RedstoneTests::registered);
        ALL.put("sub_mod_switch", RedstoneTests::subModSwitch);
        ALL.put("recipes_craft", RedstoneTests::recipesCraft);
        ALL.put("dampener_subtracts_the_stage", RedstoneTests::dampenerSubtractsTheStage);
        ALL.put("dampener_waits_one_redstone_tick", RedstoneTests::dampenerWaitsOneRedstoneTick);
        ALL.put("dampener_stage_cycles_on_right_click", RedstoneTests::dampenerStageCyclesOnRightClick);
        ALL.put("dampener_ignores_side_inputs", RedstoneTests::dampenerIgnoresSideInputs);
        ALL.put("double_piston_places_two_cells", RedstoneTests::doublePistonPlacesTwoCells);
        ALL.put("double_piston_one_block_for_a_weak_signal", RedstoneTests::doublePistonOneBlockForAWeakSignal);
        ALL.put("double_piston_two_blocks_for_a_strong_signal", RedstoneTests::doublePistonTwoBlocksForAStrongSignal);
        ALL.put("double_piston_pushes_and_retracts", RedstoneTests::doublePistonPushesAndRetracts);
        ALL.put("double_piston_cannot_be_pushed", RedstoneTests::doublePistonCannotBePushed);
        ALL.put("double_piston_falls_as_a_unit", RedstoneTests::doublePistonFallsAsAUnit);
        ALL.put("double_piston_switch_off_keeps_it_still", RedstoneTests::doublePistonSwitchOffKeepsItStill);
        ALL.put("discover_recipe_unlocks", com.simpleredstone.discover.RedstoneDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simpleredstone.discover.RedstoneDiscover::behaviourHints);
        ALL.put("guide_book", com.simpleredstone.guide.RedstoneGuide::gameTest);
    }

    private RedstoneTests() {}

    private static void clear(GameTestHelper h) {
        for (int x = 0; x <= 7; x++) {
            for (int y = 0; y <= 7; y++) {
                for (int z = 0; z <= 7; z++) {
                    h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
    }

    /** A step of a timed test: do something, then wait until the condition holds (the test fails when it never does). */
    private record Step(String what, Runnable act, BooleanSupplier done) {}

    private static void sequence(GameTestHelper h, List<Step> steps) {
        int[] index = {0};
        boolean[] started = {false};
        h.succeedWhen(() -> {
            while (index[0] < steps.size()) {
                Step step = steps.get(index[0]);
                if (!started[0]) {
                    step.act().run();
                    started[0] = true;
                }
                h.assertTrue(step.done().getAsBoolean(), "waiting for: " + step.what());
                index[0]++;
                started[0] = false;
            }
        });
    }

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 spot = h.absoluteVec(new Vec3(6.5, 1.0, 6.5));
        player.setPos(spot.x, spot.y, spot.z);
        return player;
    }

    private static BlockState at(GameTestHelper h, BlockPos pos) {
        return h.getBlockState(pos);
    }

    /** The Double Piston in a row along +x at {@code base}: base cell and barrel placed, head at ext 0. */
    private static void doublePiston(GameTestHelper h, BlockPos base) {
        h.setBlock(base.below(), Blocks.STONE);
        h.setBlock(base.east().below(), Blocks.STONE);
        h.setBlock(base, RedstoneRegistry.DOUBLE_PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.EAST));
        h.setBlock(base.east(), RedstoneRegistry.PISTON_BARREL.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.EAST));
    }

    private static int ext(GameTestHelper h, BlockPos base) {
        BlockState a = at(h, base);
        return a.is(RedstoneRegistry.DOUBLE_PISTON) ? a.getValue(DoublePistons.EXT) : -1;
    }

    private static boolean parts(GameTestHelper h, BlockPos base, int ext) {
        boolean head1 = at(h, base.east(2)).is(RedstoneRegistry.PISTON_HEAD);
        boolean arm = at(h, base.east(2)).is(RedstoneRegistry.PISTON_ARM);
        boolean head2 = at(h, base.east(3)).is(RedstoneRegistry.PISTON_HEAD);
        return switch (ext) {
            case 0 -> !head1 && !arm && !head2;
            case 1 -> head1 && !head2;
            default -> arm && head2;
        };
    }

    // ---------------------------------------------------------------- module

    public static void registered(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        for (String name : List.of("double_piston", "double_piston_barrel", "double_piston_head", "double_piston_arm", "signal_dampener")) {
            if (!BuiltInRegistries.BLOCK.containsKey(SimpleRedstone.id(name))) problems.add("block " + name);
        }
        for (String name : List.of("double_piston", "signal_dampener", "guide_book")) {
            if (!BuiltInRegistries.ITEM.containsKey(SimpleRedstone.id(name))) problems.add("item " + name);
        }
        if (RedstoneRegistry.TAB == null) problems.add("creative tab");
        for (Block part : List.of(RedstoneRegistry.DOUBLE_PISTON, RedstoneRegistry.PISTON_BARREL, RedstoneRegistry.PISTON_HEAD, RedstoneRegistry.PISTON_ARM)) {
            BlockState state = part.defaultBlockState();
            if (PistonBaseBlock.isPushable(state, h.getLevel(), BlockPos.ZERO.above(70), Direction.EAST, false, Direction.EAST)) problems.add("pushable " + state);
        }
        h.assertTrue(problems.isEmpty(), "registration problems: " + problems);
        h.succeed();
    }

    public static void subModSwitch(GameTestHelper h) {
        com.simplebuilding.framework.api.SubMods.set(SimpleRedstone.MOD_ID, false);
        try {
            h.assertFalse(SimpleRedstone.on(), "switched off");
            h.assertTrue(RedstoneRegistry.tabStacks().isEmpty() && RedstoneRegistry.partStacks().isEmpty(), "no tab stacks while off");
            h.assertTrue(BuiltInRegistries.BLOCK.containsKey(SimpleRedstone.id("double_piston")), "registrations stay");
        } finally {
            com.simplebuilding.framework.api.SubMods.set(SimpleRedstone.MOD_ID, true);
        }
        h.assertTrue(SimpleRedstone.on() && !RedstoneRegistry.tabStacks().isEmpty(), "back on");
        h.succeed();
    }

    public static void recipesCraft(GameTestHelper h) {
        var recipes = h.getLevel().getServer().getRecipeManager();
        ItemStack q = new ItemStack(Items.QUARTZ), r = new ItemStack(Items.REDSTONE_BLOCK), i = new ItemStack(Items.IRON_INGOT);
        ItemStack p = new ItemStack(Items.PISTON), b = new ItemStack(Items.IRON_BLOCK), e = ItemStack.EMPTY;
        var dp = CraftingInput.of(3, 3, List.of(q, r, q, i, p, i, b, p, e));
        ItemStack out = recipes.getRecipeFor(RecipeType.CRAFTING, dp, h.getLevel()).map(x -> x.value().assemble(dp)).orElse(ItemStack.EMPTY);
        h.assertTrue(out.is(RedstoneRegistry.DOUBLE_PISTON_ITEM) && out.getCount() == 1, "double piston recipe gave " + out);
        ItemStack t = new ItemStack(Items.REDSTONE_TORCH), s = new ItemStack(Items.STONE);
        var sd = CraftingInput.of(3, 2, List.of(t, q, t, s, s, s));
        out = recipes.getRecipeFor(RecipeType.CRAFTING, sd, h.getLevel()).map(x -> x.value().assemble(sd)).orElse(ItemStack.EMPTY);
        h.assertTrue(out.is(RedstoneRegistry.SIGNAL_DAMPENER_ITEM) && out.getCount() == 1, "dampener recipe gave " + out);
        h.succeed();
    }

    // ---------------------------------------------------------------- dampener

    private static BlockState dampener(int stage, Direction input) {
        return RedstoneRegistry.SIGNAL_DAMPENER.defaultBlockState().setValue(SignalDampenerBlock.FACING, input).setValue(SignalDampenerBlock.STAGE, stage);
    }

    /**
     * Output = max(0, input - stage) for every pair, and the owner's examples: 3 - 3 = 0, 4 - 2 = 2. In the world: a redstone block
     * (15) in front of the Dampener for the four stages, then a chain of Dampeners of stage 3 that brings the input down to 3 and 4.
     */
    public static void dampenerSubtractsTheStage(GameTestHelper h) {
        for (int input = 0; input <= 15; input++) {
            for (int stage = 0; stage <= 3; stage++) {
                h.assertValueEqual(SignalDampenerBlock.dampen(input, stage), Math.max(0, input - stage), "dampen(" + input + ", " + stage + ")");
            }
        }
        h.assertValueEqual(SignalDampenerBlock.dampen(3, 3), 0, "3 - 3");
        h.assertValueEqual(SignalDampenerBlock.dampen(4, 2), 2, "4 - 2");
        clear(h);
        BlockPos d = new BlockPos(5, 2, 3);
        for (int x = 0; x <= 5; x++) h.setBlock(new BlockPos(x, 1, 3), Blocks.STONE);
        h.setBlock(d, dampener(0, Direction.WEST));
        h.setBlock(d.west(), Blocks.REDSTONE_BLOCK);
        List<Step> steps = new ArrayList<>();
        for (int stage = 0; stage <= 3; stage++) {
            int st = stage;
            steps.add(new Step("15 with stage " + st + " -> " + (15 - st), () -> {
                h.setBlock(d, dampener(st, Direction.WEST));
                h.tickBlock(d);
            }, () -> at(h, d).getValue(SignalDampenerBlock.SIGNAL) == 15 - st && at(h, d).getValue(SignalDampenerBlock.POWERED)));
        }
        // chain: 15 -> 12 -> 9 -> 6 -> 3 (four stage-3 Dampeners), then a stage-3 Dampener makes 0; with stages 3, 3, 3, 2 it is 4, then stage 2 makes 2
        steps.add(new Step("3 - 3 = 0", () -> {
            h.setBlock(d.west(), Blocks.AIR);
            for (int x = 1; x <= 4; x++) h.setBlock(new BlockPos(x, 2, 3), dampener(3, Direction.WEST));
            h.setBlock(new BlockPos(0, 2, 3), Blocks.REDSTONE_BLOCK);
            h.setBlock(d, dampener(3, Direction.WEST));
        }, () -> at(h, new BlockPos(4, 2, 3)).getValue(SignalDampenerBlock.SIGNAL) == 3 && at(h, d).getValue(SignalDampenerBlock.SIGNAL) == 0 && !at(h, d).getValue(SignalDampenerBlock.POWERED)));
        steps.add(new Step("4 - 2 = 2", () -> {
            h.setBlock(new BlockPos(4, 2, 3), dampener(2, Direction.WEST));
            h.setBlock(d, dampener(2, Direction.WEST));
        }, () -> at(h, new BlockPos(4, 2, 3)).getValue(SignalDampenerBlock.SIGNAL) == 4 && at(h, d).getValue(SignalDampenerBlock.SIGNAL) == 2));
        sequence(h, steps);
    }

    /** The change shows after a scheduled tick of 2 game ticks (one redstone tick), not at once. */
    public static void dampenerWaitsOneRedstoneTick(GameTestHelper h) {
        clear(h);
        BlockPos d = new BlockPos(3, 2, 3);
        h.setBlock(d.below(), Blocks.STONE);
        h.setBlock(d, dampener(1, Direction.WEST));
        sequence(h, List.of(
                new Step("input arrives, output still 0 with a tick scheduled", () -> h.setBlock(d.west(), Blocks.REDSTONE_BLOCK),
                        () -> at(h, d).getValue(SignalDampenerBlock.SIGNAL) == 0 && h.getLevel().getBlockTicks().hasScheduledTick(h.absolutePos(d), RedstoneRegistry.SIGNAL_DAMPENER)),
                new Step("output 14 after the tick", () -> { }, () -> at(h, d).getValue(SignalDampenerBlock.SIGNAL) == 14)));
    }

    public static void dampenerStageCyclesOnRightClick(GameTestHelper h) {
        clear(h);
        BlockPos d = new BlockPos(3, 2, 3);
        h.setBlock(d.below(), Blocks.STONE);
        h.setBlock(d, dampener(0, Direction.WEST));
        ServerPlayer player = player(h);
        int[] seen = new int[5];
        for (int i = 0; i < 5; i++) {
            seen[i] = at(h, d).getValue(SignalDampenerBlock.STAGE);
            h.useBlock(d, player);
        }
        h.assertTrue(java.util.Arrays.equals(seen, new int[] {0, 1, 2, 3, 0}), "stages while clicking: " + java.util.Arrays.toString(seen));
        h.succeed();
    }

    /** Only the input in front counts: a strong source at the side leaves the output at 0. */
    public static void dampenerIgnoresSideInputs(GameTestHelper h) {
        clear(h);
        BlockPos d = new BlockPos(3, 2, 3);
        h.setBlock(d.below(), Blocks.STONE);
        h.setBlock(d, dampener(0, Direction.WEST));
        h.setBlock(d.north(), Blocks.REDSTONE_BLOCK);
        h.setBlock(d.south(), Blocks.REDSTONE_BLOCK);
        h.setBlock(d.above(), Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(8, () -> {
            h.assertTrue(at(h, d).getValue(SignalDampenerBlock.SIGNAL) == 0, "side sources moved the output: " + at(h, d));
            h.assertTrue(h.getLevel().getBlockTicks().hasScheduledTick(h.absolutePos(d), RedstoneRegistry.SIGNAL_DAMPENER) == false, "a tick for a side input");
            h.succeed();
        });
    }

    // ---------------------------------------------------------------- double piston

    /** The item places base and barrel (two cells, facing the clicker's opposite) and needs both cells free. */
    public static void doublePistonPlacesTwoCells(GameTestHelper h) {
        clear(h);
        h.setBlock(new BlockPos(1, 1, 3), Blocks.STONE);
        ServerPlayer player = player(h);
        player.setYRot(90.0F); // looks west: the piston faces east like Vanilla's (towards the player's opposite)
        player.setXRot(0.0F);
        ItemStack stack = new ItemStack(RedstoneRegistry.DOUBLE_PISTON_ITEM, 4);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = h.absolutePos(new BlockPos(1, 1, 3));
        var result = stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false)));
        BlockPos base = new BlockPos(1, 2, 3);
        h.assertTrue(result.consumesAction(), "not placed: " + result);
        BlockState a = at(h, base);
        h.assertTrue(a.is(RedstoneRegistry.DOUBLE_PISTON) && a.getValue(DirectionalBlock.FACING) == Direction.EAST, "base cell: " + a);
        h.assertTrue(DoublePistons.isPart(at(h, base.east()), RedstoneRegistry.PISTON_BARREL, Direction.EAST), "barrel cell: " + at(h, base.east()));
        // the front cell taken: nothing is placed
        h.setBlock(new BlockPos(1, 2, 5), Blocks.AIR);
        h.setBlock(new BlockPos(2, 2, 5), Blocks.STONE);
        h.setBlock(new BlockPos(1, 1, 5), Blocks.STONE);
        BlockPos abs2 = h.absolutePos(new BlockPos(1, 1, 5));
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs2).add(0, 0.5, 0), Direction.UP, abs2, false)));
        h.assertTrue(at(h, new BlockPos(1, 2, 5)).isAir(), "placed although the front cell is taken: " + at(h, new BlockPos(1, 2, 5)));
        h.succeed();
    }

    /** Signal 6 (redstone block, three Dampeners of stage 3: 15, 12, 9, 6) asks for one block: head at the cell after the barrel. */
    public static void doublePistonOneBlockForAWeakSignal(GameTestHelper h) {
        clear(h);
        BlockPos base = new BlockPos(1, 2, 4);
        doublePiston(h, base);
        h.setBlock(new BlockPos(1, 1, 0), Blocks.STONE);
        h.setBlock(new BlockPos(1, 2, 0), Blocks.REDSTONE_BLOCK);
        for (int z = 1; z <= 3; z++) {
            h.setBlock(new BlockPos(1, 1, z), Blocks.STONE);
            h.setBlock(new BlockPos(1, 2, z), dampener(3, Direction.NORTH));
        }
        sequence(h, List.of(new Step("ext 1 for signal 6", () -> { }, () -> {
            int signal = DoublePistons.signal(h.getLevel(), h.absolutePos(base), Direction.EAST);
            return signal == 6 && ext(h, base) == 1 && parts(h, base, 1);
        })));
    }

    /** Signal 15 (redstone block at the side) asks for two blocks: arm after the barrel, head behind it. */
    public static void doublePistonTwoBlocksForAStrongSignal(GameTestHelper h) {
        clear(h);
        BlockPos base = new BlockPos(1, 2, 4);
        doublePiston(h, base);
        h.setBlock(base.north(), Blocks.REDSTONE_BLOCK);
        h.assertValueEqual(DoublePistons.targetExt(0), 0, "target 0");
        h.assertValueEqual(DoublePistons.targetExt(1), 1, "target 1");
        h.assertValueEqual(DoublePistons.targetExt(7), 1, "target 7");
        h.assertValueEqual(DoublePistons.targetExt(8), 2, "target 8");
        h.assertValueEqual(DoublePistons.targetExt(15), 2, "target 15");
        sequence(h, List.of(new Step("ext 2 for signal 15", () -> { }, () -> ext(h, base) == 2 && parts(h, base, 2)
                && at(h, base.east()).getValue(DoublePistons.EXT) == 2)));
    }

    /** A stone in front is pushed one cell per step (1 then 2), and all parts go again when the signal ends. */
    public static void doublePistonPushesAndRetracts(GameTestHelper h) {
        clear(h);
        BlockPos base = new BlockPos(1, 2, 4);
        doublePiston(h, base);
        BlockPos stone = base.east(2);
        h.setBlock(stone.below(), Blocks.STONE);
        h.setBlock(stone, Blocks.STONE);
        h.setBlock(stone.east().below(), Blocks.STONE);
        h.setBlock(stone.east(2).below(), Blocks.STONE);
        sequence(h, List.of(
                new Step("strong signal: head out two, stone two cells away", () -> h.setBlock(base.north(), Blocks.REDSTONE_BLOCK),
                        () -> ext(h, base) == 2 && parts(h, base, 2) && at(h, base.east(4)).is(Blocks.STONE) && at(h, stone).is(RedstoneRegistry.PISTON_ARM)),
                new Step("signal gone: back to 0", () -> h.setBlock(base.north(), Blocks.AIR),
                        () -> ext(h, base) == 0 && parts(h, base, 0) && at(h, base.east(4)).is(Blocks.STONE) && at(h, base.east()).getValue(DoublePistons.EXT) == 0)));
    }

    /** Vanilla's resolver refuses all four parts (pushReaction IMMOVEABLE): no piston, no machine moves them. */
    public static void doublePistonCannotBePushed(GameTestHelper h) {
        clear(h);
        BlockPos base = new BlockPos(3, 2, 3);
        doublePiston(h, base);
        BlockPos abs = h.absolutePos(base);
        for (Block part : List.of(RedstoneRegistry.DOUBLE_PISTON, RedstoneRegistry.PISTON_BARREL, RedstoneRegistry.PISTON_HEAD, RedstoneRegistry.PISTON_ARM)) {
            h.assertTrue(part.defaultBlockState().getPistonPushReaction() == net.minecraft.world.level.material.PushReaction.IMMOVEABLE, part + " is movable");
        }
        // a Vanilla piston next to the base does not take it along
        h.setBlock(base.west().below(), Blocks.STONE);
        h.setBlock(base.west(), Blocks.PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.EAST));
        h.setBlock(base.west().north(), Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(10, () -> {
            h.assertTrue(at(h, base).is(RedstoneRegistry.DOUBLE_PISTON) && h.getLevel().getBlockState(abs).is(RedstoneRegistry.DOUBLE_PISTON), "base moved: " + at(h, base));
            h.assertTrue(at(h, base.west()).is(Blocks.PISTON) && !at(h, base.west()).getValue(PistonBaseBlock.EXTENDED), "the Vanilla piston extended against the base");
            h.succeed();
        });
    }

    /** Breaking any part removes the whole piston and drops one item (head and barrel included, even fully extended). */
    public static void doublePistonFallsAsAUnit(GameTestHelper h) {
        clear(h);
        BlockPos base = new BlockPos(1, 2, 4);
        doublePiston(h, base);
        h.setBlock(base.north(), Blocks.REDSTONE_BLOCK);
        sequence(h, List.of(
                new Step("fully out", () -> { }, () -> ext(h, base) == 2 && parts(h, base, 2)),
                new Step("head broken: everything gone", () -> h.getLevel().destroyBlock(h.absolutePos(base.east(3)), true),
                        () -> at(h, base).isAir() && at(h, base.east()).isAir() && at(h, base.east(2)).isAir() && at(h, base.east(3)).isAir()),
                new Step("one piston dropped", () -> { }, () -> {
                    int count = 0;
                    for (var e : h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(base)).inflate(8.0))) {
                        if (e.getItem().is(RedstoneRegistry.DOUBLE_PISTON_ITEM)) count += e.getItem().getCount();
                    }
                    return count == 1;
                })));
    }

    /** features.doublePiston off: the placed piston keeps still under any signal. */
    public static void doublePistonSwitchOffKeepsItStill(GameTestHelper h) {
        clear(h);
        BlockPos base = new BlockPos(1, 2, 4);
        doublePiston(h, base);
        RedstoneConfig.doublePiston = false;
        h.setBlock(base.north(), Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(12, () -> {
            try {
                h.assertTrue(ext(h, base) == 0 && parts(h, base, 0), "moved with the switch off: " + at(h, base));
            } finally {
                RedstoneConfig.doublePiston = true;
            }
            h.succeed();
        });
    }
}
