package com.simplebuilding.clientgametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.CrackedIceBlock;
import com.simplebuilding.version.McVersion;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Queue N52 (26.3): pictures of cracked ice next to vanilla ice, frosted ice and glass (row order: ice and frosted ice 0-3,
 * cracked ice 0-3, glass), and of Bridge Redstone in a corner, a T and a cross (no z-fighting). Clears its blocks again.
 */
public final class N52VisualsClientTest {
    private static final int Z_ICE = 11;
    private static final int Z_BRIDGE = 20;

    private N52VisualsClientTest() {
    }

    public static void inWorld(Script script) {
        if (!McVersion.NATURE_VARIANTS || ModBlocks.CRACKED_ICE == null) {
            return;
        }
        TestScene.build(script, "minecraft:stone", "creative");
        onServer(script, "lay out ice, cracked ice, glass and the bridge wire", server -> {
            ServerLevel level = server.overworld();
            for (int z = Z_ICE - 1; z <= Z_BRIDGE + 4; z++) for (int x = -1; x <= 10; x++) {
                level.setBlock(new BlockPos(x, -1, z), Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
            }
            level.setBlock(new BlockPos(0, 0, Z_ICE), Blocks.ICE.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(new BlockPos(1, 0, Z_ICE), Blocks.PACKED_ICE.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(new BlockPos(2, 0, Z_ICE), Blocks.GLASS.defaultBlockState(), Block.UPDATE_ALL);
            for (int age = 0; age < 4; age++) {
                level.setBlock(new BlockPos(age * 2, 0, Z_ICE + 2), Blocks.FROSTED_ICE.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.FrostedIceBlock.AGE, age), Block.UPDATE_ALL);
                level.setBlock(new BlockPos(age * 2, 0, Z_ICE + 4), ModBlocks.CRACKED_ICE.defaultBlockState()
                        .setValue(CrackedIceBlock.AGE, age), Block.UPDATE_ALL);
            }
            // Bridge redstone: a corner, a T and a cross, each with the connections of a placement.
            BlockPos[] bridge = {new BlockPos(1, 0, Z_BRIDGE), new BlockPos(2, 0, Z_BRIDGE), new BlockPos(1, 0, Z_BRIDGE + 1),
                    new BlockPos(5, 0, Z_BRIDGE), new BlockPos(4, 0, Z_BRIDGE), new BlockPos(6, 0, Z_BRIDGE), new BlockPos(5, 0, Z_BRIDGE + 1),
                    new BlockPos(9, 0, Z_BRIDGE), new BlockPos(8, 0, Z_BRIDGE), new BlockPos(10, 0, Z_BRIDGE), new BlockPos(9, 0, Z_BRIDGE + 1),
                    new BlockPos(9, 0, Z_BRIDGE - 1)};
            for (BlockPos pos : bridge) level.setBlock(pos, ModBlocks.BRIDGE_REDSTONE.defaultBlockState(), Block.UPDATE_ALL);
            for (BlockPos pos : bridge) {
                BlockState state = level.getBlockState(pos);
                level.setBlock(pos, Block.updateFromNeighbourShapes(state, level, pos), Block.UPDATE_ALL);
            }
        });
        script.awaitPackets();
        script.command("tp @a 3.5 5.0 " + (Z_ICE + 6.5) + " 180.0 50.0");
        script.awaitPackets();
        script.awaitChunks();
        script.idle("settle", 20);
        script.shot("n52-ice");
        script.command("tp @a 5.5 3.5 " + (Z_BRIDGE + 1.7) + " 180.0 70.0");
        script.awaitPackets();
        script.awaitChunks();
        script.idle("settle", 20);
        script.shot("n52-bridge-wire");
        script.command("tp @a 3.5 1.0 " + (Z_ICE + 6.5) + " 180.0 20.0");
        script.awaitPackets();
        script.idle("settle", 20);
        script.shot("n52-ice-side");
        onServer(script, "clear", server -> {
            for (int z = Z_ICE - 1; z <= Z_BRIDGE + 4; z++) for (int x = -1; x <= 10; x++) {
                server.overworld().setBlock(new BlockPos(x, 0, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                server.overworld().setBlock(new BlockPos(x, -1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            }
        });
        script.command("tp @a 10.5 0.0 15.5 0.0 0.0");
        script.awaitPackets();
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
