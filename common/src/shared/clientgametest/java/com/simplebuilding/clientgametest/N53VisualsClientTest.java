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
 * Queue N53 (26.3): cracked ice set into a frozen floor of normal ice (two blocks deep, stages 0 and 3 in the top layer,
 * one in the lower layer, stone under it) from above and from the side - it must hide in the ice without dark edges; Bridge Redstone in
 * a line, a corner, a T and a cross; the Enchanter (dark edge line). Clears its blocks again.
 */
public final class N53VisualsClientTest {
    private static final int Z = 30;

    private N53VisualsClientTest() {
    }

    public static void inWorld(Script script) {
        if (!McVersion.NATURE_VARIANTS || ModBlocks.CRACKED_ICE == null) {
            return;
        }
        TestScene.build(script, "minecraft:stone", "creative");
        onServer(script, "lay out the ice floor, the bridge wire and the enchanter", server -> {
            ServerLevel level = server.overworld();
            for (int z = Z; z <= Z + 6; z++) for (int x = 0; x <= 6; x++) {
                level.setBlock(new BlockPos(x, -2, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(new BlockPos(x, -1, z), Blocks.ICE.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(new BlockPos(x, 0, z), Blocks.ICE.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            level.setBlock(new BlockPos(2, 0, Z + 3), ModBlocks.CRACKED_ICE.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(new BlockPos(4, 0, Z + 3), ModBlocks.CRACKED_ICE.defaultBlockState().setValue(CrackedIceBlock.AGE, 3), Block.UPDATE_CLIENTS);
            level.setBlock(new BlockPos(3, 0, Z + 5), ModBlocks.CRACKED_ICE.defaultBlockState().setValue(CrackedIceBlock.AGE, 1), Block.UPDATE_CLIENTS);
            level.setBlock(new BlockPos(3, -1, Z + 3), ModBlocks.CRACKED_ICE.defaultBlockState().setValue(CrackedIceBlock.AGE, 2), Block.UPDATE_CLIENTS);
            int zb = Z + 10;
            for (int z = zb - 1; z <= zb + 4; z++) for (int x = -1; x <= 15; x++) {   // ground for the wire and the table
                level.setBlock(new BlockPos(x, -1, z), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(new BlockPos(x, 0, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            BlockPos[] bridge = {new BlockPos(0, 0, zb), new BlockPos(0, 0, zb + 1), new BlockPos(0, 0, zb + 2),
                    new BlockPos(2, 0, zb), new BlockPos(3, 0, zb), new BlockPos(2, 0, zb + 1),
                    new BlockPos(6, 0, zb), new BlockPos(5, 0, zb), new BlockPos(7, 0, zb), new BlockPos(6, 0, zb + 1),
                    new BlockPos(10, 0, zb + 1), new BlockPos(9, 0, zb + 1), new BlockPos(11, 0, zb + 1), new BlockPos(10, 0, zb + 2),
                    new BlockPos(10, 0, zb)};
            for (BlockPos pos : bridge) level.setBlock(pos, ModBlocks.BRIDGE_REDSTONE.defaultBlockState(), Block.UPDATE_ALL);
            for (BlockPos pos : bridge) {
                BlockState state = level.getBlockState(pos);
                level.setBlock(pos, Block.updateFromNeighbourShapes(state, level, pos), Block.UPDATE_ALL);
            }
            if (ModBlocks.ASTRAL_ENCHANTING_TABLE != null) {
                level.setBlock(new BlockPos(14, 0, zb + 1), ModBlocks.ASTRAL_ENCHANTING_TABLE.defaultBlockState(), Block.UPDATE_ALL);
            }
        });
        script.awaitPackets();
        script.command("gamemode spectator", true);
        script.command("time set noon", true);
        script.command("tp @a 3.5 4.5 " + (Z + 8.5) + " 180.0 55.0");
        script.awaitPackets();
        script.awaitChunks();
        script.idle("settle", 60);
        script.shot("n53-ice-top");
        script.command("tp @a 3.5 1.6 " + (Z + 9.5) + " 180.0 12.0");
        script.awaitPackets();
        script.idle("settle", 20);
        script.shot("n53-ice-side");
        script.command("tp @a 5.5 6.0 " + (Z + 11.5) + " 180.0 90.0");
        script.awaitPackets();
        script.awaitChunks();
        script.idle("settle", 20);
        script.shot("n53-bridge-wire");
        script.command("tp @a 15.9 1.4 " + (Z + 13.6) + " 150.0 25.0");
        script.awaitPackets();
        script.idle("settle", 20);
        script.shot("n53-enchanter");
        onServer(script, "clear", server -> {
            for (int z = Z; z <= Z + 14; z++) for (int x = 0; x <= 15; x++) {
                for (int y = -2; y <= 1; y++) {
                    server.overworld().setBlock(new BlockPos(x, y, z), (y < 0 ? Blocks.STONE : Blocks.AIR).defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        });
        script.command("gamemode creative", true);
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
