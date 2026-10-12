package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.SignalDustBlock;
import com.simplebuilding.datafix.LegacyItemIds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
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

    /** The removed Astral/Nihil switches and lamps load as lever and redstone lamp: items, blocks, ids. */
    public static void removedSwitchesAndLampsBecomeVanilla(GameTestHelper helper) {
        if (!active(helper)) return;
        var items = BuiltInRegistries.ITEM;
        var blocks = BuiltInRegistries.BLOCK;
        for (var entry : java.util.Map.of("astralit_switch", Blocks.LEVER, "nihilith_switch", Blocks.LEVER,
                "astralit_lamp", Blocks.REDSTONE_LAMP, "nihilith_lamp", Blocks.REDSTONE_LAMP).entrySet()) {
            Identifier old = Identifier.fromNamespaceAndPath("simplebuilding", entry.getKey());
            helper.assertTrue(blocks.getValue(old) == entry.getValue() && blocks.get(old).orElseThrow().value() == entry.getValue(),
                    "old block " + old + " does not load as " + entry.getValue());
            helper.assertTrue(items.getValue(old) == entry.getValue().asItem() && items.containsKey(old),
                    "old item " + old + " does not load as " + entry.getValue().asItem());
            helper.assertTrue(BuiltInRegistries.BLOCK.getKey(blocks.getValue(old)).getNamespace().equals("minecraft"),
                    "alias became a simplebuilding id: " + old);
            helper.assertTrue(LegacyItemIds.renamedTo(old) != null, "no item alias for " + old);
        }
        // A saved old lamp state decodes to a redstone lamp without crashing: encode a lit lamp in this version's
        // format, then point its id at the old block.
        var tag = (net.minecraft.nbt.CompoundTag) BlockState.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,
                Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true)).getOrThrow();
        for (String key : List.copyOf(tag.keySet())) {
            if (tag.get(key) instanceof net.minecraft.nbt.StringTag && "minecraft:redstone_lamp".equals(tag.getStringOr(key, ""))) {
                tag.putString(key, "simplebuilding:astralit_lamp");
            }
        }
        BlockState decoded = BlockState.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag).result().orElse(null);
        helper.assertTrue(decoded != null && decoded.is(Blocks.REDSTONE_LAMP), "saved astralit_lamp state " + tag + " decoded as " + decoded);
        helper.succeed();
    }
}
