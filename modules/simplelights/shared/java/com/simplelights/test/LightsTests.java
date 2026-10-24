package com.simplelights.test;

import com.simplelib.api.IdMoves;
import com.simplelights.LightsRegistry;
import com.simplelights.SimpleLights;
import com.simplelights.block.AbstractLightPaneBlock;
import com.simplelights.block.CopperLightPaneBlock;
import com.simplelights.block.CopperRedstoneLightPaneBlock;
import com.simplelights.block.LightPaneBlock;
import com.simplelights.block.RedstoneLightPaneBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
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
 * Loader-neutral; moved from SimpleBuilding's LightPaneTests with the panes (WB24-6). The Fabric adapter registers
 * {@link #ALL} as module_game_test_<name> on {@code simplelights:empty}; every test runs standalone and with SimpleBuilding.
 */
public final class LightsTests {
    private static final BlockPos CENTER = new BlockPos(2, 2, 2);
    /** Test name -> body, in catalogue order (the Fabric adapter names them module_game_test_<name>). */
    public static final java.util.Map<String, java.util.function.Consumer<GameTestHelper>> ALL = new java.util.LinkedHashMap<>();

    static {
        ALL.put("registered", LightsTests::registered);
        ALL.put("sub_mod_switch", LightsTests::subModSwitch);
        ALL.put("migration_from_simplebuilding", LightsTests::migrationFromSimplebuilding);
        ALL.put("panes_place_on_every_side", LightsTests::panesPlaceOnEverySide);
        ALL.put("panes_need_full_support_face", LightsTests::panesNeedFullSupportFace);
        ALL.put("panes_fall_off_without_support", LightsTests::panesFallOffWithoutSupport);
        ALL.put("panes_waterlog", LightsTests::panesWaterlog);
        ALL.put("plain_panes_shine_and_switch_with_redstone", LightsTests::plainPanesShineAndSwitchWithRedstone);
        ALL.put("pane_light_reaches_the_world_and_goes_out", LightsTests::paneLightReachesTheWorldAndGoesOut);
        ALL.put("redstone_pane_color_follows_signal_strength", LightsTests::redstonePaneColorFollowsSignalStrength);
        ALL.put("right_click_cycles_the_start_color", LightsTests::rightClickCyclesTheStartColor);
        ALL.put("panes_have_their_recipes", LightsTests::panesHaveTheirRecipes);
        ALL.put("copper_pane_flips_on_every_rising_edge", LightsTests::copperPaneFlipsOnEveryRisingEdge);
        ALL.put("copper_redstone_pane_colors_and_flips", LightsTests::copperRedstonePaneColorsAndFlips);
        ALL.put("discover_recipe_unlocks", com.simplelights.discover.LightsDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simplelights.discover.LightsDiscover::behaviourHints);
        ALL.put("guide_book", com.simplelights.guide.LightsGuide::gameTest);
    }

    private LightsTests() {
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
        helper.assertValueEqual(LightsRegistry.lightPanes().size(), 37, "number of light panes");
        ServerPlayer player = player(helper);
        List<String> problems = new ArrayList<>();
        for (Block pane : LightsRegistry.lightPanes()) {
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
        ServerPlayer player = player(helper);
        Item pane = LightsRegistry.LIGHT_PANE.asItem();
        clear(helper);
        helper.setBlock(CENTER, Blocks.OAK_FENCE);
        place(helper, player, pane, CENTER, Direction.UP);
        helper.assertFalse(helper.getBlockState(CENTER.above()).is(LightsRegistry.LIGHT_PANE), "a pane on a fence post");
        helper.setBlock(CENTER, Blocks.GLASS_PANE);
        place(helper, player, pane, CENTER, Direction.EAST);
        helper.assertFalse(helper.getBlockState(CENTER.east()).is(LightsRegistry.LIGHT_PANE), "a pane on a glass pane");
        helper.setBlock(CENTER, Blocks.STONE_SLAB);
        place(helper, player, pane, CENTER, Direction.NORTH);
        helper.assertFalse(helper.getBlockState(CENTER.north()).is(LightsRegistry.LIGHT_PANE), "a pane on the side of a slab");
        place(helper, player, pane, CENTER, Direction.UP);
        helper.assertFalse(helper.getBlockState(CENTER.above()).is(LightsRegistry.LIGHT_PANE), "a pane on the half-high top of a bottom slab");
        helper.setBlock(CENTER, Blocks.STONE_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE,
                net.minecraft.world.level.block.state.properties.SlabType.TOP));
        place(helper, player, pane, CENTER, Direction.UP);
        helper.assertTrue(helper.getBlockState(CENTER.above()).is(LightsRegistry.LIGHT_PANE), "no pane on the full top of a top slab");
        helper.succeed();
    }

    /** Without its support block a pane drops as an item (all six sides); another full block behind keeps it. */
    public static void panesFallOffWithoutSupport(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> problems = new ArrayList<>();
        for (Direction face : Direction.values()) {
            clear(helper);
            for (ItemEntity old : level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(CENTER)).inflate(6.0))) {
                old.discard();
            }
            BlockPos at = CENTER.relative(face);
            helper.setBlock(CENTER, Blocks.STONE);
            helper.setBlock(at, LightsRegistry.LIGHT_PANE.defaultBlockState().setValue(AbstractLightPaneBlock.FACING, face));
            helper.setBlock(CENTER, Blocks.DIRT);
            if (!helper.getBlockState(at).is(LightsRegistry.LIGHT_PANE)) {
                problems.add(face + ": gone when the support was replaced by another full block");
            }
            helper.setBlock(CENTER, Blocks.AIR);
            if (!helper.getBlockState(at).isAir()) {
                problems.add(face + ": still there without a support: " + helper.getBlockState(at));
            }
            boolean dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(CENTER)).inflate(3.0))
                    .stream().anyMatch(e -> e.getItem().is(LightsRegistry.LIGHT_PANE.asItem()));
            if (!dropped) {
                problems.add(face + ": did not drop as an item");
            }
        }
        helper.assertTrue(problems.isEmpty(), "fall-off problems: " + String.join("; ", problems));
        helper.succeed();
    }

    /** Placed into water a pane is waterlogged; the water stays a source; breaking drops the pane. */
    public static void panesWaterlog(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        clear(helper);
        helper.setBlock(CENTER, Blocks.STONE);
        helper.setBlock(CENTER.above(), Blocks.WATER);
        place(helper, player, LightsRegistry.TRANSPARENT_LIGHT_PANE.asItem(), CENTER, Direction.UP);
        BlockState state = helper.getBlockState(CENTER.above());
        helper.assertTrue(state.is(LightsRegistry.TRANSPARENT_LIGHT_PANE) && state.getValue(AbstractLightPaneBlock.WATERLOGGED), "pane in water is not waterlogged: " + state);
        helper.assertTrue(state.getFluidState().isSource(), "waterlogged pane has no water source");
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(CENTER.above())).getFluidState().isSource(), "level state not waterlogged");
        place(helper, player, LightsRegistry.LIGHT_PANE.asItem(), CENTER, Direction.EAST);
        helper.assertFalse(helper.getBlockState(CENTER.east()).getValue(AbstractLightPaneBlock.WATERLOGGED), "dry pane is waterlogged");
        helper.succeed();
    }

    /** N65: plain panes are dark by default; a redstone signal lights them (lamp light 15) and without it they go dark again. */
    public static void plainPanesShineAndSwitchWithRedstone(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        List<String> problems = new ArrayList<>();
        List<Block> plain = new ArrayList<>(LightsRegistry.lightPanes());
        plain.remove(LightsRegistry.REDSTONE_LIGHT_PANE);
        plain.remove(LightsRegistry.COPPER_LIGHT_PANE);
        plain.removeAll(LightsRegistry.COPPER_COLORED_LIGHT_PANES);
        plain.remove(LightsRegistry.COPPER_REDSTONE_LIGHT_PANE);
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
        helper.setBlock(at, LightsRegistry.TRANSPARENT_LIGHT_PANE.defaultBlockState().setValue(AbstractLightPaneBlock.FACING, Direction.UP));
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
        List<String> problems = new ArrayList<>();
        BlockPos support = CENTER;
        BlockPos at = CENTER.above();
        BlockPos comparator = at.east();
        for (int start = 0; start < 16; start++) {
            clear(helper);
            helper.setBlock(support, Blocks.STONE);
            helper.setBlock(comparator.below(), Blocks.STONE);
            helper.setBlock(at, LightsRegistry.REDSTONE_LIGHT_PANE.defaultBlockState().setValue(AbstractLightPaneBlock.FACING, Direction.UP)
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
        ServerPlayer player = player(helper);
        clear(helper);
        helper.setBlock(CENTER, Blocks.STONE);
        helper.setBlock(CENTER.above().east().below(), Blocks.STONE);
        BlockPos at = CENTER.above();
        BlockPos comparator = at.east();
        helper.setBlock(at, LightsRegistry.REDSTONE_LIGHT_PANE.defaultBlockState().setValue(AbstractLightPaneBlock.FACING, Direction.UP));
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
        ServerPlayer player = player(helper);
        List<String> problems = new ArrayList<>();
        List<Block> copper = new ArrayList<>(LightsRegistry.COPPER_COLORED_LIGHT_PANES);
        copper.add(LightsRegistry.COPPER_LIGHT_PANE);
        for (Block pane : copper) { // N68: the sixteen colors flip like the plain copper pane
            copperFlip(helper, player, pane, problems);
        }
        helper.assertTrue(problems.isEmpty(), "copper pane problems: " + String.join("; ", problems.subList(0, Math.min(6, problems.size()))));
        helper.succeed();
    }

    private static void copperFlip(GameTestHelper helper, ServerPlayer player, Block pane, List<String> problems) {
        clear(helper);
        helper.setBlock(CENTER, Blocks.STONE);
        helper.setBlock(CENTER.east(), Blocks.STONE);
        BlockPos at = CENTER.above();
        BlockPos power = CENTER.east().above();
        place(helper, player, pane.asItem(), CENTER, Direction.UP);
        helper.assertTrue(helper.getBlockState(at).is(pane), id(pane) + " not placed: " + helper.getBlockState(at));
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
        place(helper, player, pane.asItem(), CENTER, Direction.UP);
        if (!helper.getBlockState(at).getValue(CopperLightPaneBlock.LIT)) {
            problems.add("placed next to power it is dark");
        }
        helper.setBlock(power, Blocks.AIR);
        helper.setBlock(at, Blocks.AIR);
        place(helper, player, pane.asItem(), CENTER, Direction.UP);
        if (helper.getBlockState(at).getValue(CopperLightPaneBlock.LIT)) {
            problems.add("placed without power it is lit");
        }
    }

    /** N68: the copper multicolor pane: color per signal strength like the plain one, on/off per rising edge, right-click moves the start color. */
    public static void copperRedstonePaneColorsAndFlips(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        clear(helper);
        helper.setBlock(CENTER, Blocks.STONE);
        helper.setBlock(CENTER.above().east().below(), Blocks.STONE);
        BlockPos at = CENTER.above();
        BlockPos comparator = at.east();
        helper.setBlock(at, LightsRegistry.COPPER_REDSTONE_LIGHT_PANE.defaultBlockState().setValue(AbstractLightPaneBlock.FACING, Direction.UP));
        List<String> problems = new ArrayList<>();
        // {strength, lit, color}: strength 7 (start 0) lights it with color 6; no signal keeps lit and color; strength 3 is the second edge (off)
        int[][] steps = {{7, 1, 6}, {0, 1, 6}, {3, 0, 2}, {0, 0, 2}, {12, 1, 11}};
        for (int[] step : steps) {
            setSignal(helper, comparator, step[0]);
            BlockState state = helper.getBlockState(at);
            if ((state.getValue(CopperRedstoneLightPaneBlock.LIT) ? 1 : 0) != step[1] || state.getValue(CopperRedstoneLightPaneBlock.COLOR) != step[2]
                    || state.getLightEmission() != (step[1] == 1 ? 15 : 0)) {
                problems.add("strength " + step[0] + ": " + state + " light " + state.getLightEmission());
            }
        }
        BlockPos abs = helper.absolutePos(at);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false);
        helper.getBlockState(at).useWithoutItem(helper.getLevel(), player, hit);
        BlockState clicked = helper.getBlockState(at);
        if (clicked.getValue(CopperRedstoneLightPaneBlock.START) != 1 || clicked.getValue(CopperRedstoneLightPaneBlock.COLOR) != 12) {
            problems.add("click at strength 12: " + clicked);
        }
        helper.assertTrue(problems.isEmpty(), "copper redstone pane problems: " + String.join("; ", problems));
        helper.succeed();
    }

    /** All 37 panes are craftable: glass pane + glowstone, clear pane, 16 dyes (also recoloring), redstone, copper (copper bulb shape). */
    public static void panesHaveTheirRecipes(GameTestHelper helper) {
        List<String> missing = new ArrayList<>();
        ItemStack pane = new ItemStack(Items.GLASS_PANE);
        ItemStack dust = new ItemStack(Items.GLOWSTONE_DUST);
        craft(helper, List.of(pane, dust, dust), "2 light_pane", missing);
        craft(helper, List.of(pane, pane, dust), "2 transparent_light_pane", missing);
        ItemStack lamp = new ItemStack(LightsRegistry.ITEMS.get(0));
        for (DyeColor dye : DyeColor.values()) {
            craft(helper, List.of(lamp, new ItemStack(Items.DYE.pick(dye))), "1 " + dye.getSerializedName() + "_light_pane", missing);
            // recoloring a colored pane
            craft(helper, List.of(new ItemStack(LightsRegistry.ITEMS.get(2 + DyeColor.RED.getId())), new ItemStack(Items.DYE.pick(dye))),
                    "1 " + dye.getSerializedName() + "_light_pane", missing);
        }
        craft(helper, List.of(lamp, new ItemStack(Items.REDSTONE)), "1 redstone_light_pane", missing);
        ItemStack cu = new ItemStack(Items.COPPER_BLOCK.weathering().unaffected());
        ItemStack powder = new ItemStack(Items.BLAZE_POWDER);
        ItemStack rod = new ItemStack(Items.BLAZE_ROD);
        ItemStack redstone = new ItemStack(Items.REDSTONE);
        ItemStack none = ItemStack.EMPTY;
        craft(helper, 3, List.of(none, cu, none, cu, powder, cu, none, redstone, none), "4 copper_light_pane", missing);
        ItemStack copperPane = new ItemStack(LightsRegistry.ITEMS.get(19));
        for (DyeColor dye : DyeColor.values()) { // N68: the copper pane takes a dye, also recoloring
            craft(helper, List.of(copperPane, new ItemStack(Items.DYE.pick(dye))), "1 " + dye.getSerializedName() + "_copper_light_pane", missing);
            craft(helper, List.of(new ItemStack(LightsRegistry.ITEMS.get(20 + DyeColor.RED.getId())), new ItemStack(Items.DYE.pick(dye))),
                    "1 " + dye.getSerializedName() + "_copper_light_pane", missing);
        }
        craft(helper, List.of(copperPane, redstone), "1 copper_redstone_light_pane", missing);
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
    /** All 37 panes, their items (two with the multicolor tooltip), the guide, tags and the tab are registered under {@code simplelights:}. */
    public static void registered(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        List<String> paths = SimpleLights.panePaths();
        if (paths.size() != 37 || LightsRegistry.lightPanes().size() != 37 || LightsRegistry.ITEMS.size() != 37) {
            problems.add("counts " + paths.size() + "/" + LightsRegistry.lightPanes().size() + "/" + LightsRegistry.ITEMS.size());
        }
        for (int i = 0; i < paths.size(); i++) {
            String path = paths.get(i);
            if (!BuiltInRegistries.BLOCK.containsKey(SimpleLights.id(path))) problems.add("block " + path);
            if (!BuiltInRegistries.ITEM.containsKey(SimpleLights.id(path))) problems.add("item " + path);
            if (i < LightsRegistry.ITEMS.size() && !BuiltInRegistries.ITEM.getKey(LightsRegistry.ITEMS.get(i)).equals(SimpleLights.id(path))) problems.add("order at " + i);
        }
        if (!(LightsRegistry.REDSTONE_LIGHT_PANE.asItem() instanceof com.simplelights.block.LightPaneItem)
                || !(LightsRegistry.COPPER_REDSTONE_LIGHT_PANE.asItem() instanceof com.simplelights.block.LightPaneItem)
                || LightsRegistry.LIGHT_PANE.asItem() instanceof com.simplelights.block.LightPaneItem) {
            problems.add("multicolor tooltip items");
        }
        if (!BuiltInRegistries.ITEM.containsKey(SimpleLights.id("guide_book"))) problems.add("guide book");
        var panes = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, SimpleLights.id("light_panes"));
        var copper = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, SimpleLights.id("copper_light_panes"));
        if (!new ItemStack(LightsRegistry.LIGHT_PANE).is(panes) || !new ItemStack(LightsRegistry.COLORED_LIGHT_PANES.get(5)).is(panes)
                || new ItemStack(LightsRegistry.REDSTONE_LIGHT_PANE).is(panes)) problems.add("tag light_panes");
        if (!new ItemStack(LightsRegistry.COPPER_LIGHT_PANE).is(copper) || !new ItemStack(LightsRegistry.COPPER_COLORED_LIGHT_PANES.get(5)).is(copper)
                || new ItemStack(LightsRegistry.COPPER_REDSTONE_LIGHT_PANE).is(copper)) problems.add("tag copper_light_panes");
        if (!BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(SimpleLights.id("lights"))) problems.add("tab");
        if (LightsRegistry.tabStacks().size() != 38) problems.add("tab stacks " + LightsRegistry.tabStacks().size());
        helper.assertTrue(problems.isEmpty(), "registration: " + problems);
        helper.succeed();
    }

    /** "Enable Simple Lights" off: no tab or Vanilla tab stacks, the panes stay registered and keep working. */
    public static void subModSwitch(GameTestHelper helper) {
        com.simplebuilding.framework.api.SubMods.set(SimpleLights.MOD_ID, false);
        try {
            helper.assertFalse(SimpleLights.on(), "switched off");
            helper.assertTrue(LightsRegistry.tabStacks().isEmpty() && LightsRegistry.paneStacks().isEmpty(), "no tab stacks while off");
            helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(SimpleLights.id("copper_redstone_light_pane")), "registrations stay");
        } finally {
            com.simplebuilding.framework.api.SubMods.set(SimpleLights.MOD_ID, true);
        }
        helper.assertTrue(SimpleLights.on() && !LightsRegistry.tabStacks().isEmpty(), "back on");
        helper.succeed();
    }

    /**
     * Old worlds: every {@code simplebuilding:<pane>} block, item, recipe and recipe unlock resolves to {@code simplelights:},
     * a saved block state (chunk palette form) and a saved item stack with the old id load as the new pane and save the new id.
     */
    public static void migrationFromSimplebuilding(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        for (String path : SimpleLights.panePaths()) {
            if (BuiltInRegistries.BLOCK.getValue(SimpleLights.old(path)) != BuiltInRegistries.BLOCK.getValue(SimpleLights.id(path))) problems.add("block " + path);
            if (BuiltInRegistries.ITEM.getValue(SimpleLights.old(path)) != BuiltInRegistries.ITEM.getValue(SimpleLights.id(path))) problems.add("item " + path);
            if (!SimpleLights.id(path).equals(IdMoves.target(net.minecraft.core.registries.Registries.RECIPE, SimpleLights.old(path)))) problems.add("recipe " + path);
            String advancement = "recipes/" + SimpleLights.recipeFolder(path) + "/" + path;
            if (!SimpleLights.id(advancement).equals(IdMoves.advancement(SimpleLights.old(advancement)))) problems.add("advancement " + path);
            if (helper.getLevel().getServer().getAdvancements().get(SimpleLights.id(advancement)) == null) problems.add("unlock missing " + advancement);
        }
        // A pane of an old world, as the chunk palette stores it.
        net.minecraft.nbt.CompoundTag stateTag = new net.minecraft.nbt.CompoundTag();
        stateTag.putString("Name", "simplebuilding:redstone_light_pane");
        net.minecraft.nbt.CompoundTag props = new net.minecraft.nbt.CompoundTag();
        props.putString("facing", "north");
        props.putString("start", "3");
        stateTag.put("Properties", props);
        // Diagnose the lookup paths of an old block id (chunk palette, NBT helpers, registry key).
        Identifier oldId = SimpleLights.old("redstone_light_pane");
        net.minecraft.resources.ResourceKey<Block> oldKey = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, oldId);
        String paths = "getValue=" + BuiltInRegistries.BLOCK.getValue(oldId)
                + " getKey=" + BuiltInRegistries.BLOCK.get(oldKey).map(Object::toString).orElse("-")
                + " getId=" + BuiltInRegistries.BLOCK.get(oldId).map(Object::toString).orElse("-")
                + " getOptional=" + BuiltInRegistries.BLOCK.getOptional(oldId).map(Object::toString).orElse("-")
                + " level=" + helper.getLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK).get(oldKey).map(Object::toString).orElse("-")
                + " nbt=" + net.minecraft.nbt.NbtUtils.readBlockState(helper.getLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK), stateTag)
                + " codec=" + BlockState.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, stateTag);
        SimpleLights.LOG.info("[migration paths] {}", paths);
        // The chunk palette decodes through BlockState.CODEC (the block registry's name codec).
        BlockState old = BlockState.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, stateTag).result().orElse(Blocks.AIR.defaultBlockState());
        if (!old.is(LightsRegistry.REDSTONE_LIGHT_PANE) || old.getValue(AbstractLightPaneBlock.FACING) != Direction.NORTH
                || old.getValue(RedstoneLightPaneBlock.START) != 3) {
            problems.add("old block state loads as " + old + " | " + paths);
        } else if (!BlockState.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, old).result()
                .map(t -> ((net.minecraft.nbt.CompoundTag) t).getStringOr("Name", "")).orElse("").equals("simplelights:redstone_light_pane")) {
            problems.add("old block state saves under another id");
        }
        // A stack of an old inventory.
        net.minecraft.nbt.CompoundTag stackTag = new net.minecraft.nbt.CompoundTag();
        stackTag.putString("id", "simplebuilding:cyan_copper_light_pane");
        stackTag.putInt("count", 7);
        var ops = helper.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        ItemStack stack = ItemStack.CODEC.parse(ops, stackTag).result().orElse(ItemStack.EMPTY);
        Item cyan = LightsRegistry.COPPER_COLORED_LIGHT_PANES.get(DyeColor.CYAN.getId()).asItem();
        if (!stack.is(cyan) || stack.getCount() != 7) {
            problems.add("old stack loads as " + stack);
        } else {
            String saved = ItemStack.CODEC.encodeStart(ops, stack).result().map(t -> ((net.minecraft.nbt.CompoundTag) t).getStringOr("id", "")).orElse("");
            if (!saved.equals("simplelights:cyan_copper_light_pane")) problems.add("old stack saves as " + saved);
        }
        helper.assertTrue(problems.isEmpty(), "migration: " + problems);
        helper.succeed();
    }
}
