package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.EndSignalLampBlock;
import com.simplebuilding.blocks.custom.SignalDustBlock;
import com.simplebuilding.datafix.LegacyItemIds;
import com.simplebuilding.util.LineSounds;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.RedstoneSide;

/**
 * Redstone, Astral and Nihil as three separate redstone lines plus Bridge Redstone (owner 2026-10-11,
 * docs/ai/PLAN-REDSTONE-LINIEN-2026-10-11.md). Lanes run east from x=0; layers at y=1 (floor y=0) and y=4 (floor y=3).
 */
public final class RedstoneLinesTests {
    private RedstoneLinesTests() {
    }

    private static boolean active(GameTestHelper helper) {
        if (com.simplebuilding.version.McVersion.END_SYSTEMS) return true;
        helper.succeed();
        return false;
    }

    private static Block[] lines() {
        return new Block[]{Blocks.REDSTONE_WIRE, ModBlocks.ASTRAL_REDSTONE, ModBlocks.NIHIL_REDSTONE};
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
        if (!active(helper)) return;
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
    public static void linesStaySeparateThroughABlock(GameTestHelper helper) {
        if (!active(helper)) return;
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
        if (!active(helper)) return;
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
                for (int x = 1; x <= 3; x++) helper.setBlock(new BlockPos(x, y, z), layer == 0 ? ModBlocks.ASTRAL_REDSTONE : ModBlocks.NIHIL_REDSTONE);
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
        if (!active(helper)) return;
        floor(helper);
        for (int lane = 0; lane < 2; lane++) {
            Block dust = lane == 0 ? ModBlocks.ASTRAL_REDSTONE : ModBlocks.NIHIL_REDSTONE;
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
        if (!active(helper)) return;
        floor(helper);
        Block bridge = ModBlocks.BRIDGE_REDSTONE;
        Block[][] lanes = {
                {Blocks.REDSTONE_WIRE, Blocks.REDSTONE_WIRE, bridge, ModBlocks.ASTRAL_REDSTONE, ModBlocks.ASTRAL_REDSTONE},
                {ModBlocks.ASTRAL_REDSTONE, bridge, ModBlocks.NIHIL_REDSTONE, ModBlocks.NIHIL_REDSTONE},
                {ModBlocks.NIHIL_REDSTONE, bridge, Blocks.REDSTONE_WIRE, Blocks.REDSTONE_WIRE},
                {ModBlocks.ASTRAL_REDSTONE, bridge, bridge, Blocks.REDSTONE_WIRE}};
        for (int i = 0; i < lanes.length; i++) {
            helper.setBlock(new BlockPos(0, 1, i * 2), Blocks.REDSTONE_BLOCK);
            for (int x = 0; x < lanes[i].length; x++) helper.setBlock(new BlockPos(x + 1, 1, i * 2), lanes[i][x]);
        }
        // Looks (y=4): vanilla dust beside a bridge and beside Astral Redstone.
        helper.setBlock(new BlockPos(1, 4, 0), bridge);
        helper.setBlock(new BlockPos(1, 4, 1), Blocks.REDSTONE_WIRE);
        helper.setBlock(new BlockPos(4, 4, 0), ModBlocks.ASTRAL_REDSTONE);
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
     * The removed Astral/Nihil switches load as levers; the lamps are back (Nachtrag 44) under their old ids, so old
     * stacks and placed lamps load as the new Astral/Nihilith Lamp - a saved pre-Nachtrag-36 state keeps its power.
     */
    public static void oldSwitchesBecomeLeversLampsReturn(GameTestHelper helper) {
        if (!active(helper)) return;
        var items = BuiltInRegistries.ITEM;
        var blocks = BuiltInRegistries.BLOCK;
        for (String sw : List.of("astralit_switch", "nihilith_switch")) {
            Identifier old = Identifier.fromNamespaceAndPath("simplebuilding", sw);
            helper.assertTrue(blocks.getValue(old) == Blocks.LEVER && blocks.get(old).orElseThrow().value() == Blocks.LEVER,
                    "old block " + old + " does not load as a lever");
            helper.assertTrue(items.getValue(old) == Items.LEVER && items.containsKey(old), "old item " + old + " does not load as a lever");
            helper.assertTrue(LegacyItemIds.renamedTo(old) != null, "no item alias for " + old);
        }
        for (var entry : java.util.Map.of("astralit_lamp", ModBlocks.ASTRALIT_LAMP, "nihilith_lamp", ModBlocks.NIHILITH_LAMP).entrySet()) {
            Identifier id = Identifier.fromNamespaceAndPath("simplebuilding", entry.getKey());
            helper.assertTrue(blocks.getValue(id) == entry.getValue() && BuiltInRegistries.BLOCK.getKey(entry.getValue()).equals(id),
                    id + " is not the lamp block again: " + blocks.getValue(id));
            helper.assertTrue(items.getValue(id) == entry.getValue().asItem() && entry.getValue().asItem() != Items.AIR,
                    id + " is not the lamp item again: " + items.getValue(id));
            helper.assertTrue(LegacyItemIds.renamedTo(id) == null, "a stale alias still moves " + id);
            helper.assertTrue(LegacyItemIds.renamedIn(BuiltInRegistries.BLOCK, id) == null, "a stale block alias still moves " + id);
        }
        // An old saved lamp state (only the property power, like before Nachtrag 36) decodes to the new lamp, unlit.
        var tag = (net.minecraft.nbt.CompoundTag) BlockState.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,
                ModBlocks.ASTRALIT_LAMP.defaultBlockState().setValue(EndSignalLampBlock.POWER, 7)).getOrThrow();
        for (String key : List.copyOf(tag.keySet())) {
            if (tag.get(key) instanceof net.minecraft.nbt.CompoundTag props) props.remove("lit");
        }
        BlockState decoded = BlockState.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag).result().orElse(null);
        helper.assertTrue(decoded != null && decoded.is(ModBlocks.ASTRALIT_LAMP) && decoded.getValue(EndSignalLampBlock.POWER) == 7
                && !decoded.getValue(EndSignalLampBlock.LIT), "saved old astralit_lamp state " + tag + " decoded as " + decoded);
        helper.succeed();
    }

    /**
     * The Astral/Nihilith Lamp takes every line (N51: own line, the other line, redstone dust, Bridge Redstone and a
     * lever right beside it) and toggles like a copper bulb: the first pulse lights it, it stays lit when the signal
     * goes, the next pulse switches it off.
     */
    public static void lampsToggleOnEveryLine(GameTestHelper helper) {
        if (!active(helper)) return;
        floor(helper);
        // {y, z, dust or null for a lever beside the lamp, lamp, should toggle}
        Object[][] lanes = {
                {1, 0, ModBlocks.ASTRAL_REDSTONE, ModBlocks.ASTRALIT_LAMP, true},
                {1, 2, ModBlocks.NIHIL_REDSTONE, ModBlocks.NIHILITH_LAMP, true},
                {1, 4, ModBlocks.NIHIL_REDSTONE, ModBlocks.ASTRALIT_LAMP, true},
                {1, 6, Blocks.REDSTONE_WIRE, ModBlocks.ASTRALIT_LAMP, true},
                {4, 0, ModBlocks.BRIDGE_REDSTONE, ModBlocks.NIHILITH_LAMP, true},
                {4, 3, ModBlocks.ASTRAL_REDSTONE, ModBlocks.NIHILITH_LAMP, true},
                {4, 6, null, ModBlocks.ASTRALIT_LAMP, true}};
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
            helper.setBlock(pos, ModBlocks.NIHIL_PISTON);
        }, s -> s.getValue(com.simplebuilding.blocks.custom.EndSignalBlock.POWER) > 0));
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
        list.add(Device.of("astralit_lamp", ModBlocks.ASTRALIT_LAMP.defaultBlockState(), s -> s.getValue(EndSignalLampBlock.POWER) > 0));
        list.add(Device.of("nihilith_lamp", ModBlocks.NIHILITH_LAMP.defaultBlockState(), s -> s.getValue(EndSignalLampBlock.POWER) > 0));
        list.add(new Device("astral_piston", (helper, pos) -> {
            helper.setBlock(pos.below(), Blocks.OBSIDIAN);
            helper.setBlock(pos, ModBlocks.ASTRAL_PISTON);
        }, s -> s.getValue(com.simplebuilding.blocks.custom.EndSignalBlock.POWER) > 0));
        if (com.simplebuilding.version.McVersion.END_RAILS) {
            list.add(Device.of("astral_rail", ModBlocks.ASTRAL_RAIL.defaultBlockState(), s -> s.getValue(com.simplebuilding.blocks.custom.EndRailBlock.POWERED)));
            list.add(Device.of("nihil_rail", ModBlocks.NIHIL_RAIL.defaultBlockState(), s -> s.getValue(com.simplebuilding.blocks.custom.EndRailBlock.POWERED)));
        }
        if (com.simplebuilding.version.McVersion.AUTONOMOUS_CRAFTER) {
            list.add(Device.of("autonomous_crafter", ModBlocks.AUTONOMOUS_CRAFTER.defaultBlockState(),
                    s -> s.getValue(com.simplebuilding.blocks.custom.AutonomousCrafterBlock.TRIGGERED)));
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
        if (!active(helper)) return;
        everyDeviceReacts(helper, Blocks.REDSTONE_BLOCK, Blocks.REDSTONE_WIRE, "redstone_wire");
    }

    /** N51: Astral Redstone powers every device (vanilla, mod machine, both End lamps, pistons and rails). */
    public static void everyDeviceTakesAstral(GameTestHelper helper) {
        if (!active(helper)) return;
        everyDeviceReacts(helper, Blocks.REDSTONE_BLOCK, ModBlocks.ASTRAL_REDSTONE, "astral_redstone");
    }

    /** N51: Nihil Redstone powers every device. */
    public static void everyDeviceTakesNihil(GameTestHelper helper) {
        if (!active(helper)) return;
        everyDeviceReacts(helper, Blocks.REDSTONE_BLOCK, ModBlocks.NIHIL_REDSTONE, "nihil_redstone");
    }

    /** N51: Bridge Redstone powers every device. */
    public static void everyDeviceTakesBridge(GameTestHelper helper) {
        if (!active(helper)) return;
        everyDeviceReacts(helper, Blocks.REDSTONE_BLOCK, ModBlocks.BRIDGE_REDSTONE, "bridge_redstone");
    }

    /** N51: each line redstone block powers every device right next to it, like the redstone block. */
    public static void lineBlocksPowerEveryDevice(GameTestHelper helper) {
        if (!active(helper)) return;
        // The three blocks take turns in the same lanes: place, check, remove, wait.
        Block[] blocks = {ModBlocks.ASTRAL_REDSTONE_BLOCK, ModBlocks.NIHIL_REDSTONE_BLOCK, ModBlocks.BRIDGE_REDSTONE_BLOCK};
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
        if (!active(helper)) return;
        floor(helper);
        Block[] blocks = {ModBlocks.ASTRAL_REDSTONE_BLOCK, ModBlocks.NIHIL_REDSTONE_BLOCK, ModBlocks.BRIDGE_REDSTONE_BLOCK};
        Block[] dusts = {Blocks.REDSTONE_WIRE, ModBlocks.ASTRAL_REDSTONE, ModBlocks.NIHIL_REDSTONE, ModBlocks.BRIDGE_REDSTONE};
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
                boolean feeds = ((com.simplebuilding.blocks.custom.LineRedstoneBlock) block).line().links(SignalDustBlock.lineOf(dust.defaultBlockState()));
                int first = power(helper, slots.get(i).east()), second = power(helper, slots.get(i).east(2));
                String lane = name(block) + " -> " + name(dust);
                helper.assertTrue(feeds ? first == 15 && second == 14 : first == 0 && second == 0,
                        lane + (feeds ? " not fed" : " fed") + ": " + first + "/" + second);
            }
            helper.succeed();
        });
    }

    /** N51: nine dust craft the block and the block gives the nine back, for every line. */
    public static void lineBlockRecipesRoundTrip(GameTestHelper helper) {
        if (!active(helper)) return;
        Block[][] pairs = {{ModBlocks.ASTRAL_REDSTONE, ModBlocks.ASTRAL_REDSTONE_BLOCK}, {ModBlocks.NIHIL_REDSTONE, ModBlocks.NIHIL_REDSTONE_BLOCK},
                {ModBlocks.BRIDGE_REDSTONE, ModBlocks.BRIDGE_REDSTONE_BLOCK}};
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
        if (!active(helper)) return;
        Object[][] expected = {
                {ModBlocks.ASTRAL_REDSTONE, LineSounds.ASTRAL_DUST}, {ModBlocks.NIHIL_REDSTONE, LineSounds.NIHIL_DUST},
                {ModBlocks.BRIDGE_REDSTONE, LineSounds.BRIDGE_DUST}, {ModBlocks.ASTRALIT_LAMP, LineSounds.ASTRAL_LAMP},
                {ModBlocks.NIHILITH_LAMP, LineSounds.NIHIL_LAMP}, {ModBlocks.ASTRAL_PISTON, LineSounds.ASTRAL_PISTON},
                {ModBlocks.NIHIL_PISTON, LineSounds.NIHIL_PISTON}};
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
        JsonObject sounds = json("/assets/simplebuilding/sounds.json", problems);
        JsonObject en = json("/assets/simplebuilding/lang/en_us.json", problems);
        JsonObject de = json("/assets/simplebuilding/lang/de_de.json", problems);
        for (SoundEvent event : LineSounds.EVENTS) {
            Identifier id = event.location();
            if (BuiltInRegistries.SOUND_EVENT.getValue(id) != event) problems.add(id + " is not registered");
            JsonObject spec = sounds.getAsJsonObject(id.getPath());
            if (spec == null) {
                problems.add(id + " is missing in sounds.json");
                continue;
            }
            String subtitle = spec.has("subtitle") ? spec.get("subtitle").getAsString() : "";
            if (subtitle.startsWith("subtitles.simplebuilding.")) {
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
                if (!file.getNamespace().equals(com.simplebuilding.Simplebuilding.MOD_ID)) {
                    problems.add(id + " plays a foreign file " + file);
                    continue;
                }
                String path = "/assets/simplebuilding/sounds/" + file.getPath() + ".ogg";
                try (InputStream in = RedstoneLinesTests.class.getResourceAsStream(path)) {
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
        try (InputStream in = RedstoneLinesTests.class.getResourceAsStream(resource)) {
            if (in != null) return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            problems.add(resource + " missing");
        } catch (IOException e) {
            problems.add(resource + " unreadable: " + e);
        }
        return new JsonObject();
    }
}
