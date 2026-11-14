package com.simpleores.circuit.test;

import com.simpleores.OresRegistry;
import com.simpleores.circuit.AstralRelayBlock;
import com.simpleores.circuit.CircuitBlocks;
import com.simpleores.circuit.WallDiodeBlock;
import com.simpleores.circuit.NihilCoreBlock;
import com.simpleores.circuit.WallSignalDustBlock;
import com.simpleores.line.LineBlocks;
import com.simpleores.line.LineIds;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * N46 (b-circ46): Astral Relay (T flip-flop, signal 15 to all sides), Nihil Core (touching cores switch together, connected
 * texture states), Astral/Bridge Redstone on walls and ceilings (item placement, signal into the supporting block), and the
 * migration of SimpleBuilding's line ids (N47-M2). All on the module arena (stone floor at y = 0).
 */
public final class CircuitTests {
    private CircuitTests() {}

    public static void register(Map<String, Consumer<GameTestHelper>> all, Map<String, Integer> ticks) {
        all.put("circuit_relay_toggles_and_sends", CircuitTests::relay);
        ticks.put("circuit_relay_toggles_and_sends", 100);
        all.put("circuit_core_group_switches_together", CircuitTests::coreGroup);
        ticks.put("circuit_core_group_switches_together", 100);
        all.put("circuit_wall_dust_sticks_and_powers", CircuitTests::wallDust);
        ticks.put("circuit_wall_dust_sticks_and_powers", 100);
        all.put("circuit_wall_diodes_place_and_switch", CircuitTests::wallDiodes);
        ticks.put("circuit_wall_diodes_place_and_switch", 100);
        all.put("circuit_wall_dust_rounds_the_edge", CircuitTests::wallCorner);
        ticks.put("circuit_wall_dust_rounds_the_edge", 60);
        all.put("circuit_config_takes_over_simplebuilding", CircuitTests::configTakeover);
        ticks.put("circuit_config_takes_over_simplebuilding", 20);
        all.put("circuit_line_ids_migrate", CircuitTests::migration);
        ticks.put("circuit_line_ids_migrate", 40);
    }

    private static boolean lit(GameTestHelper h, BlockPos pos) {
        BlockState s = h.getBlockState(pos);
        return s.hasProperty(BlockStateProperties.LIT) && s.getValue(BlockStateProperties.LIT);
    }

