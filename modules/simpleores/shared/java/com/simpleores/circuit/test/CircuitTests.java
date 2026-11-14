package com.simpleores.circuit.test;

import com.simpleores.OresRegistry;
import com.simpleores.circuit.AstralRelayBlock;
import com.simpleores.circuit.CircuitBlocks;
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
        h.setBlock(dust.west(), Blocks.REDSTONE_BLOCK);
        h.setBlock(wall.south(), Blocks.REDSTONE_LAMP);
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
