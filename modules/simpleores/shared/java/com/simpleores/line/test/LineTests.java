package com.simpleores.line.test;

import com.simpleores.OresRegistry;
import com.simpleores.SimpleOres;
import com.simpleores.config.OresConfig;
import com.simpleores.line.EndPistonBlock;
import com.simpleores.line.EndPistonMoves;
import com.simpleores.line.EndRailBlock;
import com.simpleores.line.EndRailPhysics;
import com.simpleores.line.EndSignalBlock;
import com.simpleores.line.EndSignalLampBlock;
import com.simpleores.line.LineBlocks;
import com.simpleores.line.LineRedstoneBlock;
import com.simpleores.line.LineSounds;
import com.simpleores.line.LineSource;
import com.simpleores.line.SignalDustBlock;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.phys.Vec3;

/**
 * Gametests of the Astral/Nihil redstone line, moved from the core into Simple Ores with the line itself
 * (PLAN-REDSTONE-LINIEN-2026-10-11, PLAN-ASTRAL-KOLBEN-2026-10-02, PLAN-ASTRAL-NIHIL-SCHIENEN-2026-10-02). Bodies are
 * registered in {@link com.simpleores.test.OresTests}. Geometry: lanes run east from x=0, layers at y=1 and y=4; every test
 * builds its own floor and counts only blocks at y >= 1, so it runs on the empty structure and on the stone arena alike.
 */
public final class LineTests {
    private LineTests() {
    }

    /** Registers the line tests ("line_" prefix; the Fabric adapter methods are the camel case of the names). */
    public static void register(Map<String, Consumer<GameTestHelper>> all, Map<String, Integer> maxTicks) {
        all.put("line_lines_stay_separate_side_by_side", LineTests::linesStaySeparateSideBySide);
        all.put("line_lines_stay_separate_through_the_block", LineTests::linesStaySeparateThroughTheBlock);
        all.put("line_vanilla_sources_feed_astral_and_nihil", LineTests::vanillaSourcesFeedAstralAndNihil);
        all.put("line_astral_powers_lamp_and_piston", LineTests::astralPowersLampAndPiston);
        maxTicks.put("line_astral_powers_lamp_and_piston", 60);
        all.put("line_bridge_links_all_lines", LineTests::bridgeLinksAllLines);
        maxTicks.put("line_bridge_links_all_lines", 60);
        all.put("line_lamps_toggle_on_every_line", LineTests::lampsToggleOnEveryLine);
        maxTicks.put("line_lamps_toggle_on_every_line", 60);
        all.put("line_every_device_takes_redstone", LineTests::everyDeviceTakesRedstone);
        maxTicks.put("line_every_device_takes_redstone", 60);
        all.put("line_every_device_takes_astral", LineTests::everyDeviceTakesAstral);
        maxTicks.put("line_every_device_takes_astral", 60);
        all.put("line_every_device_takes_nihil", LineTests::everyDeviceTakesNihil);
        maxTicks.put("line_every_device_takes_nihil", 60);
        all.put("line_every_device_takes_bridge", LineTests::everyDeviceTakesBridge);
        maxTicks.put("line_every_device_takes_bridge", 60);
        all.put("line_line_blocks_power_every_device", LineTests::lineBlocksPowerEveryDevice);
        maxTicks.put("line_line_blocks_power_every_device", 140);
        all.put("line_line_blocks_feed_only_their_line", LineTests::lineBlocksFeedOnlyTheirLine);
        maxTicks.put("line_line_blocks_feed_only_their_line", 60);
        all.put("line_line_torches_feed_only_their_line", LineTests::lineTorchesFeedOnlyTheirLine);
        maxTicks.put("line_line_torches_feed_only_their_line", 60);
        all.put("line_line_torch_powers_the_block_above_and_inverts", LineTests::lineTorchPowersTheBlockAboveAndInverts);
        maxTicks.put("line_line_torch_powers_the_block_above_and_inverts", 60);
        all.put("line_line_torch_recipe_and_drop", LineTests::lineTorchRecipeAndDrop);
        all.put("line_line_block_recipes_round_trip", LineTests::lineBlockRecipesRoundTrip);
        all.put("line_line_blocks_have_own_sounds", LineTests::lineBlocksHaveOwnSounds);
        all.put("line_redstone_recipes_yield_two", LineTests::redstoneRecipesYieldTwo);
        all.put("line_channels_stay_isolated_and_stop_at_fifteen", LineTests::channelsStayIsolatedAndStopAtFifteen);
        maxTicks.put("line_channels_stay_isolated_and_stop_at_fifteen", 220);
        all.put("line_powder_connects_like_redstone_wire", LineTests::powderConnectsLikeRedstoneWire);
        maxTicks.put("line_powder_connects_like_redstone_wire", 220);
        all.put("line_piston_pushes_all_six_at_once", LineTests::pistonPushesAllSixAtOnce);
        maxTicks.put("line_piston_pushes_all_six_at_once", 100);
        all.put("line_piston_never_chains", LineTests::pistonNeverChains);
        maxTicks.put("line_piston_never_chains", 100);
        all.put("line_nihil_pulls_across_the_gap", LineTests::nihilPullsAcrossTheGap);
        maxTicks.put("line_nihil_pulls_across_the_gap", 100);
        all.put("line_piston_leaves_immovables_alone", LineTests::pistonLeavesImmovablesAlone);
        maxTicks.put("line_piston_leaves_immovables_alone", 100);
        all.put("line_two_pistons_move_one_block_once", LineTests::twoPistonsMoveOneBlockOnce);
        maxTicks.put("line_two_pistons_move_one_block_once", 100);
        all.put("line_piston_fires_on_rising_edge_only", LineTests::pistonFiresOnRisingEdgeOnly);
        maxTicks.put("line_piston_fires_on_rising_edge_only", 100);
        all.put("line_piston_takes_every_line", LineTests::pistonTakesEveryLine);
        maxTicks.put("line_piston_takes_every_line", 100);
        all.put("line_astral_boost_approaches_top_speed_without_passing_it", LineTests::astralBoostApproachesTopSpeedWithoutPassingIt);
        all.put("line_astral_boosts_more_than_the_powered_rail", LineTests::astralBoostsMoreThanThePoweredRail);
        all.put("line_nihil_brake_stops_smoothly", LineTests::nihilBrakeStopsSmoothly);
        all.put("line_end_rail_config_is_clamped", LineTests::endRailConfigIsClamped);
        all.put("line_end_rail_is_fed_by_every_line", LineTests::endRailIsFedByEveryLine);
        maxTicks.put("line_end_rail_is_fed_by_every_line", 100);
        all.put("line_astral_rail_launches_the_cart_past_vanilla_speed", LineTests::astralRailLaunchesTheCartPastVanillaSpeed);
        maxTicks.put("line_astral_rail_launches_the_cart_past_vanilla_speed", 140);
        all.put("line_nihil_rail_stops_and_fast_cart_takes_the_curve", LineTests::nihilRailStopsAndFastCartTakesTheCurve);
        maxTicks.put("line_nihil_rail_stops_and_fast_cart_takes_the_curve", 120);
    }

    private static Block[] lines() {
        return new Block[]{Blocks.REDSTONE_WIRE, LineBlocks.ASTRAL_REDSTONE, LineBlocks.NIHIL_REDSTONE};
    }

    /** All six ordered pairs of different lines, as lane positions (y, z). */
    private static List<int[]> lanes() {
        return List.of(new int[]{1, 0}, new int[]{1, 2}, new int[]{1, 4}, new int[]{1, 6}, new int[]{4, 0}, new int[]{4, 3});
    }

