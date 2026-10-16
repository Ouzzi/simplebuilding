package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.AbstractLightPaneBlock;
import com.simplebuilding.blocks.custom.CopperLightPaneBlock;
import com.simplebuilding.blocks.custom.LightPaneBlock;
import com.simplebuilding.blocks.custom.RedstoneLightPaneBlock;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Light Panes (QUEUE N57, docs/ai/PLAN-LIGHT-PANE-2026-10-14.md): placing on every side, falling off without a support, water,
 * lamp light and the redstone switch, the multicolor pane's color per signal strength and its right-click cycle, recipes.
 * Loader-neutral; without {@link McVersion#LIGHT_PANES} (26.2) the tests pass without a check.
 */
public final class LightPaneTests {
    private static final BlockPos CENTER = new BlockPos(2, 2, 2);

    private LightPaneTests() {
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 corner = helper.absoluteVec(new Vec3(7.5, 1.0, 7.5));
        player.setPos(corner.x, corner.y, corner.z);
        player.setShiftKeyDown(false);
        return player;
    }

    private static net.minecraft.world.InteractionResult place(GameTestHelper helper, ServerPlayer player, Item item, BlockPos on, Direction face) {
        ItemStack stack = new ItemStack(item, 8);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = helper.absolutePos(on);
        Vec3 hit = Vec3.atCenterOf(abs).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        return stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, abs, false)));
    }

    private static void clear(GameTestHelper helper) {
        for (int x = 0; x <= 4; x++) {
            for (int y = 0; y <= 4; y++) {
                for (int z = 0; z <= 4; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
    }

    private static String id(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getPath();
    }

    /** Every pane places on every side of a block, faces away from it, is 1 px thick against it, and has no collision. */
    public static void panesPlaceOnEverySide(GameTestHelper helper) {
        if (!McVersion.LIGHT_PANES) {
            helper.succeed();
            return;
        }
        helper.assertValueEqual(ModBlocks.lightPanes().size(), 20, "number of light panes");
        ServerPlayer player = player(helper);
        List<String> problems = new ArrayList<>();
        for (Block pane : ModBlocks.lightPanes()) {
            for (Direction face : Direction.values()) {
                clear(helper);
                helper.setBlock(CENTER, Blocks.STONE);
                var result = place(helper, player, pane.asItem(), CENTER, face);
                BlockPos at = CENTER.relative(face);
                BlockState state = helper.getBlockState(at);
                String where = id(pane) + " on " + face;
                if (!result.consumesAction() || !state.is(pane)) {
                    problems.add(where + ": not placed (" + result + ", " + state + ")");
                    continue;
                }
                if (state.getValue(AbstractLightPaneBlock.FACING) != face) {
                    problems.add(where + ": faces " + state.getValue(AbstractLightPaneBlock.FACING));
                }
                BlockPos abs = helper.absolutePos(at);
                AABB box = state.getShape(helper.getLevel(), abs).bounds();
                Direction.Axis axis = face.getAxis();
                double thickness = box.max(axis) - box.min(axis);
                boolean touching = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? box.min(axis) == 0.0 : box.max(axis) == 1.0;
                if (Math.abs(thickness - 1.0 / 16.0) > 1.0E-6 || !touching) {
                    problems.add(where + ": box " + box);
                }
                for (Direction.Axis other : Direction.Axis.values()) {
                    if (other != axis && Math.abs(box.max(other) - box.min(other) - 1.0) > 1.0E-6) {
                        problems.add(where + ": not a full 16 px face: " + box);
                    }
                }
                if (!state.getCollisionShape(helper.getLevel(), abs).isEmpty()) {
                    problems.add(where + ": has collision");
                }
            }
        }
        helper.assertTrue(problems.isEmpty(), problems.size() + " placement problem(s): " + String.join("; ", problems));
        helper.succeed();
    }

    /** No pane on a fence, a glass pane, the side or half-high top of a slab; a full face is required (the top of a top slab works). */
    public static void panesNeedFullSupportFace(GameTestHelper helper) {
        if (!McVersion.LIGHT_PANES) {
            helper.succeed();
            return;
        }
        ServerPlayer player = player(helper);
        Item pane = ModBlocks.LIGHT_PANE.asItem();
        clear(helper);
        helper.setBlock(CENTER, Blocks.OAK_FENCE);
        place(helper, player, pane, CENTER, Direction.UP);
        helper.assertFalse(helper.getBlockState(CENTER.above()).is(ModBlocks.LIGHT_PANE), "a pane on a fence post");
        helper.setBlock(CENTER, Blocks.GLASS_PANE);
        place(helper, player, pane, CENTER, Direction.EAST);
        helper.assertFalse(helper.getBlockState(CENTER.east()).is(ModBlocks.LIGHT_PANE), "a pane on a glass pane");
        helper.setBlock(CENTER, Blocks.STONE_SLAB);
        place(helper, player, pane, CENTER, Direction.NORTH);
        helper.assertFalse(helper.getBlockState(CENTER.north()).is(ModBlocks.LIGHT_PANE), "a pane on the side of a slab");
        place(helper, player, pane, CENTER, Direction.UP);
        helper.assertFalse(helper.getBlockState(CENTER.above()).is(ModBlocks.LIGHT_PANE), "a pane on the half-high top of a bottom slab");
        helper.setBlock(CENTER, Blocks.STONE_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE,
                net.minecraft.world.level.block.state.properties.SlabType.TOP));
        place(helper, player, pane, CENTER, Direction.UP);
        helper.assertTrue(helper.getBlockState(CENTER.above()).is(ModBlocks.LIGHT_PANE), "no pane on the full top of a top slab");
        helper.succeed();
    }

    /** Without its support block a pane drops as an item (all six sides); another full block behind keeps it. */
    public static void panesFallOffWithoutSupport(GameTestHelper helper) {
        if (!McVersion.LIGHT_PANES) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        List<String> problems = new ArrayList<>();
        for (Direction face : Direction.values()) {
            clear(helper);
            for (ItemEntity old : level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(CENTER)).inflate(6.0))) {
                old.discard();
            }
            BlockPos at = CENTER.relative(face);
            helper.setBlock(CENTER, Blocks.STONE);
            helper.setBlock(at, ModBlocks.LIGHT_PANE.defaultBlockState().setValue(AbstractLightPaneBlock.FACING, face));
            helper.setBlock(CENTER, Blocks.DIRT);
            if (!helper.getBlockState(at).is(ModBlocks.LIGHT_PANE)) {
                problems.add(face + ": gone when the support was replaced by another full block");
            }
            helper.setBlock(CENTER, Blocks.AIR);
            if (!helper.getBlockState(at).isAir()) {
                problems.add(face + ": still there without a support: " + helper.getBlockState(at));
            }
            boolean dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(CENTER)).inflate(3.0))
                    .stream().anyMatch(e -> e.getItem().is(ModBlocks.LIGHT_PANE.asItem()));
            if (!dropped) {
                problems.add(face + ": did not drop as an item");
            }
        }
        helper.assertTrue(problems.isEmpty(), "fall-off problems: " + String.join("; ", problems));
        helper.succeed();
    }

    /** Placed into water a pane is waterlogged; the water stays a source; breaking drops the pane. */
    public static void panesWaterlog(GameTestHelper helper) {
        if (!McVersion.LIGHT_PANES) {
            helper.succeed();
            return;
        }
        ServerPlayer player = player(helper);
        clear(helper);
        helper.setBlock(CENTER, Blocks.STONE);
        helper.setBlock(CENTER.above(), Blocks.WATER);
        place(helper, player, ModBlocks.TRANSPARENT_LIGHT_PANE.asItem(), CENTER, Direction.UP);
        BlockState state = helper.getBlockState(CENTER.above());
        helper.assertTrue(state.is(ModBlocks.TRANSPARENT_LIGHT_PANE) && state.getValue(AbstractLightPaneBlock.WATERLOGGED), "pane in water is not waterlogged: " + state);
        helper.assertTrue(state.getFluidState().isSource(), "waterlogged pane has no water source");
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(CENTER.above())).getFluidState().isSource(), "level state not waterlogged");
        place(helper, player, ModBlocks.LIGHT_PANE.asItem(), CENTER, Direction.EAST);
        helper.assertFalse(helper.getBlockState(CENTER.east()).getValue(AbstractLightPaneBlock.WATERLOGGED), "dry pane is waterlogged");
        helper.succeed();
    }

    /** N65: plain panes are dark by default; a redstone signal lights them (lamp light 15) and without it they go dark again. */
    public static void plainPanesShineAndSwitchWithRedstone(GameTestHelper helper) {
        if (!McVersion.LIGHT_PANES) {
            helper.succeed();
            return;
        }
        ServerPlayer player = player(helper);
        List<String> problems = new ArrayList<>();
        List<Block> plain = new ArrayList<>(ModBlocks.lightPanes());
        plain.remove(ModBlocks.REDSTONE_LIGHT_PANE);
        plain.remove(ModBlocks.COPPER_LIGHT_PANE);
        for (Block pane : plain) {
            clear(helper);
            helper.setBlock(CENTER, Blocks.STONE);
            helper.setBlock(CENTER.east(), Blocks.STONE);
            place(helper, player, pane.asItem(), CENTER, Direction.UP);
            BlockPos at = CENTER.above();
            BlockState dark = helper.getBlockState(at);
            if (!dark.is(pane) || dark.getValue(LightPaneBlock.LIT) || dark.getLightEmission() != 0) {
                problems.add(id(pane) + " is not dark when placed without power: " + dark + " light " + dark.getLightEmission());
            }
            helper.setBlock(CENTER.east().above(), Blocks.REDSTONE_BLOCK);
            BlockState lit = helper.getBlockState(at);
            if (!lit.getValue(LightPaneBlock.LIT) || lit.getLightEmission() != 15) {
                problems.add(id(pane) + " stays dark with a redstone signal: " + lit);
            }
            helper.setBlock(CENTER.east().above(), Blocks.AIR);
            if (helper.getBlockState(at).getValue(LightPaneBlock.LIT)) {
                problems.add(id(pane) + " does not go dark again without the signal");
            }
            // placed next to a powered block it comes up lit
            helper.setBlock(CENTER.east().above(), Blocks.REDSTONE_BLOCK);
            helper.setBlock(at, Blocks.AIR);
            place(helper, player, pane.asItem(), CENTER, Direction.UP);
            if (!helper.getBlockState(at).getValue(LightPaneBlock.LIT)) {
                problems.add(id(pane) + " placed next to power is dark");
            }
        }
        helper.assertTrue(problems.isEmpty(), "light problems: " + String.join("; ", problems));
        helper.succeed();
    }

    /** The light engine really shines while powered (15 in the pane's cell, 14 beside it), then goes dark without the signal. */
    public static void paneLightReachesTheWorldAndGoesOut(GameTestHelper helper) {
        if (!McVersion.LIGHT_PANES) {
            helper.succeed();
            return;
        }
        clear(helper);
        // a closed stone box around the cell so that no sky light mixes in
        for (int x = 0; x <= 4; x++) {
            for (int y = 0; y <= 4; y++) {
                for (int z = 0; z <= 4; z++) {
                    if (x == 0 || x == 4 || y == 0 || y == 4 || z == 0 || z == 4) {
                        helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                    }
                }
            }
        }
        BlockPos at = new BlockPos(2, 1, 2);
        helper.setBlock(at, ModBlocks.TRANSPARENT_LIGHT_PANE.defaultBlockState().setValue(AbstractLightPaneBlock.FACING, Direction.UP));
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.REDSTONE_BLOCK); // N65: shines only while powered
        BlockPos beside = new BlockPos(3, 1, 2);
        int[] phase = {0};
        helper.succeedWhen(() -> {
            ServerLevel level = helper.getLevel();
            int here = level.getBrightness(LightLayer.BLOCK, helper.absolutePos(at));
            int next = level.getBrightness(LightLayer.BLOCK, helper.absolutePos(beside));
            if (phase[0] == 0) {
                helper.assertTrue(here == 15 && next == 14, "powered pane light: cell " + here + ", beside " + next);
                phase[0] = 1;
                helper.setBlock(new BlockPos(2, 2, 2), Blocks.AIR);
            }
            helper.assertTrue(helper.getBlockState(at).getValue(LightPaneBlock.LIT) == false, "the lost signal did not reach the pane");
            helper.assertTrue(here == 0 && next == 0, "light after the signal is gone: cell " + here + ", beside " + next);
        });
    }

    private static void setSignal(GameTestHelper helper, BlockPos comparator, int strength) {
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(comparator);
        helper.setBlock(comparator, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.EAST)
                .setValue(ComparatorBlock.POWERED, strength > 0));
        if (level.getBlockEntity(abs) instanceof ComparatorBlockEntity be) {
            be.setOutputSignal(strength);
        }
        level.updateNeighborsAt(abs, Blocks.COMPARATOR);
    }

    /** Redstone pane: dark without a signal; strength s (1..15) shows dye color (start + s - 1) mod 16 for every start color. */
    public static void redstonePaneColorFollowsSignalStrength(GameTestHelper helper) {
        if (!McVersion.LIGHT_PANES) {
            helper.succeed();
            return;
        }
        List<String> problems = new ArrayList<>();
        BlockPos support = CENTER;
        BlockPos at = CENTER.above();
        BlockPos comparator = at.east();
        for (int start = 0; start < 16; start++) {
            clear(helper);
            helper.setBlock(support, Blocks.STONE);
            helper.setBlock(comparator.below(), Blocks.STONE);
            helper.setBlock(at, ModBlocks.REDSTONE_LIGHT_PANE.defaultBlockState().setValue(AbstractLightPaneBlock.FACING, Direction.UP)
                    .setValue(RedstoneLightPaneBlock.START, start));
            for (int strength = 0; strength <= 15; strength++) {
                setSignal(helper, comparator, strength);
                BlockState state = helper.getBlockState(at);
                int expected = strength == 0 ? RedstoneLightPaneBlock.OFF : (start + strength - 1) % 16;
                if (state.getValue(RedstoneLightPaneBlock.COLOR) != expected) {
                    problems.add("start " + start + " strength " + strength + ": color " + state.getValue(RedstoneLightPaneBlock.COLOR)
                            + " expected " + expected);
                }
                if (state.getLightEmission() != (strength == 0 ? 0 : 15)) {
                    problems.add("start " + start + " strength " + strength + ": light " + state.getLightEmission());
                }
            }
        }
        helper.assertTrue(problems.isEmpty(), problems.size() + " color problem(s): " + String.join("; ", problems.subList(0, Math.min(6, problems.size()))));
        helper.succeed();
    }

    /** Right-click advances the start color like a note block, wraps after 16, keeps the signal's color in step; sneaking places instead. */
    public static void rightClickCyclesTheStartColor(GameTestHelper helper) {
        if (!McVersion.LIGHT_PANES) {
            helper.succeed();
            return;
        }
        ServerPlayer player = player(helper);
        clear(helper);
        helper.setBlock(CENTER, Blocks.STONE);
        helper.setBlock(CENTER.above().east().below(), Blocks.STONE);
        BlockPos at = CENTER.above();
        BlockPos comparator = at.east();
        helper.setBlock(at, ModBlocks.REDSTONE_LIGHT_PANE.defaultBlockState().setValue(AbstractLightPaneBlock.FACING, Direction.UP));
        BlockPos abs = helper.absolutePos(at);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false);
        List<String> problems = new ArrayList<>();
        // without a signal: the start color moves, the pane stays dark
        for (int i = 1; i <= 17; i++) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE, 4)); // like a note block: works with a block in the hand
            var result = helper.getBlockState(at).useWithoutItem(helper.getLevel(), player, hit);
            BlockState state = helper.getBlockState(at);
            if (!result.consumesAction() || state.getValue(RedstoneLightPaneBlock.START) != i % 16) {
                problems.add("click " + i + ": start " + state.getValue(RedstoneLightPaneBlock.START) + " (" + result + ")");
            }
            if (state.getValue(RedstoneLightPaneBlock.COLOR) != RedstoneLightPaneBlock.OFF) {
                problems.add("click " + i + ": lit without a signal");
            }
        }
        // with signal 1 the pane shows the start color; one more click moves it to the next dye color
        setSignal(helper, comparator, 1);
        int before = helper.getBlockState(at).getValue(RedstoneLightPaneBlock.COLOR);
        helper.assertValueEqual(before, helper.getBlockState(at).getValue(RedstoneLightPaneBlock.START), "color of strength 1 is the start color");
        helper.getBlockState(at).useWithoutItem(helper.getLevel(), player, hit);
        helper.assertValueEqual(helper.getBlockState(at).getValue(RedstoneLightPaneBlock.COLOR), (before + 1) % 16, "color of strength 1 after a click");
        setSignal(helper, comparator, 5);
        helper.assertValueEqual(helper.getBlockState(at).getValue(RedstoneLightPaneBlock.COLOR),
                (helper.getBlockState(at).getValue(RedstoneLightPaneBlock.START) + 4) % 16, "strength 5 follows in sequence");
        helper.assertTrue(problems.isEmpty(), "cycle problems: " + String.join("; ", problems));
        helper.succeed();
    }

    /** N65: the Copper Light Pane flips on every rising redstone edge like the Copper Bulb; falling edges and a steady signal change nothing. */
    public static void copperPaneFlipsOnEveryRisingEdge(GameTestHelper helper) {
        if (!McVersion.LIGHT_PANES) {
            helper.succeed();
            return;
        }
        ServerPlayer player = player(helper);
        clear(helper);
        helper.setBlock(CENTER, Blocks.STONE);
        helper.setBlock(CENTER.east(), Blocks.STONE);
        BlockPos at = CENTER.above();
        BlockPos power = CENTER.east().above();
        place(helper, player, ModBlocks.COPPER_LIGHT_PANE.asItem(), CENTER, Direction.UP);
        helper.assertTrue(helper.getBlockState(at).is(ModBlocks.COPPER_LIGHT_PANE), "copper pane not placed: " + helper.getBlockState(at));
        List<String> problems = new ArrayList<>();
        // {powered signal?, expected lit}: off, on (lit), off (stays lit), on (dark), off (stays dark), on (lit)
        boolean[][] steps = {{false, false}, {true, true}, {false, true}, {true, false}, {false, false}, {true, true}};
        for (int i = 0; i < steps.length; i++) {
            helper.setBlock(power, steps[i][0] ? Blocks.REDSTONE_BLOCK : Blocks.AIR);
            BlockState state = helper.getBlockState(at);
            if (state.getValue(CopperLightPaneBlock.LIT) != steps[i][1] || state.getValue(CopperLightPaneBlock.POWERED) != steps[i][0]
                    || state.getLightEmission() != (steps[i][1] ? 15 : 0)) {
                problems.add("step " + i + " (signal " + steps[i][0] + "): " + state + " light " + state.getLightEmission());
            }
            int analog = state.getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(at), Direction.UP);
            if (analog != (steps[i][1] ? 15 : 0)) {
                problems.add("step " + i + ": comparator output " + analog);
            }
        }
        // a steady signal (a second source next to the powered pane) does not flip it again
        BlockState before = helper.getBlockState(at);
        helper.setBlock(at.north(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(at.north(), Blocks.AIR);
        if (helper.getBlockState(at) != before) {
            problems.add("a steady signal changed the pane: " + before + " -> " + helper.getBlockState(at));
        }
        // placed next to a powered block it comes up lit (like the bulb); a placement without power stays dark
        helper.setBlock(at, Blocks.AIR);
        place(helper, player, ModBlocks.COPPER_LIGHT_PANE.asItem(), CENTER, Direction.UP);
        if (!helper.getBlockState(at).getValue(CopperLightPaneBlock.LIT)) {
            problems.add("placed next to power it is dark");
        }
        helper.setBlock(power, Blocks.AIR);
        helper.setBlock(at, Blocks.AIR);
        place(helper, player, ModBlocks.COPPER_LIGHT_PANE.asItem(), CENTER, Direction.UP);
        if (helper.getBlockState(at).getValue(CopperLightPaneBlock.LIT)) {
            problems.add("placed without power it is lit");
        }
        helper.assertTrue(problems.isEmpty(), "copper pane problems: " + String.join("; ", problems));
        helper.succeed();
    }

    /** All 20 panes are craftable: glass pane + glowstone, clear pane, 16 dyes (also recoloring), redstone, copper (copper bulb shape). */
    public static void panesHaveTheirRecipes(GameTestHelper helper) {
        if (!McVersion.LIGHT_PANES) {
            helper.succeed();
            return;
        }
        List<String> missing = new ArrayList<>();
        ItemStack pane = new ItemStack(Items.GLASS_PANE);
        ItemStack dust = new ItemStack(Items.GLOWSTONE_DUST);
        craft(helper, List.of(pane, dust, dust), "2 light_pane", missing);
        craft(helper, List.of(pane, pane, dust), "2 transparent_light_pane", missing);
        ItemStack lamp = new ItemStack(ModItems.LIGHT_PANES.get(0));
        for (DyeColor dye : DyeColor.values()) {
            craft(helper, List.of(lamp, new ItemStack(Items.DYE.pick(dye))), "1 " + dye.getSerializedName() + "_light_pane", missing);
            // recoloring a colored pane
            craft(helper, List.of(new ItemStack(ModItems.LIGHT_PANES.get(2 + DyeColor.RED.getId())), new ItemStack(Items.DYE.pick(dye))),
                    "1 " + dye.getSerializedName() + "_light_pane", missing);
        }
        craft(helper, List.of(lamp, new ItemStack(Items.REDSTONE)), "1 redstone_light_pane", missing);
        ItemStack cu = new ItemStack(Items.COPPER_BLOCK.weathering().unaffected());
        ItemStack powder = new ItemStack(Items.BLAZE_POWDER);
        ItemStack rod = new ItemStack(Items.BLAZE_ROD);
        ItemStack redstone = new ItemStack(Items.REDSTONE);
        ItemStack none = ItemStack.EMPTY;
        craft(helper, 3, List.of(none, cu, none, cu, powder, cu, none, redstone, none), "4 copper_light_pane", missing);
        craft(helper, 3, List.of(none, cu, none, cu, rod, cu, none, redstone, none), "4 copper_bulb", missing); // Vanilla stays Vanilla
        helper.assertTrue(missing.isEmpty(), "light pane recipes wrong: " + String.join("; ", missing));
        helper.succeed();
    }

    private static void craft(GameTestHelper helper, List<ItemStack> items, String expected, List<String> missing) {
        craft(helper, items.size(), items, expected, missing);
    }

    private static void craft(GameTestHelper helper, int width, List<ItemStack> items, String expected, List<String> missing) {
        CraftingInput grid = CraftingInput.of(width, items.size() / width, items);
        Optional<RecipeHolder<CraftingRecipe>> match = helper.getLevel().getServer().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel());
        String got = match.map(m -> {
            ItemStack result = m.value().assemble(grid);
            return result.getCount() + " " + BuiltInRegistries.ITEM.getKey(result.getItem()).getPath();
        }).orElse("nothing");
        if (!got.equals(expected)) {
            missing.add(expected + " (got " + got + ")");
        }
    }
}
