package com.simplebuilding.clientgametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.SignalDustBlock;
import com.simplebuilding.version.McVersion;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

/**
 * Redstone lines (Nachtrag 36, docs/ai/PLAN-REDSTONE-LINIEN-2026-10-11.md) in the real client, one documentary picture:
 * a lever feeding Astral and Nihil Redstone into a redstone lamp, redstone dust going over Bridge Redstone into Astral
 * Redstone and a lamp, and Astral Redstone touching unpowered Nihil Redstone (dark lamp). Asserts the lamps on the
 * server; clears its blocks again.
 */
public final class RedstoneLinesClientTest {
    private static final int X0 = 0;
    private static final int[] LANES = {11, 13, 15, 17};

    private RedstoneLinesClientTest() {
    }

    public static void inWorld(Script script) {
        if (!McVersion.END_SYSTEMS) {
            return;
        }
        TestScene.build(script, "minecraft:stone", "creative");
        BlockState lever = Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR).setValue(LeverBlock.POWERED, true);
        onServer(script, "lay out the four redstone lanes", server -> {
            ServerLevel level = server.overworld();
            for (int z : LANES) for (int x = X0; x <= X0 + 14; x++) level.setBlock(new BlockPos(x, -1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            lane(level, LANES[0], lever, i -> ModBlocks.ASTRAL_REDSTONE);
            lane(level, LANES[1], lever, i -> ModBlocks.NIHIL_REDSTONE);
            lane(level, LANES[2], Blocks.REDSTONE_BLOCK.defaultBlockState(),
                    i -> i < 4 ? Blocks.REDSTONE_WIRE : i < 8 ? ModBlocks.BRIDGE_REDSTONE : ModBlocks.ASTRAL_REDSTONE);
            lane(level, LANES[3], Blocks.REDSTONE_BLOCK.defaultBlockState(), i -> i < 6 ? ModBlocks.ASTRAL_REDSTONE : ModBlocks.NIHIL_REDSTONE);
        });
        script.awaitPackets();
        script.idle("let the lamps switch", 10);
        script.command("tp @a 6.5 14.0 19.5 180.0 70.0");
        script.awaitPackets();
        script.awaitChunks();
        script.idle("let the dust settle in the picture", 20);
        script.shot("redstone-lines");
        String[] report = {null};
        onServer(script, "read the lamps on the server", server -> {
            ServerLevel level = server.overworld();
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < LANES.length; i++) {
                boolean lit = level.getBlockState(new BlockPos(X0 + 13, 0, LANES[i])).getValue(RedstoneLampBlock.LIT);
                if (lit != (i < 3)) {
                    out.append("lamp of lane ").append(i).append(" is ").append(lit ? "lit" : "dark").append(" (signal there: ")
                            .append(level.hasNeighborSignal(new BlockPos(X0 + 13, 0, LANES[i]))).append("); lane:");
                    for (int x = X0; x <= X0 + 13; x++) {
                        BlockState state = level.getBlockState(new BlockPos(x, 0, LANES[i]));
                        out.append(' ').append(state.hasProperty(SignalDustBlock.POWER) ? state.getValue(SignalDustBlock.POWER) + "/"
                                + state.getValue(SignalDustBlock.EAST) : state.toString());
                    }
                    out.append(". ");
                }
            }
            report[0] = out.toString();
        });
        script.idle("let the server answer", 5);
        script.act("lit lamps at the lever and bridge lanes; dark lamp behind the other line", client -> {
            if (report[0] == null) throw new AssertionError("the server did not report the lamps");
            if (!report[0].isEmpty()) throw new AssertionError(report[0]);
        });
        onServer(script, "clear the lanes", server -> {
            for (int z : LANES) for (int x = X0; x <= X0 + 14; x++) {
                server.overworld().setBlock(new BlockPos(x, 0, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        });
        script.command("tp @a 10.5 0.0 15.5 0.0 0.0");
        script.awaitPackets();
    }

    /** Source at X0, twelve dust pieces, a redstone lamp; then every piece gets the connections a player's placement gives. */
    private static void lane(ServerLevel level, int z, BlockState source, java.util.function.IntFunction<Block> dust) {
        level.setBlock(new BlockPos(X0, 0, z), source, Block.UPDATE_ALL);
        for (int i = 0; i < 12; i++) level.setBlock(new BlockPos(X0 + 1 + i, 0, z), dust.apply(i).defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(new BlockPos(X0 + 13, 0, z), Blocks.REDSTONE_LAMP.defaultBlockState(), Block.UPDATE_ALL);
        for (int i = 0; i < 12; i++) {
            BlockPos pos = new BlockPos(X0 + 1 + i, 0, z);
            BlockState state = level.getBlockState(pos);
            if (SignalDustBlock.lineOf(state) != null) level.setBlock(pos, Block.updateFromNeighbourShapes(state, level, pos), Block.UPDATE_ALL);
        }
    }

    private static void onServer(Script script, String name, Consumer<MinecraftServer> work) {
        script.act(name, client -> {
            MinecraftServer server = client.getSingleplayerServer();
            if (server == null) {
                throw new AssertionError("There is no integrated server");
            }
            server.execute(() -> work.accept(server));
        });
    }
}