    private static List<Block[]> pairs() {
        List<Block[]> pairs = new ArrayList<>();
        for (Block a : lines()) for (Block b : lines()) if (a != b) pairs.add(new Block[]{a, b});
        return pairs;
    }

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x <= 7; x++) for (int z = 0; z <= 7; z++) {
            helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            helper.setBlock(new BlockPos(x, 3, z), Blocks.STONE);
        }
    }

    /**
     * Gives every piece of dust in the room the connections a player's placement would give it: setBlock places the
     * default state (a dot) and only direct neighbours update it, like vanilla dust set by a command.
     */
    public static void reshape(GameTestHelper helper) {
        for (BlockPos rel : BlockPos.betweenClosed(0, 0, 0, 7, 7, 7)) {
            BlockState state = helper.getBlockState(rel);
            if (SignalDustBlock.lineOf(state) == null) continue;
            BlockPos pos = helper.absolutePos(rel);
            helper.getLevel().setBlock(pos, Block.updateFromNeighbourShapes(state, helper.getLevel(), pos), Block.UPDATE_ALL);
        }
    }

    private static int power(GameTestHelper helper, BlockPos rel) {
        return helper.getBlockState(rel).getValue(SignalDustBlock.POWER);
    }

    private static String name(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getPath();
    }

    /** Dust of one line never feeds dust of another line it touches, for all six ordered pairs. */
    public static void linesStaySeparateSideBySide(GameTestHelper helper) {
        floor(helper);
        List<Block[]> pairs = pairs();
        for (int i = 0; i < pairs.size(); i++) {
            int y = lanes().get(i)[0], z = lanes().get(i)[1];
            helper.setBlock(new BlockPos(0, y, z), Blocks.REDSTONE_BLOCK);
            for (int x = 1; x <= 2; x++) helper.setBlock(new BlockPos(x, y, z), pairs.get(i)[0]);
            for (int x = 3; x <= 4; x++) helper.setBlock(new BlockPos(x, y, z), pairs.get(i)[1]);
        }
        reshape(helper);
        helper.runAfterDelay(4, () -> {
            for (int i = 0; i < pairs.size(); i++) {
                int y = lanes().get(i)[0], z = lanes().get(i)[1];
                String lane = name(pairs.get(i)[0]) + " -> " + name(pairs.get(i)[1]);
                helper.assertTrue(power(helper, new BlockPos(1, y, z)) == 15 && power(helper, new BlockPos(2, y, z)) == 14,
                        lane + ": source line not powered 15/14 but " + power(helper, new BlockPos(1, y, z)) + "/" + power(helper, new BlockPos(2, y, z)));
                helper.assertTrue(power(helper, new BlockPos(3, y, z)) == 0 && power(helper, new BlockPos(4, y, z)) == 0,
                        lane + ": other line took power " + power(helper, new BlockPos(3, y, z)));
            }
            helper.succeed();
        });
    }

    /**
     * Dust pointing into a stone powers it like vanilla dust - a redstone lamp on top lights - but dust of another line
     * on the far side of that stone stays dark (dust never reads dust through a block, across all lines).
     */
    public static void linesStaySeparateThroughTheBlock(GameTestHelper helper) {
        floor(helper);
        List<Block[]> pairs = pairs();
        for (int i = 0; i < pairs.size(); i++) {
            int y = lanes().get(i)[0], z = lanes().get(i)[1];
            helper.setBlock(new BlockPos(0, y, z), Blocks.REDSTONE_BLOCK);
            for (int x = 1; x <= 2; x++) helper.setBlock(new BlockPos(x, y, z), pairs.get(i)[0]);
            helper.setBlock(new BlockPos(3, y, z), Blocks.STONE);
            helper.setBlock(new BlockPos(3, y + 1, z), Blocks.REDSTONE_LAMP);
            for (int x = 4; x <= 5; x++) helper.setBlock(new BlockPos(x, y, z), pairs.get(i)[1]);
        }
        reshape(helper);
        helper.runAfterDelay(6, () -> {
            for (int i = 0; i < pairs.size(); i++) {
                int y = lanes().get(i)[0], z = lanes().get(i)[1];
                String lane = name(pairs.get(i)[0]) + " | stone | " + name(pairs.get(i)[1]);
                helper.assertTrue(helper.getBlockState(new BlockPos(3, y + 1, z)).getValue(RedstoneLampBlock.LIT),
                        lane + ": lamp on the powered stone is dark");
                helper.assertTrue(power(helper, new BlockPos(4, y, z)) == 0 && power(helper, new BlockPos(5, y, z)) == 0,
                        lane + ": other line read the stone: " + power(helper, new BlockPos(4, y, z)));
            }
            helper.succeed();
        });
    }

    /** Lever, redstone torch, redstone block and a pressed button feed Astral and Nihil Redstone at full strength. */
    public static void vanillaSourcesFeedAstralAndNihil(GameTestHelper helper) {
        floor(helper);
        List<BlockState> sources = List.of(
                Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR).setValue(LeverBlock.POWERED, true),
                Blocks.REDSTONE_TORCH.defaultBlockState(),
                Blocks.REDSTONE_BLOCK.defaultBlockState(),
                Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACE, AttachFace.FLOOR).setValue(ButtonBlock.POWERED, true));
        for (int s = 0; s < sources.size(); s++) {
            for (int layer = 0; layer < 2; layer++) {
                int y = layer == 0 ? 1 : 4, z = s * 2;
                helper.setBlock(new BlockPos(0, y, z), sources.get(s));
                for (int x = 1; x <= 3; x++) helper.setBlock(new BlockPos(x, y, z), layer == 0 ? LineBlocks.ASTRAL_REDSTONE : LineBlocks.NIHIL_REDSTONE);
            }
        }
        reshape(helper);
        helper.runAfterDelay(4, () -> {
            for (int s = 0; s < sources.size(); s++) {
                for (int y : new int[]{1, 4}) {
                    String what = sources.get(s).getBlock() + " -> " + (y == 1 ? "astral" : "nihil");
                    helper.assertTrue(power(helper, new BlockPos(1, y, s * 2)) == 15, what + ": " + power(helper, new BlockPos(1, y, s * 2)));
                    helper.assertTrue(power(helper, new BlockPos(3, y, s * 2)) == 13, what + " did not drop one per step: " + power(helper, new BlockPos(3, y, s * 2)));
                }
            }
            helper.succeed();
        });
    }

    /** Astral and Nihil Redstone power a redstone lamp and push a vanilla piston, and lose it when the source goes. */
    public static void astralPowersLampAndPiston(GameTestHelper helper) {
        floor(helper);
        for (int lane = 0; lane < 2; lane++) {
            Block dust = lane == 0 ? LineBlocks.ASTRAL_REDSTONE : LineBlocks.NIHIL_REDSTONE;
            int z = lane * 4;
            helper.setBlock(new BlockPos(0, 1, z), Blocks.REDSTONE_BLOCK);
            for (int x = 1; x <= 3; x++) helper.setBlock(new BlockPos(x, 1, z), dust);
            helper.setBlock(new BlockPos(4, 1, z), Blocks.REDSTONE_LAMP);
            helper.setBlock(new BlockPos(0, 1, z + 2), Blocks.REDSTONE_BLOCK);
            for (int x = 1; x <= 2; x++) helper.setBlock(new BlockPos(x, 1, z + 2), dust);
            helper.setBlock(new BlockPos(3, 1, z + 2), Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST));
            helper.setBlock(new BlockPos(4, 1, z + 2), Blocks.STONE);
        }
        reshape(helper);
        helper.startSequence().thenIdle(8).thenExecute(() -> {
            for (int lane = 0; lane < 2; lane++) {
                int z = lane * 4;
                String line = lane == 0 ? "astral" : "nihil";
                helper.assertTrue(helper.getBlockState(new BlockPos(4, 1, z)).getValue(RedstoneLampBlock.LIT), line + " redstone did not light the lamp");
                helper.assertTrue(helper.getBlockState(new BlockPos(3, 1, z + 2)).getValue(PistonBaseBlock.EXTENDED), line + " redstone did not extend the piston");
                helper.assertTrue(helper.getBlockState(new BlockPos(5, 1, z + 2)).is(Blocks.STONE), line + ": the piston did not push the stone");
                helper.setBlock(new BlockPos(0, 1, z), Blocks.AIR);
            }
        }).thenIdle(8).thenExecute(() -> {
            for (int lane = 0; lane < 2; lane++) {
                helper.assertFalse(helper.getBlockState(new BlockPos(4, 1, lane * 4)).getValue(RedstoneLampBlock.LIT),
                        (lane == 0 ? "astral" : "nihil") + " lamp stayed lit without its source");
            }
        }).thenSucceed();
    }

    /**
     * Bridge Redstone links all three lines like one shared wire, one step weaker per piece in every direction, and
     * connects visibly to all of them - vanilla dust connects to it, but not to Astral or Nihil Redstone.
     */
    public static void bridgeLinksAllLines(GameTestHelper helper) {
        floor(helper);
        Block bridge = LineBlocks.BRIDGE_REDSTONE;
        Block[][] lanes = {
                {Blocks.REDSTONE_WIRE, Blocks.REDSTONE_WIRE, bridge, LineBlocks.ASTRAL_REDSTONE, LineBlocks.ASTRAL_REDSTONE},
                {LineBlocks.ASTRAL_REDSTONE, bridge, LineBlocks.NIHIL_REDSTONE, LineBlocks.NIHIL_REDSTONE},
                {LineBlocks.NIHIL_REDSTONE, bridge, Blocks.REDSTONE_WIRE, Blocks.REDSTONE_WIRE},
                {LineBlocks.ASTRAL_REDSTONE, bridge, bridge, Blocks.REDSTONE_WIRE}};
        for (int i = 0; i < lanes.length; i++) {
            helper.setBlock(new BlockPos(0, 1, i * 2), Blocks.REDSTONE_BLOCK);
            for (int x = 0; x < lanes[i].length; x++) helper.setBlock(new BlockPos(x + 1, 1, i * 2), lanes[i][x]);
        }
        // Looks (y=4): vanilla dust beside a bridge and beside Astral Redstone.
        helper.setBlock(new BlockPos(1, 4, 0), bridge);
        helper.setBlock(new BlockPos(1, 4, 1), Blocks.REDSTONE_WIRE);
        helper.setBlock(new BlockPos(4, 4, 0), LineBlocks.ASTRAL_REDSTONE);
        helper.setBlock(new BlockPos(4, 4, 1), Blocks.REDSTONE_WIRE);
        reshape(helper);
        helper.runAfterDelay(6, () -> {
            for (int i = 0; i < lanes.length; i++) {
                for (int x = 0; x < lanes[i].length; x++) {
                    int got = power(helper, new BlockPos(x + 1, 1, i * 2));
                    helper.assertTrue(got == 15 - x, "lane " + i + " piece " + x + " (" + name(lanes[i][x]) + ") has " + got + ", wanted " + (15 - x));
                }
            }
            helper.assertTrue(helper.getBlockState(new BlockPos(1, 4, 0)).getValue(SignalDustBlock.SOUTH) == RedstoneSide.SIDE,
                    "bridge does not connect to vanilla dust");
            helper.assertTrue(helper.getBlockState(new BlockPos(1, 4, 1)).getValue(SignalDustBlock.NORTH) == RedstoneSide.SIDE,
                    "vanilla dust does not connect to the bridge");
            helper.assertTrue(helper.getBlockState(new BlockPos(4, 4, 0)).getValue(SignalDustBlock.SOUTH) == RedstoneSide.NONE
                            && helper.getBlockState(new BlockPos(4, 4, 1)).getValue(SignalDustBlock.NORTH) == RedstoneSide.NONE,
                    "astral redstone and vanilla dust connect");
            helper.setBlock(new BlockPos(0, 1, 0), Blocks.AIR);
        });
        helper.runAfterDelay(14, () -> {
            for (int x = 1; x <= 5; x++) helper.assertTrue(power(helper, new BlockPos(x, 1, 0)) == 0,
                    "lane 0 kept power after the source went: piece " + x + " has " + power(helper, new BlockPos(x, 1, 0)));
            helper.succeed();
        });
    }



    /**
     * The Astral/Nihilith Lamp takes every line (N51: own line, the other line, redstone dust, Bridge Redstone and a
     * lever right beside it) and toggles like a copper bulb: the first pulse lights it, it stays lit when the signal
     * goes, the next pulse switches it off.
     */
    public static void lampsToggleOnEveryLine(GameTestHelper helper) {
        floor(helper);
        // {y, z, dust or null for a lever beside the lamp, lamp, should toggle}
        Object[][] lanes = {
                {1, 0, LineBlocks.ASTRAL_REDSTONE, LineBlocks.ASTRALIT_LAMP, true},
                {1, 2, LineBlocks.NIHIL_REDSTONE, LineBlocks.NIHILITH_LAMP, true},
                {1, 4, LineBlocks.NIHIL_REDSTONE, LineBlocks.ASTRALIT_LAMP, true},
                {1, 6, Blocks.REDSTONE_WIRE, LineBlocks.ASTRALIT_LAMP, true},
                {4, 0, LineBlocks.BRIDGE_REDSTONE, LineBlocks.NIHILITH_LAMP, true},
                {4, 3, LineBlocks.ASTRAL_REDSTONE, LineBlocks.NIHILITH_LAMP, true},
                {4, 6, null, LineBlocks.ASTRALIT_LAMP, true}};
        BlockState off = Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR);
        for (Object[] lane : lanes) {
            int y = (int) lane[0], z = (int) lane[1];
            helper.setBlock(new BlockPos(lane[2] == null ? 3 : 0, y, z), off);
            if (lane[2] != null) for (int x = 1; x <= 3; x++) helper.setBlock(new BlockPos(x, y, z), (Block) lane[2]);
            helper.setBlock(new BlockPos(4, y, z), (Block) lane[3]);
        }
        reshape(helper);
        Runnable[] flip = {null};
        boolean[] on = {false};
        flip[0] = () -> {
            on[0] = !on[0];
            for (Object[] lane : lanes) {
                helper.setBlock(new BlockPos(lane[2] == null ? 3 : 0, (int) lane[0], (int) lane[1]), off.setValue(LeverBlock.POWERED, on[0]));
            }
        };
        java.util.function.Consumer<Boolean> expectLit = lit -> {
            for (Object[] lane : lanes) {
                BlockPos lamp = new BlockPos(4, (int) lane[0], (int) lane[1]);
                boolean want = (boolean) lane[4] && lit;
                String what = (lane[2] == null ? "lever" : name((Block) lane[2])) + " -> " + name((Block) lane[3]);
                helper.assertTrue(helper.getBlockState(lamp).is((Block) lane[3]), what + ": lamp missing");
                helper.assertTrue(helper.getBlockState(lamp).getValue(EndSignalLampBlock.LIT) == want,
                        what + ": lit should be " + want + " but is " + helper.getBlockState(lamp).getValue(EndSignalLampBlock.LIT));
            }
        };
        helper.startSequence().thenIdle(4)
                .thenExecute(() -> expectLit.accept(false))
                .thenExecute(flip[0]).thenIdle(4).thenExecute(() -> expectLit.accept(true))
                .thenExecute(flip[0]).thenIdle(4).thenExecute(() -> expectLit.accept(true))
                .thenExecute(flip[0]).thenIdle(4).thenExecute(() -> expectLit.accept(false))
                .thenSucceed();
    }

    // ---- N51 (2026-10-13): every device takes every line; the line redstone blocks ----

    /** A device under test: how it is placed at a position and whether it reacted (End lamps: their stored input). */
    private record Device(String name, java.util.function.BiConsumer<GameTestHelper, BlockPos> place,
                          java.util.function.Predicate<BlockState> on) {
        static Device of(String name, BlockState state, java.util.function.Predicate<BlockState> on) {
            return new Device(name, (helper, pos) -> helper.setBlock(pos, state), current -> current.is(state.getBlock()) && on.test(current));
        }
    }

    /**
     * Vanilla devices, one mod machine and the End devices (lamps, pistons, rails). The Nihil piston comes second, so it
     * lands in an east slot (it pulls from two cells away; the west slot's neighbour lane would be in reach).
     */
    private static List<Device> devices() {
        List<Device> list = new ArrayList<>();
        list.add(Device.of("redstone_lamp", Blocks.REDSTONE_LAMP.defaultBlockState(), s -> s.getValue(RedstoneLampBlock.LIT)));
        list.add(new Device("nihil_piston", (helper, pos) -> {
            helper.setBlock(pos.below(), Blocks.OBSIDIAN);
            // It pulls from two cells away: keep the upper layer's floor (and what stands on it) in place.
            helper.setBlock(pos.above(2), Blocks.OBSIDIAN);
            helper.setBlock(pos, LineBlocks.NIHIL_PISTON);
        }, s -> s.getValue(EndSignalBlock.POWER) > 0));
        list.add(Device.of("piston", Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP),
                s -> s.getValue(PistonBaseBlock.EXTENDED)));
        list.add(Device.of("dispenser", Blocks.DISPENSER.defaultBlockState().setValue(net.minecraft.world.level.block.DispenserBlock.FACING, Direction.UP),
                s -> s.getValue(net.minecraft.world.level.block.DispenserBlock.TRIGGERED)));
        list.add(new Device("iron_door", (helper, pos) -> {
            BlockState door = Blocks.IRON_DOOR.defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.FACING, Direction.NORTH);
            helper.setBlock(pos, door);
            helper.setBlock(pos.above(), door.setValue(net.minecraft.world.level.block.DoorBlock.HALF,
                    net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        }, s -> s.getValue(net.minecraft.world.level.block.DoorBlock.OPEN)));
        // A repeater's FACING points at its input: the dust (or block) lies west of it.
        list.add(Device.of("repeater", Blocks.REPEATER.defaultBlockState().setValue(net.minecraft.world.level.block.RepeaterBlock.FACING, Direction.WEST),
                s -> s.getValue(net.minecraft.world.level.block.RepeaterBlock.POWERED)));
        list.add(Device.of("astralit_lamp", LineBlocks.ASTRALIT_LAMP.defaultBlockState(), s -> s.getValue(EndSignalLampBlock.POWER) > 0));
        list.add(Device.of("nihilith_lamp", LineBlocks.NIHILITH_LAMP.defaultBlockState(), s -> s.getValue(EndSignalLampBlock.POWER) > 0));
        list.add(new Device("astral_piston", (helper, pos) -> {
            helper.setBlock(pos.below(), Blocks.OBSIDIAN);
            helper.setBlock(pos, LineBlocks.ASTRAL_PISTON);
        }, s -> s.getValue(EndSignalBlock.POWER) > 0));
        list.add(Device.of("astral_rail", LineBlocks.ASTRAL_RAIL.defaultBlockState(), s -> s.getValue(EndRailBlock.POWERED)));
        list.add(Device.of("nihil_rail", LineBlocks.NIHIL_RAIL.defaultBlockState(), s -> s.getValue(EndRailBlock.POWERED)));
        // Simple Machines' Autonomous Crafter, when that sub-mod is present (looked up by id, no dependency).
        var crafter = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("simplemachines", "autonomous_crafter"));
        if (crafter != Blocks.AIR) {
            list.add(Device.of("autonomous_crafter", crafter.defaultBlockState(), s -> s.getValue(BlockStateProperties.TRIGGERED)));
        }
        return list;
    }

    /** Sixteen lanes in the 8x8 room: two per row (west x 0..2, east x 4..6), rows z 0/2/4/6, layers y 1 and 4. */
    private static List<BlockPos> slots() {
        List<BlockPos> slots = new ArrayList<>();
        for (int y : new int[]{1, 4}) for (int z = 0; z <= 6; z += 2) for (int x : new int[]{0, 4}) slots.add(new BlockPos(x, y, z));
        return slots;
    }

    /**
     * Builds one lane per device: {@code source} at the slot, {@code dust} (or nothing) in the middle, the device two
     * cells east; after a while every device must have reacted. With {@code dust == null} the source sits in the middle,
     * right next to the device.
     */
    private static void everyDeviceReacts(GameTestHelper helper, Block source, Block dust, String what) {
        floor(helper);
        List<Device> devices = devices();
        List<BlockPos> slots = slots();
        for (int i = 0; i < devices.size(); i++) {
            BlockPos slot = slots.get(i);
            if (dust != null) {
                helper.setBlock(slot, source);
                helper.setBlock(slot.east(), dust);
            } else {
                helper.setBlock(slot.east(), source);
            }
            devices.get(i).place().accept(helper, slot.east(2));
        }
        reshape(helper);
        // A diode only looks at its input on a neighbour update (setBlock placed it unpowered): poke every device.
        for (int i = 0; i < devices.size(); i++) helper.getLevel().updateNeighborsAt(helper.absolutePos(slots.get(i).east()), Blocks.AIR);
        helper.startSequence().thenIdle(12).thenExecute(() -> {
            for (int i = 0; i < devices.size(); i++) {
                BlockPos at = slots.get(i).east(2);
                helper.assertTrue(devices.get(i).on().test(helper.getBlockState(at)),
                        what + ": " + devices.get(i).name() + " did not react (" + helper.getBlockState(at) + ")");
            }
        }).thenSucceed();
    }

    /** N51: vanilla redstone dust powers every device, the End lamps, pistons and rails included. */
    public static void everyDeviceTakesRedstone(GameTestHelper helper) {
        everyDeviceReacts(helper, Blocks.REDSTONE_BLOCK, Blocks.REDSTONE_WIRE, "redstone_wire");
    }

    /** N51: Astral Redstone powers every device (vanilla, mod machine, both End lamps, pistons and rails). */
    public static void everyDeviceTakesAstral(GameTestHelper helper) {
        everyDeviceReacts(helper, Blocks.REDSTONE_BLOCK, LineBlocks.ASTRAL_REDSTONE, "astral_redstone");
    }

    /** N51: Nihil Redstone powers every device. */
    public static void everyDeviceTakesNihil(GameTestHelper helper) {
        everyDeviceReacts(helper, Blocks.REDSTONE_BLOCK, LineBlocks.NIHIL_REDSTONE, "nihil_redstone");
    }

    /** N51: Bridge Redstone powers every device. */
    public static void everyDeviceTakesBridge(GameTestHelper helper) {
        everyDeviceReacts(helper, Blocks.REDSTONE_BLOCK, LineBlocks.BRIDGE_REDSTONE, "bridge_redstone");
    }

    /** N51: each line redstone block powers every device right next to it, like the redstone block. */
    public static void lineBlocksPowerEveryDevice(GameTestHelper helper) {
        // The three blocks take turns in the same lanes: place, check, remove, wait.
        Block[] blocks = {LineBlocks.ASTRAL_REDSTONE_BLOCK, LineBlocks.NIHIL_REDSTONE_BLOCK, LineBlocks.BRIDGE_REDSTONE_BLOCK};
        floor(helper);
        List<Device> devices = devices();
        List<BlockPos> slots = slots();
        for (int i = 0; i < devices.size(); i++) devices.get(i).place().accept(helper, slots.get(i).east(2));
        var sequence = helper.startSequence();
        for (Block block : blocks) {
            sequence = sequence.thenExecute(() -> {
                for (int i = 0; i < devices.size(); i++) helper.setBlock(slots.get(i).east(), block);
            // Checked within the End pistons' cooldown: an Astral piston that fired has pushed the block away again.
            }).thenIdle(6).thenExecute(() -> {
                for (int i = 0; i < devices.size(); i++) {
                    BlockPos at = slots.get(i).east(2);
                    helper.assertTrue(devices.get(i).on().test(helper.getBlockState(at)),
                            name(block) + ": " + devices.get(i).name() + " did not react (" + helper.getBlockState(at) + ")");
                }
                for (int i = 0; i < devices.size(); i++) helper.setBlock(slots.get(i).east(), Blocks.AIR);
            }).thenIdle(12);
        }
        sequence.thenSucceed();
    }

    /**
     * N51: a line redstone block feeds dust of its own line and Bridge Redstone at full strength, never dust of another
     * line (the Bridge block feeds all three).
     */
    public static void lineBlocksFeedOnlyTheirLine(GameTestHelper helper) {
        floor(helper);
        Block[] blocks = {LineBlocks.ASTRAL_REDSTONE_BLOCK, LineBlocks.NIHIL_REDSTONE_BLOCK, LineBlocks.BRIDGE_REDSTONE_BLOCK};
        Block[] dusts = {Blocks.REDSTONE_WIRE, LineBlocks.ASTRAL_REDSTONE, LineBlocks.NIHIL_REDSTONE, LineBlocks.BRIDGE_REDSTONE};
        List<BlockPos> slots = slots();
        List<Block[]> lanes = new ArrayList<>();
        for (Block block : blocks) for (Block dust : dusts) lanes.add(new Block[]{block, dust});
        for (int i = 0; i < lanes.size(); i++) {
            helper.setBlock(slots.get(i), lanes.get(i)[0]);
            for (int x = 1; x <= 2; x++) helper.setBlock(slots.get(i).east(x), lanes.get(i)[1]);
        }
        reshape(helper);
        helper.runAfterDelay(6, () -> {
            for (int i = 0; i < lanes.size(); i++) {
                Block block = lanes.get(i)[0], dust = lanes.get(i)[1];
                boolean feeds = ((LineRedstoneBlock) block).line().links(SignalDustBlock.lineOf(dust.defaultBlockState()));
                int first = power(helper, slots.get(i).east()), second = power(helper, slots.get(i).east(2));
                String lane = name(block) + " -> " + name(dust);
                helper.assertTrue(feeds ? first == 15 && second == 14 : first == 0 && second == 0,
                        lane + (feeds ? " not fed" : " fed") + ": " + first + "/" + second);
            }
            helper.succeed();
        });
    }

    /**
     * B23-M1: an Astral/Nihil Redstone Torch feeds dust of its own line and Bridge Redstone like a redstone torch (15, then
     * 14), never redstone dust or the other line's dust.
     */
    public static void lineTorchesFeedOnlyTheirLine(GameTestHelper helper) {
        floor(helper);
        Block[] torches = {LineBlocks.ASTRAL_REDSTONE_TORCH, LineBlocks.NIHIL_REDSTONE_TORCH};
        Block[] dusts = {Blocks.REDSTONE_WIRE, LineBlocks.ASTRAL_REDSTONE, LineBlocks.NIHIL_REDSTONE, LineBlocks.BRIDGE_REDSTONE};
        List<BlockPos> slots = slots();
        List<Block[]> lanes = new ArrayList<>();
        for (Block torch : torches) for (Block dust : dusts) lanes.add(new Block[]{torch, dust});
        for (int i = 0; i < lanes.size(); i++) {
            helper.setBlock(slots.get(i), lanes.get(i)[0]);
            for (int x = 1; x <= 2; x++) helper.setBlock(slots.get(i).east(x), lanes.get(i)[1]);
        }
        reshape(helper);
        helper.runAfterDelay(6, () -> {
            for (int i = 0; i < lanes.size(); i++) {
                Block torch = lanes.get(i)[0], dust = lanes.get(i)[1];
                boolean feeds = ((LineSource) torch).line().links(SignalDustBlock.lineOf(dust.defaultBlockState()));
                int first = power(helper, slots.get(i).east()), second = power(helper, slots.get(i).east(2));
                String lane = name(torch) + " -> " + name(dust);
                helper.assertTrue(feeds ? first == 15 && second == 14 : first == 0 && second == 0,
                        lane + (feeds ? " not fed" : " fed") + ": " + first + "/" + second);
            }
            helper.succeed();
        });
    }

    /**
     * B23-M1: the line torch strongly powers the block above it like a redstone torch - its own line's dust beside that
     * block takes it, the other line's dust and redstone dust do not, a lamp does - and it is an inverter: a wall torch on a
     * block that Nihil dust powers goes out (any line switches it), and with it the Astral dust it fed.
     */
    public static void lineTorchPowersTheBlockAboveAndInverts(GameTestHelper helper) {
        floor(helper);
        // lower room: astral torch, stone above it, astral / nihil / redstone dust around that stone, a lamp touching it
        BlockPos torch = new BlockPos(2, 1, 2);
        BlockPos astral = new BlockPos(3, 2, 2), nihil = new BlockPos(1, 2, 2), wire = new BlockPos(2, 2, 3), lamp = new BlockPos(2, 2, 1);
        helper.setBlock(torch, LineBlocks.ASTRAL_REDSTONE_TORCH);
        helper.setBlock(lamp, Blocks.REDSTONE_LAMP); // before the stone: placing the stone updates the lamp
        helper.setBlock(torch.above(), Blocks.STONE);
        for (BlockPos dust : List.of(astral, nihil, wire)) helper.setBlock(dust.below(), Blocks.STONE);
        helper.setBlock(astral, LineBlocks.ASTRAL_REDSTONE);
        helper.setBlock(nihil, LineBlocks.NIHIL_REDSTONE);
        helper.setBlock(wire, Blocks.REDSTONE_WIRE);
        // upper room: stone with an astral wall torch on its east face, astral dust beside the torch, a redstone wire too
        BlockPos wall = new BlockPos(5, 4, 5);
        helper.setBlock(wall, Blocks.STONE);
        BlockPos wallTorch = wall.east();
        helper.setBlock(wallTorch, LineBlocks.ASTRAL_REDSTONE_WALL_TORCH.defaultBlockState()
                .setValue(net.minecraft.world.level.block.RedstoneWallTorchBlock.FACING, Direction.EAST));
        BlockPos fed = wallTorch.south(), notFed = wallTorch.north();
        helper.setBlock(fed, LineBlocks.ASTRAL_REDSTONE);
        helper.setBlock(notFed, Blocks.REDSTONE_WIRE);
        reshape(helper);
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(power(helper, astral) == 15, "astral dust beside the powered block: " + power(helper, astral));
            helper.assertTrue(power(helper, nihil) == 0, "nihil dust read the astral torch's block: " + power(helper, nihil));
            helper.assertTrue(power(helper, wire) == 0, "redstone dust read the astral torch's block: " + power(helper, wire));
            helper.assertTrue(helper.getBlockState(lamp).getValue(RedstoneLampBlock.LIT), "the lamp on the powered block is dark");
            helper.assertTrue(helper.getBlockState(wallTorch).getValue(net.minecraft.world.level.block.RedstoneTorchBlock.LIT), "wall torch lit");
            helper.assertTrue(power(helper, fed) == 15 && power(helper, notFed) == 0,
                    "wall torch feeds its line only: " + power(helper, fed) + "/" + power(helper, notFed));
            // nihil dust running into the stone switches the astral wall torch off
            helper.setBlock(new BlockPos(2, 4, 5), LineBlocks.NIHIL_REDSTONE_BLOCK);
            for (int x = 3; x <= 4; x++) helper.setBlock(new BlockPos(x, 4, 5), LineBlocks.NIHIL_REDSTONE);
            reshape(helper);
        });
        helper.runAfterDelay(14, () -> {
            helper.assertTrue(!helper.getBlockState(wallTorch).getValue(net.minecraft.world.level.block.RedstoneTorchBlock.LIT),
                    "nihil-powered block did not switch the astral torch off");
            helper.assertTrue(power(helper, fed) == 0, "astral dust still fed by the torch that went out: " + power(helper, fed));
            helper.succeed();
        });
    }

    /** B23-M1: Astral/Nihil Redstone over a stick crafts the torch; the wall torch drops the torch item. */
    public static void lineTorchRecipeAndDrop(GameTestHelper helper) {
        var recipes = helper.getLevel().getServer().getRecipeManager();
        Block[][] pairs = {{LineBlocks.ASTRAL_REDSTONE, LineBlocks.ASTRAL_REDSTONE_TORCH, LineBlocks.ASTRAL_REDSTONE_WALL_TORCH},
                {LineBlocks.NIHIL_REDSTONE, LineBlocks.NIHIL_REDSTONE_TORCH, LineBlocks.NIHIL_REDSTONE_WALL_TORCH}};
        for (Block[] pair : pairs) {
            var input = net.minecraft.world.item.crafting.CraftingInput.of(1, 2,
                    List.of(new net.minecraft.world.item.ItemStack(pair[0].asItem()), new net.minecraft.world.item.ItemStack(Items.STICK)));
            var out = recipes.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, helper.getLevel())
                    .map(r -> r.value().assemble(input)).orElse(net.minecraft.world.item.ItemStack.EMPTY);
            helper.assertTrue(out.is(pair[1].asItem()) && out.getCount() == 1, name(pair[0]) + " over a stick gave " + out);
            helper.assertTrue(pair[2].asItem() == pair[1].asItem() || pair[2].asItem() == Items.AIR, "wall torch has no own item");
            helper.assertTrue(pair[2].getLootTable().equals(pair[1].getLootTable()), "wall torch drops the torch");
            helper.assertTrue(pair[2].getDescriptionId().equals(pair[1].getDescriptionId()), "wall torch is named like the torch");
        }
        helper.succeed();
    }

    /** N51: nine dust craft the block and the block gives the nine back, for every line. */
    public static void lineBlockRecipesRoundTrip(GameTestHelper helper) {
        Block[][] pairs = {{LineBlocks.ASTRAL_REDSTONE, LineBlocks.ASTRAL_REDSTONE_BLOCK}, {LineBlocks.NIHIL_REDSTONE, LineBlocks.NIHIL_REDSTONE_BLOCK},
                {LineBlocks.BRIDGE_REDSTONE, LineBlocks.BRIDGE_REDSTONE_BLOCK}};
        var recipes = helper.getLevel().getServer().getRecipeManager();
        for (Block[] pair : pairs) {
            var nine = net.minecraft.world.item.crafting.CraftingInput.of(3, 3,
                    java.util.Collections.nCopies(9, new net.minecraft.world.item.ItemStack(pair[0].asItem())));
            var packed = recipes.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, nine, helper.getLevel())
                    .map(r -> r.value().assemble(nine)).orElse(net.minecraft.world.item.ItemStack.EMPTY);
            helper.assertTrue(packed.is(pair[1].asItem()) && packed.getCount() == 1, "nine " + name(pair[0]) + " gave " + packed);
            var one = net.minecraft.world.item.crafting.CraftingInput.of(1, 1, List.of(new net.minecraft.world.item.ItemStack(pair[1].asItem())));
            var unpacked = recipes.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, one, helper.getLevel())
                    .map(r -> r.value().assemble(one)).orElse(net.minecraft.world.item.ItemStack.EMPTY);
            helper.assertTrue(unpacked.is(pair[0].asItem()) && unpacked.getCount() == 9, name(pair[1]) + " gave " + unpacked);
        }
        helper.succeed();
    }

    /**
     * SP22-5 (owner 22.10.): every line block has its own sound type (never amethyst any more); every event is registered
     * and in the 26.3 sounds.json with a subtitle, every own file is there (Ogg), Vanilla only as a referenced event, and
     * the own subtitles exist in English and German.
     */
    public static void lineBlocksHaveOwnSounds(GameTestHelper helper) {
        Object[][] expected = {
                {LineBlocks.ASTRAL_REDSTONE, LineSounds.ASTRAL_DUST}, {LineBlocks.NIHIL_REDSTONE, LineSounds.NIHIL_DUST},
                {LineBlocks.BRIDGE_REDSTONE, LineSounds.BRIDGE_DUST}, {LineBlocks.ASTRALIT_LAMP, LineSounds.ASTRAL_LAMP},
                {LineBlocks.NIHILITH_LAMP, LineSounds.NIHIL_LAMP}, {LineBlocks.ASTRAL_PISTON, LineSounds.ASTRAL_PISTON},
                {LineBlocks.NIHIL_PISTON, LineSounds.NIHIL_PISTON}};
        List<String> problems = new ArrayList<>();
        for (Object[] row : expected) {
            Block block = (Block) row[0];
            SoundType sound = block.defaultBlockState().getSoundType();
            if (sound == SoundType.AMETHYST) problems.add(name(block) + " still sounds like amethyst");
            if (sound != row[1]) problems.add(name(block) + " has the wrong sound type");
            if (!sound.getBreakSound().location().getPath().equals("block." + name(block) + ".break")) {
                problems.add(name(block) + " breaks with " + sound.getBreakSound().location());
            }
        }
        JsonObject sounds = json("/assets/simpleores/sounds.json", problems);
        JsonObject en = json("/assets/simpleores/lang/en_us.json", problems);
        JsonObject de = json("/assets/simpleores/lang/de_de.json", problems);
        for (SoundEvent event : LineSounds.EVENTS) {
            Identifier id = event.location();
            if (BuiltInRegistries.SOUND_EVENT.getValue(id) != event) problems.add(id + " is not registered");
            JsonObject spec = sounds.getAsJsonObject(id.getPath());
            if (spec == null) {
                problems.add(id + " is missing in sounds.json");
                continue;
            }
            String subtitle = spec.has("subtitle") ? spec.get("subtitle").getAsString() : "";
            if (subtitle.startsWith("subtitles.simpleores.")) {
                if (!en.has(subtitle) || !de.has(subtitle)) problems.add(subtitle + " lacks English or German text");
            } else if (!subtitle.startsWith("subtitles.block.generic.")) {
                problems.add(id + " has no subtitle");
            }
            for (JsonElement entry : spec.getAsJsonArray("sounds")) {
                JsonObject o = entry.isJsonObject() ? entry.getAsJsonObject() : null;
                Identifier file = Identifier.parse(o == null ? entry.getAsString() : o.get("name").getAsString());
                if (o != null && o.has("type") && o.get("type").getAsString().equals("event")) {
                    if (!BuiltInRegistries.SOUND_EVENT.containsKey(file)) problems.add(id + " refers to unknown " + file);
                    continue;
                }
                if (!file.getNamespace().equals(SimpleOres.MOD_ID)) {
                    problems.add(id + " plays a foreign file " + file);
                    continue;
                }
                String path = "/assets/simpleores/sounds/" + file.getPath() + ".ogg";
                try (InputStream in = LineTests.class.getResourceAsStream(path)) {
                    if (in == null || !new String(in.readNBytes(4), StandardCharsets.US_ASCII).equals("OggS")) {
                        problems.add(path + " is missing or no Ogg file");
                    }
                } catch (IOException e) {
                    problems.add(path + " unreadable: " + e);
                }
            }
        }
        helper.assertTrue(problems.isEmpty(), problems.size() + " line sound problems: " + problems);
        helper.succeed();
    }

    private static JsonObject json(String resource, List<String> problems) {
        try (InputStream in = LineTests.class.getResourceAsStream(resource)) {
            if (in != null) return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            problems.add(resource + " missing");
        } catch (IOException e) {
            problems.add(resource + " unreadable: " + e);
        }
        return new JsonObject();
    }

    public static void redstoneRecipesYieldTwo(GameTestHelper helper) {
        for (var entry : java.util.Map.of(OresRegistry.item("astralit_dust"),
                OresRegistry.item("astral_redstone"), OresRegistry.item("nihilith_shard"),
                OresRegistry.item("nihil_redstone")).entrySet()) {
            var input = net.minecraft.world.item.crafting.CraftingInput.of(2, 1,
                    java.util.List.of(new ItemStack(entry.getKey()), new ItemStack(Items.REDSTONE)));
            var recipe = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                    net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow();
            ItemStack result = recipe.value().assemble(input);
            helper.assertTrue(result.is(entry.getValue()) && result.getCount() == 2, "redstone recipe must yield two: " + result);
        }
        helper.succeed();
    }

    public static void channelsStayIsolatedAndStopAtFifteen(GameTestHelper helper) {
        // Fold the 16-segment path into the loader's 8x8 room, with one-block gaps.
        var path = java.util.List.of(new BlockPos(1,1,1), new BlockPos(2,1,1), new BlockPos(3,1,1), new BlockPos(4,1,1), new BlockPos(5,1,1),
                new BlockPos(5,1,2), new BlockPos(5,1,3), new BlockPos(4,1,3), new BlockPos(3,1,3), new BlockPos(2,1,3), new BlockPos(1,1,3),
                new BlockPos(1,1,4), new BlockPos(1,1,5), new BlockPos(2,1,5), new BlockPos(3,1,5), new BlockPos(4,1,5));
        for (int x = 0; x <= 6; x++) for (int z = 0; z <= 6; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        // Since 2026-10-11 a redstone block feeds Nihil Redstone like vanilla dust; Astral next to it stays dark.
        helper.setBlock(new BlockPos(0, 1, 1), Blocks.REDSTONE_BLOCK);
        for (var pos : path) helper.setBlock(pos, LineBlocks.NIHIL_REDSTONE);
        for (int x = 1; x <= 5; x++) helper.setBlock(new BlockPos(x, 1, 6), LineBlocks.ASTRAL_REDSTONE);
        helper.setBlock(new BlockPos(5, 1, 5), Blocks.REDSTONE_LAMP);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(helper.getBlockState(path.get(0)).getValue(EndSignalBlock.POWER) == 15, "redstone block did not feed nihil redstone");
            helper.assertTrue(helper.getBlockState(path.get(14)).getValue(EndSignalBlock.POWER) == 1, "fifteenth segment not powered");
            helper.assertTrue(helper.getBlockState(path.get(15)).getValue(EndSignalBlock.POWER) == 0, "sixteenth segment powered");
            helper.assertFalse(helper.getBlockState(new BlockPos(5, 1, 5)).getValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT), "out-of-range lamp lit");
            for (int x = 1; x <= 5; x++) helper.assertTrue(helper.getBlockState(new BlockPos(x, 1, 6)).getValue(EndSignalBlock.POWER) == 0, "astral took nihil power");
            helper.setBlock(new BlockPos(0, 1, 1), Blocks.AIR);
        });
        helper.runAfterDelay(180, () -> {
            for (var pos : path) helper.assertTrue(helper.getBlockState(pos).getValue(EndSignalBlock.POWER) == 0, "signal latched after source turned off");
            helper.succeed();
        });
    }

    /**
     * Astral/Nihil Redstone lays out like vanilla dust (owner 2026-10-02): an untouched dot stays a dot,
     * two pieces form a straight line, powder on top of a block makes the lower one climb the wall
     * ("up") and the upper one reach down, the signal crosses that step losing one per piece, and
     * nothing connects to the other channel or to vanilla redstone dust.
     */
    public static void powderConnectsLikeRedstoneWire(GameTestHelper helper) {
        for (int x = 0; x <= 6; x++) for (int z = 0; z <= 6; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 1, 5), LineBlocks.NIHIL_REDSTONE);
        helper.setBlock(new BlockPos(3, 1, 1), LineBlocks.NIHIL_REDSTONE);
        helper.setBlock(new BlockPos(4, 1, 1), LineBlocks.NIHIL_REDSTONE);
        helper.setBlock(new BlockPos(1, 1, 3), LineBlocks.ASTRAL_REDSTONE);
        helper.setBlock(new BlockPos(2, 1, 3), LineBlocks.NIHIL_REDSTONE);
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.REDSTONE_WIRE);
        helper.setBlock(new BlockPos(5, 1, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 1, 4), Blocks.REDSTONE_BLOCK);
        helper.setBlock(new BlockPos(4, 1, 4), LineBlocks.NIHIL_REDSTONE);
        helper.setBlock(new BlockPos(5, 2, 4), LineBlocks.NIHIL_REDSTONE);
        reshape(helper);
        helper.runAfterDelay(20, () -> {
            var dot = helper.getBlockState(new BlockPos(1, 1, 5));
            helper.assertTrue(sides(dot).equals("none none none none"), "lone powder is not a dot: " + sides(dot));
            helper.assertTrue(sides(helper.getBlockState(new BlockPos(3, 1, 1))).equals("none side none side"), "two pieces are no east-west line: " + sides(helper.getBlockState(new BlockPos(3, 1, 1))));
            helper.assertTrue(sides(helper.getBlockState(new BlockPos(2, 1, 3))).equals("none none none none"), "nihil redstone connected to astral redstone or vanilla dust: " + sides(helper.getBlockState(new BlockPos(2, 1, 3))));
            helper.assertTrue(helper.getBlockState(new BlockPos(1, 1, 3)).getValue(SignalDustBlock.EAST)
                    == net.minecraft.world.level.block.state.properties.RedstoneSide.NONE, "astral powder connected to nihil powder");
            var lower = helper.getBlockState(new BlockPos(4, 1, 4));
            helper.assertTrue(sides(lower).equals("none up none side"), "powder does not climb the wall: " + sides(lower));
            var upper = helper.getBlockState(new BlockPos(5, 2, 4));
            helper.assertTrue(upper.getValue(SignalDustBlock.WEST)
                    == net.minecraft.world.level.block.state.properties.RedstoneSide.SIDE, "upper powder does not reach down: " + sides(upper));
            int range = 15;
            helper.assertTrue(lower.getValue(EndSignalBlock.POWER) == range, "powder next to the redstone block has " + lower.getValue(EndSignalBlock.POWER));
            helper.assertTrue(upper.getValue(EndSignalBlock.POWER) == range - 1, "signal did not climb the step: " + upper.getValue(EndSignalBlock.POWER));
            helper.assertTrue(helper.getBlockState(new BlockPos(2, 1, 3)).getValue(EndSignalBlock.POWER) == 0, "nihil powder took vanilla or astral power");
            helper.succeed();
        });
    }

    // ----- Astral-/Nihil-Kolben (docs/ai/PLAN-ASTRAL-KOLBEN-2026-10-02.md, Abschnitt 9) -----

    private static final BlockPos PISTON = new BlockPos(3, 3, 3);

    private static int fire(GameTestHelper helper, BlockPos rel, boolean astral) {
        return EndPistonMoves.fire(helper.getLevel(), helper.absolutePos(rel), astral);
    }

    private static boolean isMoving(GameTestHelper helper, BlockPos rel) {
        return helper.getBlockState(rel).is(Blocks.MOVING_PISTON);
    }

    /** Stones and moving blocks carrying stone in the room: a move must neither add nor lose one. */
    private static int stones(GameTestHelper helper) {
        int count = 0;
        for (BlockPos rel : BlockPos.betweenClosed(0, 1, 0, 7, 7, 7)) { // y >= 1: the arena floor is stone too
            var state = helper.getBlockState(rel);
            if (state.is(Blocks.STONE)) count++;
            else if (state.is(Blocks.MOVING_PISTON)
                    && helper.getLevel().getBlockEntity(helper.absolutePos(rel)) instanceof net.minecraft.world.level.block.piston.PistonMovingBlockEntity moving
                    && moving.getMovedState().is(Blocks.STONE)) count++;
        }
        return count;
    }

    private static int droppedItems(GameTestHelper helper) {
        var corner = Vec3.atLowerCornerOf(helper.absolutePos(BlockPos.ZERO));
        return helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(corner.add(-2, -2, -2), corner.add(10, 10, 10))).size();
    }

    /** The Astral piston pushes all six neighbours one cell, in one call, and nothing further. */
    public static void pistonPushesAllSixAtOnce(GameTestHelper helper) {
        helper.setBlock(PISTON, LineBlocks.ASTRAL_PISTON);
        for (Direction d : Direction.values()) helper.setBlock(PISTON.relative(d), Blocks.STONE);
        helper.assertTrue(fire(helper, PISTON, true) == 6, "astral piston did not move all six neighbours");
        for (Direction d : Direction.values()) {
            helper.assertTrue(isMoving(helper, PISTON.relative(d, 2)), "no moving block towards " + d);
            helper.assertTrue(helper.getBlockState(PISTON.relative(d)).isAir(), "source left behind towards " + d);
        }
        helper.runAfterDelay(8, () -> {
            for (Direction d : Direction.values()) {
                helper.assertTrue(helper.getBlockState(PISTON.relative(d, 2)).is(Blocks.STONE), "stone did not land towards " + d);
                helper.assertTrue(helper.getBlockState(PISTON.relative(d)).isAir(), "neighbour cell not empty towards " + d);
            }
            helper.assertTrue(helper.getBlockState(PISTON).is(LineBlocks.ASTRAL_PISTON), "the piston itself moved");
            helper.assertTrue(stones(helper) == 6 && droppedItems(helper) == 0, "stones were duplicated, lost or dropped");
            helper.succeed();
        });
    }

    /** A taken target stops that side: no chain, the block stays; a liquid source is never replaced; grass is. */
    public static void pistonNeverChains(GameTestHelper helper) {
        helper.setBlock(PISTON, LineBlocks.ASTRAL_PISTON);
        helper.setBlock(new BlockPos(4, 3, 3), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 3, 3), Blocks.DIRT);
        helper.setBlock(new BlockPos(3, 4, 3), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 5, 3), Blocks.WATER);
        helper.setBlock(new BlockPos(1, 2, 3), Blocks.DIRT);
        helper.setBlock(new BlockPos(2, 3, 3), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 3, 3), Blocks.SHORT_GRASS);
        helper.assertTrue(fire(helper, PISTON, true) == 1, "only the side towards the grass may move");
        helper.assertTrue(helper.getBlockState(new BlockPos(4, 3, 3)).is(Blocks.STONE)
                && helper.getBlockState(new BlockPos(5, 3, 3)).is(Blocks.DIRT)
                && helper.getBlockState(new BlockPos(6, 3, 3)).isAir(), "a row was pushed (chain)");
        helper.assertTrue(helper.getBlockState(new BlockPos(3, 4, 3)).is(Blocks.STONE)
                && helper.getBlockState(new BlockPos(3, 5, 3)).getFluidState().isSource(), "a water source was replaced");
        helper.assertTrue(isMoving(helper, new BlockPos(1, 3, 3)), "replaceable grass target was not taken");
        helper.succeed();
    }

    /** The Nihil piston pulls the block across a one-cell gap; a block already touching it stays. */
    public static void nihilPullsAcrossTheGap(GameTestHelper helper) {
        helper.setBlock(PISTON, LineBlocks.NIHIL_PISTON);
        for (Direction d : Direction.values()) {
            if (d != Direction.EAST) helper.setBlock(PISTON.relative(d, 2), Blocks.STONE);
        }
        helper.setBlock(new BlockPos(4, 3, 3), Blocks.DIRT);
        helper.setBlock(new BlockPos(5, 3, 3), Blocks.STONE);
        helper.assertTrue(fire(helper, PISTON, false) == 5, "nihil piston did not pull the five gapped blocks");
        helper.runAfterDelay(8, () -> {
            for (Direction d : Direction.values()) {
                if (d == Direction.EAST) continue;
                helper.assertTrue(helper.getBlockState(PISTON.relative(d)).is(Blocks.STONE), "stone not pulled from " + d);
                helper.assertTrue(helper.getBlockState(PISTON.relative(d, 2)).isAir(), "stone left at distance two " + d);
            }
            helper.assertTrue(helper.getBlockState(new BlockPos(4, 3, 3)).is(Blocks.DIRT)
                    && helper.getBlockState(new BlockPos(5, 3, 3)).is(Blocks.STONE), "touching block moved or was passed");
            helper.assertTrue(stones(helper) == 6 && droppedItems(helper) == 0, "stones were duplicated, lost or dropped");
            helper.succeed();
        });
    }

    /**
     * Obsidian, bedrock, a chest (block entity), a door (two parts, DESTROY), a torch (DESTROY) and a
     * vanilla piston stay where they are, nothing is broken; glazed terracotta (PUSH_ONLY) is pushed by
     * the Astral piston but never pulled by the Nihil piston.
     */
    public static void pistonLeavesImmovablesAlone(GameTestHelper helper) {
        helper.setBlock(PISTON, LineBlocks.ASTRAL_PISTON);
        helper.setBlock(new BlockPos(4, 3, 3), Blocks.OBSIDIAN);
        helper.setBlock(new BlockPos(2, 3, 3), Blocks.CHEST);
        helper.setBlock(new BlockPos(3, 4, 3), Blocks.BEDROCK);
        helper.setBlock(new BlockPos(3, 2, 3), Blocks.PISTON);
        helper.setBlock(new BlockPos(3, 3, 4), Blocks.OAK_DOOR.defaultBlockState());
        helper.setBlock(new BlockPos(3, 4, 4), Blocks.OAK_DOOR.defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF,
                        net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        helper.setBlock(new BlockPos(3, 2, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 3, 2), Blocks.TORCH);
        helper.assertTrue(fire(helper, PISTON, true) == 0, "an immovable block was moved");
        helper.assertTrue(helper.getBlockState(new BlockPos(4, 3, 3)).is(Blocks.OBSIDIAN)
                && helper.getBlockState(new BlockPos(2, 3, 3)).is(Blocks.CHEST)
                && helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(2, 3, 3))) != null
                && helper.getBlockState(new BlockPos(3, 4, 3)).is(Blocks.BEDROCK)
                && helper.getBlockState(new BlockPos(3, 2, 3)).is(Blocks.PISTON)
                && helper.getBlockState(new BlockPos(3, 3, 4)).is(Blocks.OAK_DOOR)
                && helper.getBlockState(new BlockPos(3, 4, 4)).is(Blocks.OAK_DOOR)
                && helper.getBlockState(new BlockPos(3, 3, 2)).is(Blocks.TORCH), "an immovable block changed");
        helper.assertTrue(droppedItems(helper) == 0, "an immovable block was broken");
        // PUSH_ONLY: Astral pushes it away, Nihil does not pull it.
        helper.setBlock(new BlockPos(4, 3, 3), Blocks.GLAZED_TERRACOTTA.white());
        helper.assertTrue(fire(helper, PISTON, true) == 1 && isMoving(helper, new BlockPos(5, 3, 3)), "astral did not push glazed terracotta");
        BlockPos nihil = new BlockPos(3, 3, 6);
        helper.setBlock(nihil, LineBlocks.NIHIL_PISTON);
        helper.setBlock(new BlockPos(1, 3, 6), Blocks.GLAZED_TERRACOTTA.white());
        helper.assertTrue(fire(helper, nihil, false) == 0, "nihil pulled glazed terracotta");
        helper.succeed();
    }

    /** Two pistons on one block, or on one target, in the same tick: exactly one move, nothing duplicated. */
    public static void twoPistonsMoveOneBlockOnce(GameTestHelper helper) {
        // Same block: A pushes it east, B pushes it south.
        helper.setBlock(new BlockPos(1, 2, 2), LineBlocks.ASTRAL_PISTON);
        helper.setBlock(new BlockPos(2, 2, 1), LineBlocks.ASTRAL_PISTON);
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.STONE);
        int first = fire(helper, new BlockPos(1, 2, 2), true) + fire(helper, new BlockPos(2, 2, 1), true);
        helper.assertTrue(first == 1, "one block moved " + first + " times in one tick");
        // Same target: A and B push towards (4, 5, 4) from both sides.
        helper.setBlock(new BlockPos(2, 5, 4), LineBlocks.ASTRAL_PISTON);
        helper.setBlock(new BlockPos(3, 5, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(6, 5, 4), LineBlocks.ASTRAL_PISTON);
        helper.setBlock(new BlockPos(5, 5, 4), Blocks.STONE);
        int second = fire(helper, new BlockPos(2, 5, 4), true) + fire(helper, new BlockPos(6, 5, 4), true);
        helper.assertTrue(second == 1, "two blocks moved into one cell: " + second);
        helper.assertTrue(stones(helper) == 3, "stones were duplicated or lost: " + stones(helper));
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(stones(helper) == 3 && droppedItems(helper) == 0, "stones were duplicated, lost or dropped after landing");
            helper.succeed();
        });
    }

    /** Rising edge only: a held signal fires once; the cooldown is capped 4..100; switched off, nothing moves. */
    public static void pistonFiresOnRisingEdgeOnly(GameTestHelper helper) {
        int beforeCooldown = OresConfig.endPistonCooldownTicks;
        boolean before = OresConfig.endPistons;
        try {
            OresConfig.endPistonCooldownTicks = 1;
            helper.assertTrue(EndPistonBlock.cooldown() == 4, "cooldown floor bypassed");
            OresConfig.endPistonCooldownTicks = 999;
            helper.assertTrue(EndPistonBlock.cooldown() == 100, "cooldown cap bypassed");
            OresConfig.endPistonCooldownTicks = 8;
            for (int x = 0; x <= 7; x++) for (int z = 0; z <= 7; z++) helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            helper.setBlock(new BlockPos(3, 0, 3), Blocks.STONE);
            // The source: a redstone block at sw feeding the Astral Redstone between it and the piston.
            BlockPos piston = new BlockPos(3, 2, 3), sw = new BlockPos(1, 2, 3);
            helper.setBlock(piston, LineBlocks.ASTRAL_PISTON);
            helper.setBlock(new BlockPos(2, 2, 3), LineBlocks.ASTRAL_REDSTONE);
            helper.setBlock(new BlockPos(4, 2, 3), Blocks.STONE);
            Runnable tick = () -> {
                var pos = helper.absolutePos(piston);
                helper.getLevel().getBlockState(pos).tick(helper.getLevel(), pos, helper.getLevel().getRandom());
            };
            helper.setBlock(sw, Blocks.REDSTONE_BLOCK);
            tick.run();
            helper.assertTrue(helper.getBlockState(piston).getValue(EndSignalBlock.POWER) > 0, "piston does not receive its line");
            helper.assertTrue(isMoving(helper, new BlockPos(5, 2, 3)), "rising edge did not fire");
            helper.setBlock(new BlockPos(3, 3, 3), Blocks.STONE);
            tick.run();
            helper.assertTrue(helper.getBlockState(new BlockPos(3, 3, 3)).is(Blocks.STONE), "held signal fired again");
            helper.setBlock(sw, Blocks.AIR);
            tick.run();
            helper.assertTrue(helper.getBlockState(piston).getValue(EndSignalBlock.POWER) == 0, "signal latched after the source was removed");
            helper.setBlock(sw, Blocks.REDSTONE_BLOCK);
            tick.run();
            helper.assertTrue(isMoving(helper, new BlockPos(3, 4, 3)), "second rising edge did not fire");
            OresConfig.endPistons = false;
            helper.setBlock(sw, Blocks.AIR);
            tick.run();
            helper.setBlock(new BlockPos(3, 2, 4), Blocks.STONE);
            helper.setBlock(sw, Blocks.REDSTONE_BLOCK);
            tick.run();
            helper.assertTrue(helper.getBlockState(piston).getValue(EndSignalBlock.POWER) == 0
                    && helper.getBlockState(new BlockPos(3, 2, 4)).is(Blocks.STONE), "disabled piston moved or took power");
        } finally { OresConfig.endPistonCooldownTicks = beforeCooldown; OresConfig.endPistons = before; }
        helper.succeed();
    }

    /** N51: a redstone block and the other line's dust trigger an End piston too; its own line's powder visibly connects. */
    public static void pistonTakesEveryLine(GameTestHelper helper) {
        for (int x = 0; x <= 7; x++) for (int z = 0; z <= 7; z++) helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        BlockPos astral = new BlockPos(2, 2, 2), nihil = new BlockPos(5, 2, 5);
        helper.setBlock(astral, LineBlocks.ASTRAL_PISTON);
        helper.setBlock(astral.west(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(astral.east(), LineBlocks.NIHIL_REDSTONE);
        helper.setBlock(astral.east(2), Blocks.REDSTONE_BLOCK);
        helper.setBlock(astral.above(), Blocks.STONE);
        helper.setBlock(nihil, LineBlocks.NIHIL_PISTON);
        helper.setBlock(nihil.west(), LineBlocks.ASTRAL_REDSTONE);
        helper.setBlock(nihil.west(2), Blocks.REDSTONE_BLOCK);
        helper.setBlock(nihil.above(2), Blocks.STONE);
        helper.setBlock(astral.north(), LineBlocks.ASTRAL_REDSTONE);
        reshape(helper);
        for (BlockPos rel : List.of(astral, nihil, astral.north())) {
            var pos = helper.absolutePos(rel);
            helper.getLevel().getBlockState(pos).tick(helper.getLevel(), pos, helper.getLevel().getRandom());
        }
        helper.assertTrue(helper.getBlockState(astral).getValue(EndSignalBlock.POWER) > 0, "astral piston ignored a redstone block and nihil redstone");
        helper.assertTrue(helper.getBlockState(nihil).getValue(EndSignalBlock.POWER) > 0, "nihil piston ignored astral redstone");
        helper.assertTrue(helper.getBlockState(astral.north()).getValue(SignalDustBlock.SOUTH)
                .isConnected(), "astral powder does not connect to the astral piston");
        helper.assertTrue(helper.getBlockState(astral.north()).getValue(EndSignalBlock.POWER) == 0, "piston passed a signal on");
        helper.succeed();
    }

    private static String sides(net.minecraft.world.level.block.state.BlockState state) {
        return state.getValue(SignalDustBlock.NORTH).getSerializedName() + " "
                + state.getValue(SignalDustBlock.EAST).getSerializedName() + " "
                + state.getValue(SignalDustBlock.SOUTH).getSerializedName() + " "
                + state.getValue(SignalDustBlock.WEST).getSerializedName();
    }

    // ------------------------------------------------------------------------------------------------
    // Astral-/Nihil-Schienen (2026-10-04, docs/ai/PLAN-ASTRAL-NIHIL-SCHIENEN-2026-10-02.md)
    // ------------------------------------------------------------------------------------------------

    private static net.minecraft.world.level.block.state.BlockState eastWest(net.minecraft.world.level.block.Block rail) {
        return rail.defaultBlockState().setValue(EndRailBlock.SHAPE, RailShape.EAST_WEST);
    }

    private static boolean powered(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getValue(EndRailBlock.POWERED);
    }

    /**
     * The boost formula: speed rises every tick, never passes the cap (stays strictly below it until the gap is below
     * double precision) and gets within 2 % of it; every tick adds
     * less the faster the cart already is ("the faster, the more boost you need"); a stronger boost is faster after
     * the same number of ticks; momentum above the cap is cut to the cap.
     */
    public static void astralBoostApproachesTopSpeedWithoutPassingIt(GameTestHelper helper) {
        double[] caps = {0.4, 0.8, 1.0, 1.0 / 0.75};
        double[] boosts = {EndRailPhysics.MIN_BOOST, 0.12, EndRailPhysics.MAX_BOOST};
        for (double cap : caps) {
            double previousAfterTen = -1;
            for (double boost : boosts) {
                double v = 0, gain = Double.MAX_VALUE, afterTen = 0;
                for (int tick = 1; tick <= 2000; tick++) {
                    double next = EndRailPhysics.boosted(v, cap, boost);
                    // Never above the cap; strictly below it until the remaining gap is beyond double precision.
                    helper.assertTrue(next <= cap && (next < cap || cap - v < 1e-9),
                            "speed reached or passed the cap " + cap + " (boost " + boost + ", tick " + tick + ")");
                    helper.assertTrue(next > v || v > cap * 0.999, "speed did not rise at " + v + " (cap " + cap + ")");
                    helper.assertTrue(next - v <= gain + 1e-12, "gain grew with speed at " + v + " (cap " + cap + ")");
                    gain = next - v;
                    v = next;
                    if (tick == 10) afterTen = v;
                }
                helper.assertTrue(v > cap * 0.98, "speed stalled at " + v + " below the cap " + cap);
                helper.assertTrue(afterTen >= previousAfterTen, "a stronger boost was not faster (cap " + cap + ", boost " + boost + ")");
                previousAfterTen = afterTen;
            }
        }
        helper.assertTrue(EndRailPhysics.boosted(2.0, 0.8, 0.12) == 0.8, "momentum above the cap was not cut");
        helper.succeed();
    }

    /** Astral pushes harder than vanilla's powered rail (0.06 per tick) at every vanilla speed, even at the lowest config. */
    public static void astralBoostsMoreThanThePoweredRail(GameTestHelper helper) {
        double cap = EndRailPhysics.maxSpeedPerTick();
        double boost = EndRailPhysics.boost();
        for (double v = 0; v <= EndRailPhysics.VANILLA_MAX_SPEED + 1e-9; v += 0.05) {
            helper.assertTrue(EndRailPhysics.boosted(v, cap, boost) - v > EndRailPhysics.VANILLA_POWERED_RAIL_BOOST,
                    "default Astral boost at " + v + " is not above the powered rail");
        }
        double minCap = EndRailPhysics.MIN_MAX_SPEED / 20.0;
        helper.assertTrue(EndRailPhysics.boosted(0, minCap, EndRailPhysics.MIN_BOOST) > EndRailPhysics.VANILLA_POWERED_RAIL_BOOST,
                "lowest config Astral boost from standstill is not above the powered rail");
        helper.assertTrue(cap > EndRailPhysics.VANILLA_MAX_SPEED, "default top speed is not above vanilla");
        helper.succeed();
    }

    /** Powered Nihil: the speed only falls, never below zero, not in one tick, and 16 blocks/s stop within 10 ticks. */
    public static void nihilBrakeStopsSmoothly(GameTestHelper helper) {
        double v = 0.8;
        int ticks = 0;
        while (v > 0 && ticks < 50) {
            double next = EndRailPhysics.braked(v, EndRailPhysics.brake());
            helper.assertTrue(next < v && next >= 0, "Nihil brake did not slow down at " + v);
            helper.assertTrue(ticks > 0 || next > 0, "Nihil brake stopped a full-speed cart in one tick (not smooth)");
            v = next;
            ticks++;
        }
        helper.assertTrue(v == 0 && ticks <= 10, "Nihil brake needed " + ticks + " ticks");
        helper.assertTrue(EndRailPhysics.braked(0.02, EndRailPhysics.MIN_BRAKE) == 0, "slow cart not held");
        helper.succeed();
    }

    /** Hand-edited or commanded values are clamped at the runtime access and by validate(). */
    public static void endRailConfigIsClamped(GameTestHelper helper) {
        int speed = OresConfig.astralRailMaxSpeed;
        double boost = OresConfig.astralRailBoost, brake = OresConfig.nihilRailBrake;
        try {
            OresConfig.astralRailMaxSpeed = 999;
            OresConfig.astralRailBoost = 50;
            OresConfig.nihilRailBrake = 50;
            helper.assertTrue(EndRailPhysics.maxSpeedPerTick() == 1.0, "top speed cap bypassed");
            helper.assertTrue(EndRailPhysics.boost() == EndRailPhysics.MAX_BOOST, "boost cap bypassed");
            helper.assertTrue(EndRailPhysics.brake() == EndRailPhysics.MAX_BRAKE, "brake cap bypassed");
            OresConfig.astralRailMaxSpeed = -1;
            OresConfig.astralRailBoost = -1;
            OresConfig.nihilRailBrake = Double.NaN;
            helper.assertTrue(EndRailPhysics.maxSpeedPerTick() == 0.4, "top speed floor bypassed");
            helper.assertTrue(EndRailPhysics.boost() == EndRailPhysics.MIN_BOOST, "boost floor bypassed");
            helper.assertTrue(EndRailPhysics.brake() == 0.08, "NaN brake not replaced");
        } finally {
            OresConfig.astralRailMaxSpeed = speed;
            OresConfig.astralRailBoost = boost;
            OresConfig.nihilRailBrake = brake;
        }
        helper.succeed();
    }

    /** N51: every line powers an End rail - its own line, the other line and a vanilla redstone block. */
    public static void endRailIsFedByEveryLine(GameTestHelper helper) {
        for (int x = 0; x <= 7; x++) for (int z = 0; z <= 6; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        BlockPos astral = new BlockPos(1, 1, 1), nihil = new BlockPos(4, 1, 1), vanilla = new BlockPos(1, 1, 4), viaPowder = new BlockPos(5, 1, 4);
        helper.setBlock(astral, eastWest(LineBlocks.ASTRAL_RAIL));
        helper.setBlock(astral.north(), LineBlocks.ASTRAL_REDSTONE);
        helper.setBlock(astral.north().west(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(nihil, eastWest(LineBlocks.NIHIL_RAIL));
        helper.setBlock(nihil.north(), LineBlocks.ASTRAL_REDSTONE);
        helper.setBlock(nihil.north().east(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(vanilla, eastWest(LineBlocks.ASTRAL_RAIL));
        helper.setBlock(vanilla.north(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(viaPowder, eastWest(LineBlocks.NIHIL_RAIL));
        helper.setBlock(new BlockPos(5, 1, 5), LineBlocks.NIHIL_REDSTONE);
        helper.setBlock(new BlockPos(6, 1, 5), Blocks.REDSTONE_BLOCK);
        // One sequence: scheduling new delays from inside a delayed callback breaks the test ticker.
        helper.startSequence().thenIdle(20).thenExecute(() -> {
            helper.assertTrue(powered(helper, astral), "astral redstone did not power the astral rail");
            helper.assertTrue(powered(helper, nihil), "astral redstone did not power a nihil rail");
            helper.assertTrue(powered(helper, vanilla), "a redstone block did not power an astral rail");
            helper.assertTrue(powered(helper, viaPowder), "nihil powder did not power the nihil rail");
            helper.setBlock(astral.north().west(), Blocks.AIR);
        }).thenIdle(6).thenExecute(() ->
                helper.assertFalse(powered(helper, astral), "astral rail stayed powered without its source")).thenSucceed();
    }

    /**
     * In the real game (default minecart behaviour): a cart standing on powered Astral rails next to a stone is pushed
     * off and moves faster than vanilla's 0.4 blocks per tick within seven rails - the raised top speed and the boost
     * reach the cart through both mixins - without ever exceeding the configured top speed.
     */
    public static void astralRailLaunchesTheCartPastVanillaSpeed(GameTestHelper helper) {
        for (int x = 0; x <= 7; x++) for (int z = 1; z <= 2; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        helper.setBlock(new BlockPos(0, 1, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(0, 1, 2), Blocks.REDSTONE_BLOCK);
        for (int x = 1; x <= 7; x++) {
            helper.setBlock(new BlockPos(x, 1, 1), eastWest(LineBlocks.ASTRAL_RAIL));
            helper.setBlock(new BlockPos(x, 1, 2), LineBlocks.ASTRAL_REDSTONE);
        }
        double cap = EndRailPhysics.maxSpeedPerTick();
        net.minecraft.world.entity.vehicle.minecart.AbstractMinecart[] cart = {null};
        double[] startX = {0}, last = {0}, fastest = {0};
        helper.onEachTick(() -> {
            if (cart[0] == null || cart[0].isRemoved()) return;
            fastest[0] = Math.max(fastest[0], Math.abs(cart[0].getX() - last[0]));
            last[0] = cart[0].getX();
        });
        helper.startSequence().thenIdle(24).thenExecute(() -> {
            for (int x = 1; x <= 7; x++) helper.assertTrue(powered(helper, new BlockPos(x, 1, 1)), "rail " + x + " not powered");
            cart[0] = helper.spawn(net.minecraft.world.entity.EntityTypes.MINECART, new BlockPos(1, 1, 1));
            startX[0] = last[0] = cart[0].getX();
        }).thenWaitUntil(() -> {
            helper.assertTrue(fastest[0] <= cap + 1e-6, "cart moved " + fastest[0] + " in a tick, above the top speed " + cap);
            helper.assertTrue(cart[0].getX() - startX[0] > 4 && fastest[0] > EndRailPhysics.VANILLA_MAX_SPEED + 0.05,
                    "cart not past vanilla speed yet: fastest step " + fastest[0] + ", travelled " + (cart[0].getX() - startX[0]));
        }).thenExecute(() -> cart[0].discard()).thenSucceed();
    }

    /**
     * Powered Nihil rails stop a fast cart and hold it; unpowered ones let it roll through. A cart sent at the Astral
     * top speed onto a plain curve stays on the rails (the raised limit ends with the Astral rail).
     */
    public static void nihilRailStopsAndFastCartTakesTheCurve(GameTestHelper helper) {
        for (int x = 0; x <= 7; x++) for (int z = 0; z <= 7; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        java.util.Map<BlockPos, net.minecraft.world.level.block.state.BlockState> rails = new java.util.LinkedHashMap<>();
        // Lane z=1: powered Nihil rails x=1..6 (powder north of them, redstone block at x=0). Lane z=3: unpowered Nihil rails.
        helper.setBlock(new BlockPos(0, 1, 0), Blocks.REDSTONE_BLOCK);
        for (int x = 1; x <= 6; x++) {
            rails.put(new BlockPos(x, 1, 1), eastWest(LineBlocks.NIHIL_RAIL));
            helper.setBlock(new BlockPos(x, 1, 0), LineBlocks.NIHIL_REDSTONE);
            rails.put(new BlockPos(x, 1, 3), eastWest(LineBlocks.NIHIL_RAIL));
        }
        // Lane z=5: Astral rails x=1..4, a plain curve at x=5 turning south onto powered Nihil rails z=6..7 (the stop).
        for (int x = 1; x <= 4; x++) rails.put(new BlockPos(x, 1, 5), eastWest(LineBlocks.ASTRAL_RAIL));
        rails.put(new BlockPos(5, 1, 5), Blocks.RAIL.defaultBlockState().setValue(net.minecraft.world.level.block.RailBlock.SHAPE, RailShape.SOUTH_WEST));
        for (int z = 6; z <= 7; z++) {
            rails.put(new BlockPos(5, 1, z), LineBlocks.NIHIL_RAIL.defaultBlockState().setValue(EndRailBlock.SHAPE, RailShape.NORTH_SOUTH));
            helper.setBlock(new BlockPos(6, 1, z), LineBlocks.NIHIL_REDSTONE);
        }
        helper.setBlock(new BlockPos(7, 1, 6), Blocks.REDSTONE_BLOCK);
        // Placing a rail lets it reconnect to its neighbours; setting the same block again keeps the given shape.
        for (int pass = 0; pass < 2; pass++) rails.forEach(helper::setBlock);
        double cap = EndRailPhysics.maxSpeedPerTick();
        net.minecraft.world.entity.vehicle.minecart.AbstractMinecart[] carts = new net.minecraft.world.entity.vehicle.minecart.AbstractMinecart[3];
        double[] starts = new double[2];
        helper.startSequence().thenIdle(20).thenExecute(() -> {
            helper.assertTrue(powered(helper, new BlockPos(6, 1, 1)) && !powered(helper, new BlockPos(1, 1, 3))
                    && powered(helper, new BlockPos(5, 1, 7)), "nihil lanes not set up");
            var stopped = carts[0] = helper.spawn(net.minecraft.world.entity.EntityTypes.MINECART, new BlockPos(1, 1, 1));
            var rolling = carts[1] = helper.spawn(net.minecraft.world.entity.EntityTypes.MINECART, new BlockPos(1, 1, 3));
            var curving = carts[2] = helper.spawn(net.minecraft.world.entity.EntityTypes.MINECART, new BlockPos(1, 1, 5));
            stopped.setDeltaMovement(cap, 0, 0);
            rolling.setDeltaMovement(0.3, 0, 0);
            curving.setDeltaMovement(cap, 0, 0);
            starts[0] = stopped.getX();
            starts[1] = rolling.getX();
        }).thenIdle(30).thenExecute(() -> {
                var stopped = carts[0];
                var rolling = carts[1];
                var curving = carts[2];
                double stoppedX = starts[0], rollingX = starts[1];
                helper.assertTrue(stopped.getDeltaMovement().horizontalDistance() == 0, "powered Nihil rail did not stop the cart: " + stopped.getDeltaMovement());
                helper.assertTrue(stopped.getX() - stoppedX < 4, "cart braked too late: " + (stopped.getX() - stoppedX));
                helper.assertTrue(rolling.getX() - rollingX > 2, "unpowered Nihil rail braked the cart");
                BlockPos curve = helper.absolutePos(new BlockPos(5, 1, 5));
                helper.assertTrue(net.minecraft.world.level.block.BaseRailBlock.isRail(helper.getLevel().getBlockState(curving.getCurrentBlockPosOrRailBelow()))
                        && curving.getZ() > curve.getZ() + 1 && Math.abs(curving.getX() - (curve.getX() + 0.5)) < 0.1
                        && curving.getDeltaMovement().horizontalDistance() == 0,
                        "fast cart did not follow the curve onto the stop: at " + curving.position() + " moving " + curving.getDeltaMovement());
                stopped.discard();
                rolling.discard();
                curving.discard();
        }).thenSucceed();
    }
}