    /** Copper bulb rule with a real output: a lamp beside it follows, a comparator would read 15. */
    private static void relay(GameTestHelper h) {
        BlockPos relay = new BlockPos(3, 1, 3), source = relay.west(), lamp = relay.east();
        h.setBlock(relay, CircuitBlocks.ASTRAL_RELAY);
        h.setBlock(lamp, Blocks.REDSTONE_LAMP);
        h.setBlock(source, Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(4, () -> {
            h.assertTrue(lit(h, relay), "first pulse turns the relay on");
            h.assertTrue(lit(h, lamp), "a lit relay powers the lamp next to it");
            BlockState state = h.getBlockState(relay);
            h.assertTrue(state.getAnalogOutputSignal(h.getLevel(), h.absolutePos(relay), Direction.NORTH) == 15, "comparator reads 15");
            h.setBlock(source, Blocks.AIR);
            h.runAfterDelay(6, () -> {
                h.assertTrue(lit(h, relay), "the relay stays on without input (T flip-flop)");
                h.setBlock(source, Blocks.REDSTONE_BLOCK);
                h.runAfterDelay(6, () -> {
                    h.assertFalse(lit(h, relay), "second pulse turns the relay off");
                    h.assertFalse(lit(h, lamp), "an unlit relay sends nothing");
                    h.succeed();
                });
            });
        });
    }

    /** Three touching cores switch as one, a fourth one apart does not; connection states for the texture. */
    private static void coreGroup(GameTestHelper h) {
        BlockPos a = new BlockPos(2, 1, 2), b = a.east(), c = b.east(), apart = new BlockPos(2, 1, 6);
        for (BlockPos p : new BlockPos[] {a, b, c, apart}) h.setBlock(p, CircuitBlocks.NIHIL_CORE);
        BlockState middle = h.getBlockState(b);
        h.assertTrue(middle.getValue(NihilCoreBlock.EAST) && middle.getValue(NihilCoreBlock.WEST) && !middle.getValue(NihilCoreBlock.UP),
                "the middle core knows its two neighbours: " + middle);
        BlockPos lamp = c.east();
        h.setBlock(lamp, Blocks.REDSTONE_LAMP);
        h.setBlock(a.west(), Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(4, () -> {
            h.assertTrue(lit(h, a) && lit(h, b) && lit(h, c), "a pulse at one core lights the whole group");
            h.assertFalse(lit(h, apart), "a core that does not touch the group stays off");
            h.assertTrue(lit(h, lamp), "a lit core gives a redstone signal");
            h.setBlock(a.west(), Blocks.AIR);
            h.runAfterDelay(6, () -> {
                h.assertTrue(lit(h, c), "the group stays on without input");
                h.setBlock(a.west(), Blocks.REDSTONE_BLOCK);
                h.runAfterDelay(6, () -> {
                    h.assertFalse(lit(h, a) || lit(h, b) || lit(h, c), "the next pulse turns the group off");
                    h.assertFalse(lit(h, lamp), "an unlit core gives no signal");
                    h.succeed();
                });
            });
        });
    }

    /** The dust item sticks Astral Redstone to a wall; powered, it powers its wall block strongly (a lamp behind lights). */
    private static void wallDust(GameTestHelper h) {
        BlockPos wall = new BlockPos(3, 1, 3);
        h.setBlock(wall, Blocks.STONE);
        Player player = h.makeMockPlayer(GameType.CREATIVE);
        ItemStack stack = new ItemStack(OresRegistry.item("astral_redstone"));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos absWall = h.absolutePos(wall);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absWall).relative(Direction.NORTH, 0.5), Direction.NORTH, absWall, false);
        ((BlockItem) stack.getItem()).place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit));
        BlockPos dust = wall.north();
        BlockState placed = h.getBlockState(dust);
        h.assertTrue(placed.is(CircuitBlocks.ASTRAL_WALL_REDSTONE) && placed.getValue(WallSignalDustBlock.FACING) == Direction.NORTH,
                "the item sticks the dust to the north face: " + placed);
        // Ceiling: the underside of a block placed one up.
        BlockPos roof = new BlockPos(5, 3, 3);
        h.setBlock(roof, Blocks.STONE);
        BlockPos absRoof = h.absolutePos(roof);
        ItemStack bridge = new ItemStack(OresRegistry.item("bridge_redstone"));
        ((BlockItem) bridge.getItem()).place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, bridge,
                new BlockHitResult(Vec3.atCenterOf(absRoof).relative(Direction.DOWN, 0.5), Direction.DOWN, absRoof, false)));
        BlockState ceiling = h.getBlockState(roof.below());
        h.assertTrue(ceiling.is(CircuitBlocks.BRIDGE_WALL_REDSTONE) && ceiling.getValue(WallSignalDustBlock.FACING) == Direction.DOWN,
                "bridge redstone hangs under the ceiling: " + ceiling);
        // Power: a redstone block beside the wall dust, a lamp behind the wall block (powered through it).
        // (The lamp first: a lamp set into an already powered spot waits for the next neighbour update.)
        h.setBlock(wall.south(), Blocks.REDSTONE_LAMP);
        h.setBlock(dust.west(), Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(4, () -> {
            h.assertTrue(h.getBlockState(dust).getValue(WallSignalDustBlock.POWER) == 15, "wall dust takes the signal");
            h.assertTrue(lit(h, wall.south()), "wall dust powers its wall block like floor dust powers the block below");
            h.setBlock(wall, Blocks.AIR);
            h.runAfterDelay(2, () -> {
                h.assertTrue(h.getBlockState(dust).isAir(), "without the wall the dust drops");
                h.succeed();
            });
        });
    }

    /**
     * The Vanilla Repeater item sticks to a wall where it cannot stand (output away from the player, here up) and repeats;
     * a ceiling Signal Dampener takes its stage off; a wall Comparator compares and subtracts its side signal.
     */
    private static void wallDiodes(GameTestHelper h) {
        BlockPos support = new BlockPos(3, 3, 3), cell = support.north();
        h.setBlock(support, Blocks.STONE);
        h.setBlock(cell.above(), Blocks.REDSTONE_LAMP);
        Player player = h.makeMockPlayer(GameType.CREATIVE);
        player.setXRot(-80.0F); // looking up: the output points up
        ItemStack stack = new ItemStack(Items.REPEATER);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = h.absolutePos(support);
        ((BlockItem) Items.REPEATER).place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(abs).relative(Direction.NORTH, 0.5), Direction.NORTH, abs, false)));
        BlockState repeater = h.getBlockState(cell);
        h.assertTrue(repeater.is(CircuitBlocks.WALL_REPEATER) && repeater.getValue(WallDiodeBlock.FACING) == Direction.NORTH
                && WallDiodeBlock.output(repeater) == Direction.UP, "the repeater item sticks to the wall, output up: " + repeater);
        h.setBlock(cell.below(), Blocks.REDSTONE_BLOCK);
        // Ceiling dampener: support above, output north, input from the south.
        BlockPos roof = new BlockPos(6, 4, 3), damp = roof.below();
        h.setBlock(roof, Blocks.STONE);
        h.setBlock(damp, CircuitBlocks.WALL_SIGNAL_DAMPENER.defaultBlockState().setValue(WallDiodeBlock.FACING, Direction.DOWN).setValue(WallDiodeBlock.OUT, 0));
        h.setBlock(damp.south(), Blocks.REDSTONE_BLOCK);
        // Wall comparator: output east, input from the west, sides up and down.
        BlockPos wall = new BlockPos(9, 3, 3), comp = wall.north();
        h.setBlock(wall, Blocks.STONE);
        h.setBlock(comp, CircuitBlocks.WALL_COMPARATOR.defaultBlockState().setValue(WallDiodeBlock.FACING, Direction.NORTH).setValue(WallDiodeBlock.OUT, 1));
        h.setBlock(comp.west(), Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(6, () -> {
            h.assertTrue(h.getBlockState(cell).getValue(WallDiodeBlock.POWERED), "the wall repeater switches on");
            h.assertTrue(lit(h, cell.above()), "the wall repeater powers the lamp in front of it");
            h.assertTrue(h.getBlockState(damp).getValue(WallDiodeBlock.Dampener.POWER) == 14, "the ceiling dampener takes 1 off: "
                    + h.getBlockState(damp));
            h.assertTrue(h.getBlockState(comp).getValue(WallDiodeBlock.Comparator.POWER) == 15, "the wall comparator passes 15");
            h.setBlock(comp.above(), Blocks.REDSTONE_BLOCK);
            h.setBlock(comp, h.getBlockState(comp).setValue(WallDiodeBlock.Comparator.SUBTRACT, true));
            h.runAfterDelay(6, () -> {
                h.assertTrue(h.getBlockState(comp).getValue(WallDiodeBlock.Comparator.POWER) == 0, "subtract: 15 - 15 = 0");
                h.setBlock(support, Blocks.AIR);
                h.runAfterDelay(2, () -> {
                    h.assertFalse(h.getBlockState(cell).is(CircuitBlocks.WALL_REPEATER), "without its wall the repeater drops");
                    h.succeed();
                });
            });
        });
    }

    /** Wall dust goes round the outer edge of its block: to wall dust on the next face and to floor dust on top. */
    private static void wallCorner(GameTestHelper h) {
        BlockPos block = new BlockPos(3, 2, 4), north = block.north(), east = block.east(), top = block.above();
        h.setBlock(block, Blocks.STONE);
        h.setBlock(north, CircuitBlocks.ASTRAL_WALL_REDSTONE.defaultBlockState().setValue(WallSignalDustBlock.FACING, Direction.NORTH));
        h.setBlock(east, CircuitBlocks.ASTRAL_WALL_REDSTONE.defaultBlockState().setValue(WallSignalDustBlock.FACING, Direction.EAST));
        h.setBlock(top, LineBlocks.ASTRAL_REDSTONE);
        h.setBlock(north.west(), Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(4, () -> {
            h.assertTrue(h.getBlockState(north).getValue(WallSignalDustBlock.POWER) == 15, "the powered wall dust: 15");
            h.assertTrue(h.getBlockState(east).getValue(WallSignalDustBlock.POWER) == 14, "round the edge onto the east face: 14, got "
                    + h.getBlockState(east).getValue(WallSignalDustBlock.POWER));
            h.assertTrue(h.getBlockState(top).getValue(BlockStateProperties.POWER) == 14, "over the top edge onto floor dust: 14, got "
                    + h.getBlockState(top).getValue(BlockStateProperties.POWER));
            h.succeed();
        });
    }

    /** First start without simpleores-server.json: the line options of an old simplebuilding.json are taken over once. */
    private static void configTakeover(GameTestHelper h) {
        com.google.gson.JsonObject before = com.simpleores.config.OresConfig.toJson();
        try {
            java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("simpleores-sb-config");
            java.nio.file.Files.writeString(dir.resolve("simplebuilding.json"),
                    "{\"server\":{\"features\":{\"endRails\":false},\"machines\":{\"astralRailMaxSpeed\":10},\"oreGeneration\":{\"nihilitOre\":false}}}");
            com.simpleores.config.OresConfig.load(dir);
            h.assertTrue(!com.simpleores.config.OresConfig.endRails && com.simpleores.config.OresConfig.astralRailMaxSpeed == 10
                    && !com.simpleores.config.OresConfig.nihilitOre && com.simpleores.config.OresConfig.endSignals, "old values taken over, others default");
            h.assertTrue(java.nio.file.Files.isRegularFile(dir.resolve("simpleores-server.json")), "own file written as marker");
            java.nio.file.Files.writeString(dir.resolve("simpleores-server.json"), "{\"endRails\":true}");
            com.simpleores.config.OresConfig.load(dir);
            h.assertTrue(com.simpleores.config.OresConfig.endRails && com.simpleores.config.OresConfig.astralRailMaxSpeed == 16, "own file wins");
        } catch (java.io.IOException e) {
            h.fail("config io: " + e);
        } finally {
            com.simpleores.config.OresConfig.apply(before);
        }
        h.succeed();
    }

    /** N47-M2: SimpleBuilding's line ids load as Simple Ores' blocks and items (IdMoves). */
    private static void migration(GameTestHelper h) {
        for (String path : LineIds.LINE_ITEMS) {
            var item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("simplebuilding", path));
            h.assertTrue(item == OresRegistry.item(path), "simplebuilding:" + path + " loads as simpleores:" + path + " (got " + item + ")");
        }
        h.assertTrue(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("simplebuilding", "astral_redstone_wall_torch")) == LineBlocks.ASTRAL_REDSTONE_WALL_TORCH,
                "the wall torch block moves too");
        h.assertTrue(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("simplebuilding", "astral_relay")) != CircuitBlocks.ASTRAL_RELAY,
                "new blocks have no old id");
        h.succeed();
    }
}
