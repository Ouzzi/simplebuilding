package com.simplemachines.test;

import com.simplemachines.MachinesRegistry;
import com.simplemachines.SimpleMachines;
import com.simplemachines.block.SignalDampenerBlock;
import com.simplemachines.config.MachinesConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.material.PushReaction;
import com.simplemachines.piston.DoublePistonHeadBlock;
import com.simplemachines.piston.DoublePistons;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
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
 * Simple Machines (MACH32-MOD): the Double Piston (one block, stage 1 head +1, stage 2 body +0.5 and head +1 more; Vanilla's resolver: limit 12,
 * obsidian stops it; retracts, falls as a unit, cannot be pushed) and the Signal Dampener (stages 0-3, one redstone tick, side inputs ignored). Loader-neutral; the Fabric adapter
 * registers {@link #ALL} as module_game_test_<name> on {@code simplemachines:empty} (8x8x8), every test runs standalone.
 */
public final class MachinesTests {
    /** Test name -> body, in catalogue order (the Fabric adapter names them module_game_test_<name>). */
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();

    static {
        ALL.put("registered", MachinesTests::registered);
        ALL.put("old_ids_move", MachinesTests::oldIdsMove);
        ALL.put("sub_mod_switch", MachinesTests::subModSwitch);
        ALL.put("recipes_craft", MachinesTests::recipesCraft);
        ALL.put("switch_off_removes_recipe", MachinesTests::switchOffRemovesRecipe);
        ALL.put("dampener_subtracts_the_stage", MachinesTests::dampenerSubtractsTheStage);
        ALL.put("dampener_waits_one_redstone_tick", MachinesTests::dampenerWaitsOneRedstoneTick);
        ALL.put("dampener_stage_cycles_on_right_click", MachinesTests::dampenerStageCyclesOnRightClick);
        ALL.put("dampener_ignores_side_inputs", MachinesTests::dampenerIgnoresSideInputs);
        ALL.put("dampener_is_pushable", MachinesTests::dampenerIsPushable);
        ALL.put("dampener_switch_off_outputs_nothing", MachinesTests::dampenerSwitchOffOutputsNothing);
        ALL.put("double_piston_places_one_cell", MachinesTests::doublePistonPlacesOneCell);
        ALL.put("double_piston_stage_one_for_weak_signal", MachinesTests::doublePistonStageOneForWeakSignal);
        ALL.put("double_piston_stage_two_for_strong_signal", MachinesTests::doublePistonStageTwoForStrongSignal);
        ALL.put("double_piston_pushes_and_retracts", MachinesTests::doublePistonPushesAndRetracts);
        ALL.put("double_piston_steps_back_on_weaker_signal", MachinesTests::doublePistonStepsBackOnWeakerSignal);
        ALL.put("double_piston_push_limit", MachinesTests::doublePistonPushLimit);
        ALL.put("double_piston_stopped_by_obsidian", MachinesTests::doublePistonStoppedByObsidian);
        ALL.put("double_piston_cannot_be_pushed", MachinesTests::doublePistonCannotBePushed);
        ALL.put("double_piston_pushed_by_vanilla_piston", MachinesTests::doublePistonPushedByVanillaPiston);
        ALL.put("double_piston_falls_as_unit", MachinesTests::doublePistonFallsAsUnit);
        ALL.put("double_piston_switch_off_keeps_it_still", MachinesTests::doublePistonSwitchOffKeepsItStill);
        ALL.put("double_piston_all_facings", MachinesTests::doublePistonAllFacings);
        ALL.put("double_piston_moves_with_vanilla_blocks", MachinesTests::doublePistonMovesWithVanillaBlocks);
        ALL.put("double_piston_breaks_a_pot_with_its_item", MachinesTests::doublePistonBreaksAPotWithItsItem);
        ALL.put("discover_recipe_unlocks", com.simplemachines.discover.MachinesDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simplemachines.discover.MachinesDiscover::behaviourHints);
        ALL.put("guide_book", com.simplemachines.guide.MachinesGuide::gameTest);
    }

    private MachinesTests() {}

    // ---------------------------------------------------------------- helpers

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

    /** The Double Piston at {@code base}, facing +x (east), stage 0, on a stone block. */
    private static void doublePiston(GameTestHelper h, BlockPos base) {
        h.setBlock(base.below(), Blocks.STONE);
        h.setBlock(base, MachinesRegistry.DOUBLE_PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.EAST));
    }

    private static int ext(GameTestHelper h, BlockPos base) {
        BlockState a = at(h, base);
        return a.is(MachinesRegistry.DOUBLE_PISTON) ? a.getValue(DoublePistons.EXT) : -1;
    }

    private static boolean parts(GameTestHelper h, BlockPos base, int ext) {
        boolean head1 = at(h, base.east()).is(MachinesRegistry.head());
        boolean arm = at(h, base.east()).is(MachinesRegistry.arm());
        boolean head2 = at(h, base.east(2)).is(MachinesRegistry.head());
        return switch (ext) {
            case 0 -> !head1 && !arm && !head2;
            case 1 -> head1 && !head2;
            default -> arm && head2;
        };
    }

    private static BlockState dampener(int stage, Direction input) {
        return MachinesRegistry.SIGNAL_DAMPENER.defaultBlockState().setValue(SignalDampenerBlock.FACING, input).setValue(SignalDampenerBlock.STAGE, stage);
    }

    private static List<Block> pistonParts() {
        return List.of(MachinesRegistry.DOUBLE_PISTON, MachinesRegistry.head(), MachinesRegistry.arm());
    }

    // ---------------------------------------------------------------- registration and migration

    public static void registered(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        for (String name : List.of("double_piston", "double_piston_head", "double_piston_arm", "signal_dampener")) {
            if (!BuiltInRegistries.BLOCK.containsKey(SimpleMachines.id(name))) problems.add("block " + name);
        }
        for (String name : List.of("double_piston", "signal_dampener", "guide_book")) {
            if (!BuiltInRegistries.ITEM.containsKey(SimpleMachines.id(name))) problems.add("item " + name);
        }
        if (MachinesRegistry.TAB == null) problems.add("creative tab");
        for (Block part : List.of(MachinesRegistry.head(), MachinesRegistry.arm())) {
            BlockState state = part.defaultBlockState();
            if (state.getPistonPushReaction() != PushReaction.IMMOVEABLE) problems.add("push reaction " + state);
            if (PistonBaseBlock.isPushable(state, h.getLevel(), BlockPos.ZERO.above(70), Direction.EAST, false, Direction.EAST)) problems.add("pushable " + state);
        }
        h.assertTrue(problems.isEmpty(), "registration problems: " + problems);
        h.succeed();
    }

    /**
     * Worlds of the former holders: simpleredstone: (module before 31.10.) and simplebuilding: (core 31.10. to 01.11.) ids resolve to
     * simplemachines: (the 1x2 barrel to air, the old module's handbook to this module's).
     */
    public static void oldIdsMove(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        for (String old : List.of(SimpleMachines.REDSTONE_NAMESPACE, SimpleMachines.CORE_NAMESPACE)) {
            if (BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(old, "double_piston")) != MachinesRegistry.DOUBLE_PISTON_ITEM) problems.add(old + " item double_piston");
            if (BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(old, "signal_dampener")) != MachinesRegistry.SIGNAL_DAMPENER_ITEM) problems.add(old + " item signal_dampener");
            if (BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(old, "double_piston")) != MachinesRegistry.DOUBLE_PISTON) problems.add(old + " block double_piston");
            if (BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(old, "double_piston_head")) != MachinesRegistry.head()) problems.add(old + " block head");
            if (BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(old, "double_piston_arm")) != MachinesRegistry.arm()) problems.add(old + " block arm");
            if (BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(old, "signal_dampener")) != MachinesRegistry.SIGNAL_DAMPENER) problems.add(old + " block signal_dampener");
        }
        if (BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(SimpleMachines.REDSTONE_NAMESPACE, "guide_book")) != com.simplemachines.guide.MachinesGuide.book()) problems.add("old guide_book");
        if (BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(SimpleMachines.REDSTONE_NAMESPACE, "double_piston_barrel")) != Blocks.AIR) problems.add("barrel is not air");
        h.assertTrue(problems.isEmpty(), "old ids: " + problems);
        h.succeed();
    }

    public static void subModSwitch(GameTestHelper h) {
        com.simplebuilding.framework.api.SubMods.set(SimpleMachines.MOD_ID, false);
        try {
            h.assertFalse(SimpleMachines.on(), "switched off");
            h.assertTrue(MachinesRegistry.tabStacks().isEmpty() && MachinesRegistry.partStacks().isEmpty(), "no tab stacks while off");
            h.assertTrue(BuiltInRegistries.BLOCK.containsKey(SimpleMachines.id("double_piston")), "registrations stay");
        } finally {
            com.simplebuilding.framework.api.SubMods.set(SimpleMachines.MOD_ID, true);
        }
        h.assertTrue(SimpleMachines.on() && !MachinesRegistry.tabStacks().isEmpty(), "back on");
        h.succeed();
    }

    public static void recipesCraft(GameTestHelper h) {
        var recipes = h.getLevel().getServer().getRecipeManager();
        ItemStack q = new ItemStack(Items.QUARTZ), r = new ItemStack(Items.REDSTONE_BLOCK), i = new ItemStack(Items.IRON_INGOT);
        ItemStack p = new ItemStack(Items.PISTON), b = new ItemStack(Items.IRON_BLOCK), e = ItemStack.EMPTY;
        var dp = CraftingInput.of(3, 3, List.of(q, r, q, i, p, i, b, p, e));
        ItemStack out = recipes.getRecipeFor(RecipeType.CRAFTING, dp, h.getLevel()).map(x -> x.value().assemble(dp)).orElse(ItemStack.EMPTY);
        h.assertTrue(out.is(MachinesRegistry.DOUBLE_PISTON_ITEM) && out.getCount() == 1, "double piston recipe gave " + out);
        ItemStack t = new ItemStack(Items.REDSTONE_TORCH), s = new ItemStack(Items.STONE);
        var sd = CraftingInput.of(3, 2, List.of(t, q, t, s, s, s));
        out = recipes.getRecipeFor(RecipeType.CRAFTING, sd, h.getLevel()).map(x -> x.value().assemble(sd)).orElse(ItemStack.EMPTY);
        h.assertTrue(out.is(MachinesRegistry.SIGNAL_DAMPENER_ITEM) && out.getCount() == 1, "dampener recipe gave " + out);
        h.succeed();
    }

    // ---------------------------------------------------------------- dampener

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
                        () -> at(h, d).getValue(SignalDampenerBlock.SIGNAL) == 0 && h.getLevel().getBlockTicks().hasScheduledTick(h.absolutePos(d), MachinesRegistry.SIGNAL_DAMPENER)),
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
            h.assertFalse(h.getLevel().getBlockTicks().hasScheduledTick(h.absolutePos(d), MachinesRegistry.SIGNAL_DAMPENER), "a tick for a side input");
            h.succeed();
        });
    }

    /** IG: the Dampener is a plain movable block (no lever): a Vanilla piston pushes it one cell and it keeps its state. */
    public static void dampenerIsPushable(GameTestHelper h) {
        clear(h);
        BlockPos d = new BlockPos(3, 2, 3);
        for (int x = 0; x <= 6; x++) h.setBlock(new BlockPos(x, 1, 3), Blocks.STONE);
        h.setBlock(d, dampener(2, Direction.WEST));
        h.setBlock(d.west(), Blocks.PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.EAST));
        h.setBlock(d.west().north(), Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(14, () -> {
            h.assertTrue(at(h, d.east()).is(MachinesRegistry.SIGNAL_DAMPENER) && at(h, d.east()).getValue(SignalDampenerBlock.STAGE) == 2,
                    "dampener not pushed: " + at(h, d.east()) + " / " + at(h, d));
            h.succeed();
        });
    }

    /** A switched-off feature takes its recipe out of the table (SimpleLib RecipeFilters), the other recipes stay. */
    public static void switchOffRemovesRecipe(GameTestHelper h) {
        var dp = net.minecraft.resources.Identifier.fromNamespaceAndPath("simplemachines", "double_piston");
        var sd = net.minecraft.resources.Identifier.fromNamespaceAndPath("simplemachines", "signal_dampener");
        boolean before = MachinesConfig.doublePiston;
        h.assertTrue(!com.simplelib.api.RecipeFilters.removes(dp) && !com.simplelib.api.RecipeFilters.removes(sd), "switches on: recipes stay");
        MachinesConfig.doublePiston = false;
        try {
            h.assertTrue(com.simplelib.api.RecipeFilters.removes(dp), "double piston off: its recipe goes");
            h.assertTrue(!com.simplelib.api.RecipeFilters.removes(sd), "double piston off: dampener recipe stays");
            h.assertTrue(!com.simplelib.api.RecipeFilters.removes(net.minecraft.resources.Identifier.fromNamespaceAndPath("other", "double_piston")), "other namespaces stay");
        } finally {
            MachinesConfig.doublePiston = before;
        }
        h.succeed();
    }

    /** server.features.signalDampener off: the next tick drops the output to 0 although the input is 15. Synchronous (global switch). */
    public static void dampenerSwitchOffOutputsNothing(GameTestHelper h) {
        clear(h);
        BlockPos d = new BlockPos(3, 2, 3);
        h.setBlock(d.below(), Blocks.STONE);
        h.setBlock(d.west(), Blocks.REDSTONE_BLOCK);
        h.setBlock(d, dampener(0, Direction.WEST).setValue(SignalDampenerBlock.SIGNAL, 15).setValue(SignalDampenerBlock.POWERED, true));
        boolean before = MachinesConfig.signalDampener;
        MachinesConfig.signalDampener = false;
        try {
            BlockPos abs = h.absolutePos(d);
            h.getLevel().getBlockState(abs).tick(h.getLevel(), abs, h.getLevel().getRandom());
            h.assertTrue(at(h, d).getValue(SignalDampenerBlock.SIGNAL) == 0 && !at(h, d).getValue(SignalDampenerBlock.POWERED), "switched off but outputs: " + at(h, d));
        } finally {
            MachinesConfig.signalDampener = before;
        }
        h.succeed();
    }

    // ---------------------------------------------------------------- double piston

    /** The item places ONE block facing the clicker's opposite like a Vanilla piston, also with a block right in front. */
    public static void doublePistonPlacesOneCell(GameTestHelper h) {
        clear(h);
        h.setBlock(new BlockPos(1, 1, 3), Blocks.STONE);
        h.setBlock(new BlockPos(2, 2, 3), Blocks.STONE);
        ServerPlayer player = player(h);
        player.setYRot(90.0F); // looks west: the piston faces east like Vanilla's (towards the player's opposite)
        player.setYHeadRot(90.0F); // the view direction of a living entity follows its head
        player.setXRot(0.0F);
        ItemStack stack = new ItemStack(MachinesRegistry.DOUBLE_PISTON_ITEM, 4);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = h.absolutePos(new BlockPos(1, 1, 3));
        var result = stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false)));
        BlockPos base = new BlockPos(1, 2, 3);
        h.assertTrue(result.consumesAction(), "not placed: " + result);
        BlockState a = at(h, base);
        h.assertTrue(a.is(MachinesRegistry.DOUBLE_PISTON) && a.getValue(DirectionalBlock.FACING) == Direction.EAST && a.getValue(DoublePistons.EXT) == 0, "placed: " + a);
        h.assertTrue(at(h, base.east()).is(Blocks.STONE), "the cell in front changed: " + at(h, base.east()));
        h.succeed();
    }

    /** Signal 6 (redstone block, three Dampeners of stage 3: 15, 12, 9, 6) asks for stage 1: Vanilla's reach, head in front. */
    public static void doublePistonStageOneForWeakSignal(GameTestHelper h) {
        clear(h);
        BlockPos base = new BlockPos(1, 2, 4);
        doublePiston(h, base);
        h.setBlock(new BlockPos(1, 1, 0), Blocks.STONE);
        h.setBlock(new BlockPos(1, 2, 0), Blocks.REDSTONE_BLOCK);
        for (int z = 1; z <= 3; z++) {
            h.setBlock(new BlockPos(1, 1, z), Blocks.STONE);
            h.setBlock(new BlockPos(1, 2, z), dampener(3, Direction.NORTH));
        }
        sequence(h, List.of(new Step("stage 1 for signal 6", () -> { }, () -> {
            int signal = DoublePistons.signal(h.getLevel(), h.absolutePos(base), Direction.EAST);
            return signal == 6 && ext(h, base) == 1 && parts(h, base, 1);
        })));
    }

    /**
     * Signal 15 (redstone block at the side) asks for stage 2: arm in front, head two cells out, and the body moved half a block
     * (its shape covers only the front half of its own cell).
     */
    public static void doublePistonStageTwoForStrongSignal(GameTestHelper h) {
        clear(h);
        BlockPos base = new BlockPos(1, 2, 4);
        doublePiston(h, base);
        h.setBlock(base.north(), Blocks.REDSTONE_BLOCK);
        h.assertValueEqual(DoublePistons.targetExt(0), 0, "target 0");
        h.assertValueEqual(DoublePistons.targetExt(1), 1, "target 1");
        h.assertValueEqual(DoublePistons.targetExt(7), 1, "target 7");
        h.assertValueEqual(DoublePistons.targetExt(8), 2, "target 8");
        h.assertValueEqual(DoublePistons.targetExt(15), 2, "target 15");
        sequence(h, List.of(new Step("stage 2 for signal 15", () -> { }, () -> {
            if (ext(h, base) != 2 || !parts(h, base, 2)) return false;
            // DW04C-6: Vanilla's extended base (12 px, front 4 px free), no longer the body moved half a block
            var bounds = at(h, base).getShape(h.getLevel(), h.absolutePos(base)).bounds();
            return bounds.minX == 0.0 && bounds.maxX == 0.75;
        })));
    }

    /** A stone in front is pushed one cell per stage (two in all), and every part goes back when the signal ends. */
    public static void doublePistonPushesAndRetracts(GameTestHelper h) {
        clear(h);
        BlockPos base = new BlockPos(1, 2, 4);
        doublePiston(h, base);
        BlockPos stone = base.east();
        for (int i = 1; i <= 3; i++) h.setBlock(base.east(i).below(), Blocks.STONE);
        h.setBlock(stone, Blocks.STONE);
        sequence(h, List.of(
                new Step("strong signal: two stages, stone three cells from the base", () -> h.setBlock(base.north(), Blocks.REDSTONE_BLOCK),
                        () -> ext(h, base) == 2 && parts(h, base, 2) && at(h, base.east(3)).is(Blocks.STONE)),
                new Step("signal gone: back to 0, the stone stays", () -> h.setBlock(base.north(), Blocks.AIR),
                        () -> ext(h, base) == 0 && parts(h, base, 0) && at(h, base.east(3)).is(Blocks.STONE) && at(h, base.east()).isAir())));
    }

    /**
     * Audit b-audsb31-a (simplemachines-P2-1): a block the double piston breaks (a decorated pot) goes like under Vanilla's piston -
     * the pot drops and so does the item inside it (before, the moving head replaced the cell without block entity side effects).
     */
    public static void doublePistonBreaksAPotWithItsItem(GameTestHelper h) {
        clear(h);
        BlockPos base = new BlockPos(1, 2, 4);
        doublePiston(h, base);
        h.setBlock(base.east().below(), Blocks.STONE);
        h.setBlock(base.east(), Blocks.DECORATED_POT);
        h.getBlockEntity(base.east(), net.minecraft.world.level.block.entity.DecoratedPotBlockEntity.class)
                .setTheItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 3));
        sequence(h, List.of(
                new Step("signal: the head breaks the pot, its diamonds drop", () -> h.setBlock(base.north(), Blocks.REDSTONE_BLOCK),
                        () -> ext(h, base) >= 1 && !at(h, base.east(2)).is(Blocks.DECORATED_POT)
                                && dropped(h, net.minecraft.world.item.Items.DIAMOND) == 3)));
    }

    private static int dropped(GameTestHelper h, net.minecraft.world.item.Item item) {
        int n = 0;
        for (net.minecraft.world.entity.item.ItemEntity e : h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(h.absolutePos(BlockPos.ZERO)).expandTowards(8, 8, 8), e -> e.getItem().is(item))) n += e.getItem().getCount();
        return n;
    }

    /** From stage 2 a weaker signal (15 down to 6) takes it back one stage: head at the first cell, second cell free. */
    public static void doublePistonStepsBackOnWeakerSignal(GameTestHelper h) {
        clear(h);
        BlockPos base = new BlockPos(1, 2, 4);
        doublePiston(h, base);
        for (int z = 0; z <= 3; z++) h.setBlock(new BlockPos(1, 1, z), Blocks.STONE);
        sequence(h, List.of(
                new Step("stage 2", () -> h.setBlock(base.north(), Blocks.REDSTONE_BLOCK), () -> ext(h, base) == 2 && parts(h, base, 2)),
                new Step("signal 6: stage 1", () -> {
                    h.setBlock(new BlockPos(1, 2, 0), Blocks.REDSTONE_BLOCK);
                    for (int z = 1; z <= 3; z++) h.setBlock(new BlockPos(1, 2, z), dampener(3, Direction.NORTH));
                }, () -> ext(h, base) == 1 && parts(h, base, 1) && at(h, base.east(2)).isAir()
                        && DoublePistons.signal(h.getLevel(), h.absolutePos(base), Direction.EAST) == 6)));
    }

    /**
     * Vanilla's push limit of 12: a floating cluster of 13 slime blocks in front keeps the piston in; with 12 it moves both stages.
     * The redstone block sits below the base, out of the slime's reach.
     */
    public static void doublePistonPushLimit(GameTestHelper h) {
        clear(h);
        BlockPos base = new BlockPos(1, 3, 3);
        h.setBlock(base, MachinesRegistry.DOUBLE_PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.EAST));
        for (int x = 2; x <= 3; x++) {
            for (int y = 3; y <= 4; y++) {
                for (int z = 2; z <= 4; z++) h.setBlock(new BlockPos(x, y, z), Blocks.SLIME_BLOCK);
            }
        }
        BlockPos thirteenth = new BlockPos(2, 5, 3);
        h.setBlock(thirteenth, Blocks.SLIME_BLOCK);
        h.setBlock(base.below(), Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(10, () -> {
            h.assertTrue(ext(h, base) == 0 && at(h, new BlockPos(2, 3, 3)).is(Blocks.SLIME_BLOCK), "13 blocks moved: stage " + ext(h, base));
            h.setBlock(thirteenth, Blocks.AIR);
            h.tickBlock(base);
            h.succeedWhen(() -> {
                h.assertTrue(ext(h, base) == 2 && parts(h, base, 2), "12 blocks: stage " + ext(h, base));
                h.assertTrue(at(h, new BlockPos(4, 3, 3)).is(Blocks.SLIME_BLOCK) && at(h, new BlockPos(5, 4, 4)).is(Blocks.SLIME_BLOCK), "cluster not moved two cells");
            });
        });
    }

    /** Obsidian stops it like a Vanilla piston: stage 1 pushes the stone next to the obsidian, stage 2 cannot go on. */
    public static void doublePistonStoppedByObsidian(GameTestHelper h) {
        clear(h);
        BlockPos base = new BlockPos(1, 2, 3);
        doublePiston(h, base);
        for (int i = 1; i <= 3; i++) h.setBlock(base.east(i).below(), Blocks.STONE);
        h.setBlock(base.east(), Blocks.STONE);
        h.setBlock(base.east(3), Blocks.OBSIDIAN);
        h.setBlock(base.north(), Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(20, () -> {
            h.assertTrue(ext(h, base) == 1 && parts(h, base, 1), "stage with obsidian ahead: " + ext(h, base));
            h.assertTrue(at(h, base.east(2)).is(Blocks.STONE) && at(h, base.east(3)).is(Blocks.OBSIDIAN), "stone/obsidian moved");
            h.succeed();
        });
    }

    /**
     * MACH32-PUSH: head and arm stay immovable (Vanilla's piston head), the base is movable only while it is in (stage 0), like
     * Vanilla's retracted piston; out (stage 1, 2) the resolver refuses it like an extended Vanilla piston.
     */
    public static void doublePistonCannotBePushed(GameTestHelper h) {
        clear(h);
        BlockPos base = new BlockPos(3, 2, 3);
        doublePiston(h, base);
        for (Block part : List.of(MachinesRegistry.head(), MachinesRegistry.arm())) {
            h.assertTrue(part.defaultBlockState().getPistonPushReaction() == PushReaction.IMMOVEABLE, part + " is movable");
        }
        BlockState in = MachinesRegistry.DOUBLE_PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.EAST);
        BlockPos abs = h.absolutePos(base);
        h.assertTrue(PistonBaseBlock.isPushable(in, h.getLevel(), abs, Direction.EAST, false, Direction.EAST), "stage 0 is not pushable");
        for (int ext = 1; ext <= 2; ext++) {
            BlockState out = in.setValue(DoublePistons.EXT, ext);
            h.assertTrue(!PistonBaseBlock.isPushable(out, h.getLevel(), abs, Direction.EAST, false, Direction.EAST), "stage " + ext + " is pushable");
            h.setBlock(base, out);
            h.assertTrue(!new PistonStructureResolver(h.getLevel(), abs.west(), Direction.EAST, true).resolve(), "a piston resolves a push of stage " + ext);
        }
        h.succeed();
    }

    /** MACH32-PUSH: a powered Vanilla piston next to a Double Piston at stage 0 pushes it one cell, still stage 0 and facing the same way. */
    public static void doublePistonPushedByVanillaPiston(GameTestHelper h) {
        clear(h);
        BlockPos base = new BlockPos(3, 2, 3);
        doublePiston(h, base);
        h.setBlock(base.east().below(), Blocks.STONE);
        h.setBlock(base.west().below(), Blocks.STONE);
        h.setBlock(base.west(), Blocks.PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.EAST));
        h.setBlock(base.west().north(), Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(20, () -> {
            BlockState moved = at(h, base.east());
            h.assertTrue(moved.is(MachinesRegistry.DOUBLE_PISTON) && moved.getValue(DirectionalBlock.FACING) == Direction.EAST && moved.getValue(DoublePistons.EXT) == 0,
                    "the Double Piston was not pushed one cell east: " + moved + " / " + at(h, base));
            h.assertTrue(at(h, base.west()).is(Blocks.PISTON) && at(h, base.west()).getValue(PistonBaseBlock.EXTENDED), "the Vanilla piston did not extend");
            h.succeed();
        });
    }

    /** Breaking the head at stage 2 removes the whole piston and drops one item. */
    public static void doublePistonFallsAsUnit(GameTestHelper h) {
        clear(h);
        BlockPos base = new BlockPos(1, 2, 4);
        doublePiston(h, base);
        h.setBlock(base.north(), Blocks.REDSTONE_BLOCK);
        sequence(h, List.of(
                new Step("fully out", () -> { }, () -> ext(h, base) == 2 && parts(h, base, 2)),
                new Step("head broken: everything gone", () -> h.getLevel().destroyBlock(h.absolutePos(base.east(2)), true),
                        () -> at(h, base).isAir() && at(h, base.east()).isAir() && at(h, base.east(2)).isAir()),
                new Step("one piston dropped", () -> { }, () -> {
                    int count = 0;
                    for (var e : h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(base)).inflate(8.0))) {
                        if (e.getItem().is(MachinesRegistry.DOUBLE_PISTON_ITEM)) count += e.getItem().getCount();
                    }
                    return count == 1;
                })));
    }

    /** server.features.doublePiston off: a step does nothing. Synchronous on purpose (global switch, tests run side by side). */
    public static void doublePistonSwitchOffKeepsItStill(GameTestHelper h) {
        clear(h);
        BlockPos base = new BlockPos(1, 2, 4);
        doublePiston(h, base);
        h.setBlock(base.north(), Blocks.REDSTONE_BLOCK);
        BlockPos abs = h.absolutePos(base);
        boolean before = MachinesConfig.doublePiston;
        MachinesConfig.doublePiston = false;
        try {
            DoublePistons.step(h.getLevel(), abs, h.getLevel().getBlockState(abs));
            h.assertTrue(ext(h, base) == 0 && parts(h, base, 0), "moved with the switch off: " + at(h, base));
        } finally {
            MachinesConfig.doublePiston = before;
        }
        h.succeed();
    }

    /**
     * DW04C-6: all six facings at stage 2 - the base as Vanilla's extended base (12 px, the 4 px at its front free), the arm and the
     * head one and two cells out with the base's facing - and all back to stage 0 when the signal ends.
     */
    public static void doublePistonAllFacings(GameTestHelper h) {
        clear(h);
        Map<Direction, BlockPos> bases = new java.util.EnumMap<>(Direction.class);
        bases.put(Direction.UP, new BlockPos(1, 1, 1));
        bases.put(Direction.DOWN, new BlockPos(3, 5, 1));
        bases.put(Direction.NORTH, new BlockPos(5, 1, 4));
        bases.put(Direction.SOUTH, new BlockPos(1, 1, 4));
        bases.put(Direction.EAST, new BlockPos(2, 4, 6));
        bases.put(Direction.WEST, new BlockPos(6, 1, 7));
        bases.forEach((f, b) -> h.setBlock(b, MachinesRegistry.DOUBLE_PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, f)));
        sequence(h, List.of(
                new Step("power behind every base: all at stage 2", () -> bases.forEach((f, b) -> h.setBlock(b.relative(f.getOpposite()), Blocks.REDSTONE_BLOCK)),
                        () -> bases.entrySet().stream().allMatch(e -> stageTwoRight(h, e.getValue(), e.getKey()))),
                new Step("power gone: all back to 0, nothing left in front", () -> bases.forEach((f, b) -> h.setBlock(b.relative(f.getOpposite()), Blocks.AIR)),
                        () -> bases.entrySet().stream().allMatch(e -> ext(h, e.getValue()) == 0
                                && at(h, e.getValue().relative(e.getKey())).isAir() && at(h, e.getValue().relative(e.getKey(), 2)).isAir()))));
    }

    private static boolean stageTwoRight(GameTestHelper h, BlockPos base, Direction f) {
        BlockState arm = at(h, base.relative(f));
        BlockState head = at(h, base.relative(f, 2));
        if (ext(h, base) != 2 || !arm.is(MachinesRegistry.arm()) || !head.is(MachinesRegistry.head())
                || arm.getValue(DirectionalBlock.FACING) != f || head.getValue(DirectionalBlock.FACING) != f
                || at(h, base).getValue(DirectionalBlock.FACING) != f) {
            return false;
        }
        var bounds = at(h, base).getShape(h.getLevel(), h.absolutePos(base)).bounds();
        double front = f.getAxisDirection() == Direction.AxisDirection.POSITIVE ? bounds.max(f.getAxis()) : 1.0 - bounds.min(f.getAxis());
        double volume = bounds.getXsize() * bounds.getYsize() * bounds.getZsize();
        return front == 0.75 && volume == 0.75;
    }

    /**
     * DW04C-6 (smooth like Vanilla): every step runs over Vanilla's moving block - going out the head is the moving block, going back
     * from 2 to 1 the head with the arm's sleeve, from 1 to 0 the base itself (source piston, like Vanilla), whose collision keeps the
     * base (DoublePistonMovingMixin); at rest the head carries neither short nor sleeve.
     */
    public static void doublePistonMovesWithVanillaBlocks(GameTestHelper h) {
        clear(h);
        BlockPos base = new BlockPos(1, 2, 4);
        doublePiston(h, base);
        sequence(h, List.of(
                new Step("out: the head moves as Vanilla's moving block", () -> h.setBlock(base.west(), Blocks.REDSTONE_BLOCK),
                        () -> movingBe(h, base.east()) instanceof PistonMovingBlockEntity m && m.isExtending() && m.getMovedState().is(MachinesRegistry.head())),
                new Step("stage 2 at rest, head without short and sleeve", () -> { },
                        () -> ext(h, base) == 2 && parts(h, base, 2) && !at(h, base.east(2)).getValue(DoublePistonHeadBlock.SHORT)
                                && !at(h, base.east(2)).getValue(DoublePistonHeadBlock.SLEEVE)),
                new Step("in 2 -> 1: the head with the sleeve slides over the arm's cell", () -> h.setBlock(base.west(), Blocks.AIR),
                        () -> movingBe(h, base.east()) instanceof PistonMovingBlockEntity m && !m.isExtending() && !m.isSourcePiston()
                                && m.getMovedState().is(MachinesRegistry.head()) && m.getMovedState().getValue(DoublePistonHeadBlock.SLEEVE)),
                new Step("in 1 -> 0: the base is the moving source block and keeps its collision", () -> { },
                        () -> movingBe(h, base) instanceof PistonMovingBlockEntity m && m.isSourcePiston() && !m.isExtending()
                                && m.getMovedState().is(MachinesRegistry.DOUBLE_PISTON)
                                && at(h, base).getCollisionShape(h.getLevel(), h.absolutePos(base)).toAabbs().stream().anyMatch(b -> b.contains(0.05, 0.05, 0.05))),
                new Step("back at 0: the base is back, nothing in front", () -> { },
                        () -> ext(h, base) == 0 && parts(h, base, 0) && at(h, base.east()).isAir())));
    }

    private static net.minecraft.world.level.block.entity.BlockEntity movingBe(GameTestHelper h, BlockPos pos) {
        return at(h, pos).is(Blocks.MOVING_PISTON) ? h.getLevel().getBlockEntity(h.absolutePos(pos)) : null;
    }
}
