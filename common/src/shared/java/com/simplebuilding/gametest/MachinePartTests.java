package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.config.ServerTuning;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.machines.DoublePistons;
import com.simplebuilding.machines.MachineParts;
import com.simplebuilding.machines.SignalDampenerBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
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
 * Double Piston (IG31-26: one block, stage 1 head +1, stage 2 body +0.5 and head +1 more, three cells) and Signal Dampener
 * (stages 0-3, one redstone tick, side inputs ignored), moved from Simple Redstone into the core (IG31-12). Pushing follows
 * Vanilla's resolver: limit 12, obsidian stops it. Fabric adapter: {@code MachinePartGameTest} (8x8x8, default structure);
 * the old-id test needs SimpleLib's IdMoves (26.3).
 */
public final class MachinePartTests {
    private MachinePartTests() {}

    // ---------------------------------------------------------------- helpers

    /** Before 26.3 the parts do not exist ({@code McVersion.MACHINE_PARTS}): the test passes without doing anything. */
    private static boolean skip(GameTestHelper h) {
        if (com.simplebuilding.version.McVersion.MACHINE_PARTS) return false;
        h.succeed();
        return true;
    }

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
        h.setBlock(base, ModBlocks.DOUBLE_PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.EAST));
    }

    private static int ext(GameTestHelper h, BlockPos base) {
        BlockState a = at(h, base);
        return a.is(ModBlocks.DOUBLE_PISTON) ? a.getValue(DoublePistons.EXT) : -1;
    }

    private static boolean parts(GameTestHelper h, BlockPos base, int ext) {
        boolean head1 = at(h, base.east()).is(ModBlocks.DOUBLE_PISTON_HEAD);
        boolean arm = at(h, base.east()).is(ModBlocks.DOUBLE_PISTON_ARM);
        boolean head2 = at(h, base.east(2)).is(ModBlocks.DOUBLE_PISTON_HEAD);
        return switch (ext) {
            case 0 -> !head1 && !arm && !head2;
            case 1 -> head1 && !head2;
            default -> arm && head2;
        };
    }

    private static BlockState dampener(int stage, Direction input) {
        return ModBlocks.SIGNAL_DAMPENER.defaultBlockState().setValue(SignalDampenerBlock.FACING, input).setValue(SignalDampenerBlock.STAGE, stage);
    }

    private static List<Block> pistonParts() {
        return List.of(ModBlocks.DOUBLE_PISTON, ModBlocks.DOUBLE_PISTON_HEAD, ModBlocks.DOUBLE_PISTON_ARM);
    }

    // ---------------------------------------------------------------- registration and migration

    public static void registered(GameTestHelper h) {
        if (skip(h)) return;
        List<String> problems = new ArrayList<>();
        for (String name : List.of("double_piston", "double_piston_head", "double_piston_arm", "signal_dampener")) {
            if (!BuiltInRegistries.BLOCK.containsKey(Identifier.fromNamespaceAndPath("simplebuilding", name))) problems.add("block " + name);
        }
        for (String name : List.of("double_piston", "signal_dampener")) {
            if (!BuiltInRegistries.ITEM.containsKey(Identifier.fromNamespaceAndPath("simplebuilding", name))) problems.add("item " + name);
        }
        if (ModItems.DOUBLE_PISTON == null || ModItems.SIGNAL_DAMPENER == null) problems.add("ModItems fields");
        for (Block part : pistonParts()) {
            BlockState state = part.defaultBlockState();
            if (state.getPistonPushReaction() != com.simplebuilding.version.McVersion.PUSH_BLOCKED) problems.add("push reaction " + state);
            if (PistonBaseBlock.isPushable(state, h.getLevel(), BlockPos.ZERO.above(70), Direction.EAST, false, Direction.EAST)) problems.add("pushable " + state);
        }
        h.assertTrue(problems.isEmpty(), "registration problems: " + problems);
        h.succeed();
    }

    /** Worlds of the former module Simple Redstone: its ids resolve to the core's parts (the 1x2 barrel to air). */
    public static void oldIdsMove(GameTestHelper h) {
        if (skip(h)) return;
        List<String> problems = new ArrayList<>();
        String old = MachineParts.OLD_NAMESPACE;
        if (BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(old, "double_piston")) != ModItems.DOUBLE_PISTON) problems.add("item double_piston");
        if (BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(old, "signal_dampener")) != ModItems.SIGNAL_DAMPENER) problems.add("item signal_dampener");
        if (BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(old, "guide_book")) != ModItems.GUIDE_BOOK_MACHINES) problems.add("item guide_book");
        if (BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(old, "double_piston")) != ModBlocks.DOUBLE_PISTON) problems.add("block double_piston");
        if (BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(old, "double_piston_head")) != ModBlocks.DOUBLE_PISTON_HEAD) problems.add("block head");
        if (BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(old, "double_piston_arm")) != ModBlocks.DOUBLE_PISTON_ARM) problems.add("block arm");
        if (BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(old, "signal_dampener")) != ModBlocks.SIGNAL_DAMPENER) problems.add("block signal_dampener");
        if (BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(old, "double_piston_barrel")) != Blocks.AIR) problems.add("barrel is not air");
        h.assertTrue(problems.isEmpty(), "old Simple Redstone ids: " + problems);
        h.succeed();
    }

    public static void recipesCraft(GameTestHelper h) {
        if (skip(h)) return;
        var recipes = h.getLevel().getServer().getRecipeManager();
        ItemStack q = new ItemStack(Items.QUARTZ), r = new ItemStack(Items.REDSTONE_BLOCK), i = new ItemStack(Items.IRON_INGOT);
        ItemStack p = new ItemStack(Items.PISTON), b = new ItemStack(Items.IRON_BLOCK), e = ItemStack.EMPTY;
        var dp = CraftingInput.of(3, 3, List.of(q, r, q, i, p, i, b, p, e));
        ItemStack out = recipes.getRecipeFor(RecipeType.CRAFTING, dp, h.getLevel()).map(x -> x.value().assemble(dp)).orElse(ItemStack.EMPTY);
        h.assertTrue(out.is(ModItems.DOUBLE_PISTON) && out.getCount() == 1, "double piston recipe gave " + out);
        ItemStack t = new ItemStack(Items.REDSTONE_TORCH), s = new ItemStack(Items.STONE);
        var sd = CraftingInput.of(3, 2, List.of(t, q, t, s, s, s));
        out = recipes.getRecipeFor(RecipeType.CRAFTING, sd, h.getLevel()).map(x -> x.value().assemble(sd)).orElse(ItemStack.EMPTY);
        h.assertTrue(out.is(ModItems.SIGNAL_DAMPENER) && out.getCount() == 1, "dampener recipe gave " + out);
        h.succeed();
    }

    // ---------------------------------------------------------------- dampener

    /**
     * Output = max(0, input - stage) for every pair, and the owner's examples: 3 - 3 = 0, 4 - 2 = 2. In the world: a redstone block
     * (15) in front of the Dampener for the four stages, then a chain of Dampeners of stage 3 that brings the input down to 3 and 4.
     */
    public static void dampenerSubtractsTheStage(GameTestHelper h) {
        if (skip(h)) return;
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
        if (skip(h)) return;
        clear(h);
        BlockPos d = new BlockPos(3, 2, 3);
        h.setBlock(d.below(), Blocks.STONE);
        h.setBlock(d, dampener(1, Direction.WEST));
        sequence(h, List.of(
                new Step("input arrives, output still 0 with a tick scheduled", () -> h.setBlock(d.west(), Blocks.REDSTONE_BLOCK),
                        () -> at(h, d).getValue(SignalDampenerBlock.SIGNAL) == 0 && h.getLevel().getBlockTicks().hasScheduledTick(h.absolutePos(d), ModBlocks.SIGNAL_DAMPENER)),
                new Step("output 14 after the tick", () -> { }, () -> at(h, d).getValue(SignalDampenerBlock.SIGNAL) == 14)));
    }

    public static void dampenerStageCyclesOnRightClick(GameTestHelper h) {
        if (skip(h)) return;
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
        if (skip(h)) return;
        clear(h);
        BlockPos d = new BlockPos(3, 2, 3);
        h.setBlock(d.below(), Blocks.STONE);
        h.setBlock(d, dampener(0, Direction.WEST));
        h.setBlock(d.north(), Blocks.REDSTONE_BLOCK);
        h.setBlock(d.south(), Blocks.REDSTONE_BLOCK);
        h.setBlock(d.above(), Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(8, () -> {
            h.assertTrue(at(h, d).getValue(SignalDampenerBlock.SIGNAL) == 0, "side sources moved the output: " + at(h, d));
            h.assertFalse(h.getLevel().getBlockTicks().hasScheduledTick(h.absolutePos(d), ModBlocks.SIGNAL_DAMPENER), "a tick for a side input");
            h.succeed();
        });
    }

    /** server.features.signalDampener off: the next tick drops the output to 0 although the input is 15. Synchronous (global switch). */
    public static void dampenerSwitchOffOutputsNothing(GameTestHelper h) {
        if (skip(h)) return;
        clear(h);
        BlockPos d = new BlockPos(3, 2, 3);
        h.setBlock(d.below(), Blocks.STONE);
        h.setBlock(d.west(), Blocks.REDSTONE_BLOCK);
        h.setBlock(d, dampener(0, Direction.WEST).setValue(SignalDampenerBlock.SIGNAL, 15).setValue(SignalDampenerBlock.POWERED, true));
        boolean before = ServerTuning.get().features.signalDampener;
        ServerTuning.get().features.signalDampener = false;
        try {
            BlockPos abs = h.absolutePos(d);
            h.getLevel().getBlockState(abs).tick(h.getLevel(), abs, h.getLevel().getRandom());
            h.assertTrue(at(h, d).getValue(SignalDampenerBlock.SIGNAL) == 0 && !at(h, d).getValue(SignalDampenerBlock.POWERED), "switched off but outputs: " + at(h, d));
        } finally {
            ServerTuning.get().features.signalDampener = before;
        }
        h.succeed();
    }

    // ---------------------------------------------------------------- double piston

    /** The item places ONE block facing the clicker's opposite like a Vanilla piston, also with a block right in front. */
    public static void doublePistonPlacesOneCell(GameTestHelper h) {
        if (skip(h)) return;
        clear(h);
        h.setBlock(new BlockPos(1, 1, 3), Blocks.STONE);
        h.setBlock(new BlockPos(2, 2, 3), Blocks.STONE);
        ServerPlayer player = player(h);
        player.setYRot(90.0F); // looks west: the piston faces east like Vanilla's (towards the player's opposite)
        player.setYHeadRot(90.0F); // the view direction of a living entity follows its head
        player.setXRot(0.0F);
        ItemStack stack = new ItemStack(ModItems.DOUBLE_PISTON, 4);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = h.absolutePos(new BlockPos(1, 1, 3));
        var result = stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false)));
        BlockPos base = new BlockPos(1, 2, 3);
        h.assertTrue(result.consumesAction(), "not placed: " + result);
        BlockState a = at(h, base);
        h.assertTrue(a.is(ModBlocks.DOUBLE_PISTON) && a.getValue(DirectionalBlock.FACING) == Direction.EAST && a.getValue(DoublePistons.EXT) == 0, "placed: " + a);
        h.assertTrue(at(h, base.east()).is(Blocks.STONE), "the cell in front changed: " + at(h, base.east()));
        h.succeed();
    }

    /** Signal 6 (redstone block, three Dampeners of stage 3: 15, 12, 9, 6) asks for stage 1: Vanilla's reach, head in front. */
    public static void doublePistonStageOneForWeakSignal(GameTestHelper h) {
        if (skip(h)) return;
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
        if (skip(h)) return;
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
            var bounds = at(h, base).getShape(h.getLevel(), h.absolutePos(base)).bounds();
            return bounds.minX == 0.5 && bounds.maxX == 1.0;
        })));
    }

    /** A stone in front is pushed one cell per stage (two in all), and every part goes back when the signal ends. */
    public static void doublePistonPushesAndRetracts(GameTestHelper h) {
        if (skip(h)) return;
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

    /** From stage 2 a weaker signal (15 down to 6) takes it back one stage: head at the first cell, second cell free. */
    public static void doublePistonStepsBackOnWeakerSignal(GameTestHelper h) {
        if (skip(h)) return;
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
        if (skip(h)) return;
        clear(h);
        BlockPos base = new BlockPos(1, 3, 3);
        h.setBlock(base, ModBlocks.DOUBLE_PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.EAST));
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
        if (skip(h)) return;
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

    /** Vanilla's resolver refuses all three parts (pushReaction McVersion.PUSH_BLOCKED): a Vanilla piston next to the base stays in. */
    public static void doublePistonCannotBePushed(GameTestHelper h) {
        if (skip(h)) return;
        clear(h);
        BlockPos base = new BlockPos(3, 2, 3);
        doublePiston(h, base);
        for (Block part : pistonParts()) {
            h.assertTrue(part.defaultBlockState().getPistonPushReaction() == com.simplebuilding.version.McVersion.PUSH_BLOCKED, part + " is movable");
        }
        h.setBlock(base.west().below(), Blocks.STONE);
        h.setBlock(base.west(), Blocks.PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.EAST));
        h.setBlock(base.west().north(), Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(10, () -> {
            h.assertTrue(at(h, base).is(ModBlocks.DOUBLE_PISTON), "base moved: " + at(h, base));
            h.assertTrue(at(h, base.west()).is(Blocks.PISTON) && !at(h, base.west()).getValue(PistonBaseBlock.EXTENDED), "the Vanilla piston extended against the base");
            h.succeed();
        });
    }

    /** Breaking the head at stage 2 removes the whole piston and drops one item. */
    public static void doublePistonFallsAsUnit(GameTestHelper h) {
        if (skip(h)) return;
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
                        if (e.getItem().is(ModItems.DOUBLE_PISTON)) count += e.getItem().getCount();
                    }
                    return count == 1;
                })));
    }

    /** server.features.doublePiston off: a step does nothing. Synchronous on purpose (global switch, tests run side by side). */
    public static void doublePistonSwitchOffKeepsItStill(GameTestHelper h) {
        if (skip(h)) return;
        clear(h);
        BlockPos base = new BlockPos(1, 2, 4);
        doublePiston(h, base);
        h.setBlock(base.north(), Blocks.REDSTONE_BLOCK);
        BlockPos abs = h.absolutePos(base);
        boolean before = ServerTuning.get().features.doublePiston;
        ServerTuning.get().features.doublePiston = false;
        try {
            DoublePistons.step(h.getLevel(), abs, h.getLevel().getBlockState(abs));
            h.assertTrue(ext(h, base) == 0 && parts(h, base, 0), "moved with the switch off: " + at(h, base));
        } finally {
            ServerTuning.get().features.doublePiston = before;
        }
        h.succeed();
    }
}
