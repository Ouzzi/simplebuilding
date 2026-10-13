package com.simplebuilding.clientgametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.CheckerOctetBlock;
import com.simplebuilding.blocks.custom.OctetCellBlock;
import com.simplebuilding.blocks.custom.OctetCells;
import com.simplebuilding.blocks.entity.custom.MixedOctetBlockEntity;
import com.simplebuilding.chess.ChessColor;
import com.simplebuilding.util.MaterialOctets;
import com.simplebuilding.version.McVersion;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Queue N48 (docs/ai/PLAN-OCTETS-MIX-2026-10-13.md): mixed octet cells in the real client. A row of cells - an octet
 * checkerboard (lapis/quartz), stone below oak, wool colours, a partial mix and a pure cell for comparison - must reach
 * the client with their materials (block entity sync) and are drawn by the MixedOctetRenderer; two pictures. Clears the
 * row again.
 */
public final class OctetMixClientTest {
    private static final int Z = 24;
    private static final int X0 = 4;

    private OctetMixClientTest() {
    }

    private static BlockState material(Block full) {
        return OctetCells.material(MaterialOctets.cellFor(full).defaultBlockState());
    }

    private static BlockState chess(ChessColor color) {
        return OctetCells.material(ModBlocks.CHECKER_OCTET.defaultBlockState().setValue(CheckerOctetBlock.COLOR, color));
    }

    /** The cells of the row, x from {@link #X0}: each eight parts (null = empty). */
    private static List<BlockState[]> row() {
        BlockState[] checker = new BlockState[8];
        BlockState[] layered = new BlockState[8];
        BlockState[] wool = new BlockState[8];
        BlockState[] partial = new BlockState[8];
        Block[] wools = {Blocks.RED_WOOL, Blocks.ORANGE_WOOL, Blocks.YELLOW_WOOL, Blocks.LIME_WOOL, Blocks.CYAN_WOOL,
                Blocks.BLUE_WOOL, Blocks.PURPLE_WOOL, Blocks.WHITE_WOOL};
        for (int i = 0; i < 8; i++) {
            int parity = (OctetCellBlock.x(i) + OctetCellBlock.y(i) + OctetCellBlock.z(i)) & 1;
            checker[i] = chess(parity == 0 ? ChessColor.LAPIS : ChessColor.QUARTZ);
            layered[i] = OctetCellBlock.y(i) == 0 ? material(Blocks.STONE) : material(Blocks.OAK_PLANKS);
            wool[i] = material(wools[i]);
        }
        partial[OctetCellBlock.index(0, 0, 0)] = material(Blocks.STONE_BRICKS);
        partial[OctetCellBlock.index(1, 0, 0)] = material(Blocks.STONE_BRICKS);
        partial[OctetCellBlock.index(1, 0, 1)] = material(Blocks.GOLD_BLOCK);
        partial[OctetCellBlock.index(1, 1, 1)] = chess(ChessColor.ASTRALIT);
        partial[OctetCellBlock.index(0, 1, 0)] = material(Blocks.OAK_PLANKS);
        BlockState[] pure = new BlockState[8];
        for (int i = 0; i < 8; i++) {
            pure[i] = OctetCellBlock.y(i) == 0 || OctetCellBlock.x(i) == 0 ? material(Blocks.OAK_PLANKS) : null;
        }
        return List.of(checker, layered, wool, partial, pure);
    }

    public static void inWorld(Script script) {
        if (!McVersion.CHESS || ModBlocks.MIXED_OCTET == null) {
            return;
        }
        TestScene.build(script, "minecraft:stone", "creative");
        List<BlockState[]> row = row();
        onServer(script, "set up a row of mixed octet cells", server -> {
            ServerLevel level = server.overworld();
            for (int i = 0; i < row.size(); i++) {
                OctetCells.set(level, new BlockPos(X0 + i * 2, 0, Z), row.get(i), false, Block.UPDATE_ALL);
            }
        });
        script.awaitPackets();
        script.await("the client has every cell with its materials", 200, client -> {
            for (int i = 0; i < row.size(); i++) {
                if (!synced(client.level, new BlockPos(X0 + i * 2, 0, Z), row.get(i))) {
                    return false;
                }
            }
            return true;
        }, client -> "the client misses materials of a mixed octet cell");
        script.command("tp @a 8.5 2.0 21.0 0.0 30.0");
        script.awaitPackets();
        script.awaitChunks();
        script.idle("let the cells settle in the picture", 20);
        script.shot("octets-mix-row");
        script.command("tp @a 15.5 1.6 27.5 135.0 20.0");
        script.awaitPackets();
        script.awaitChunks();
        script.idle("let the cells settle in the picture", 20);
        script.shot("octets-mix-back");
        onServer(script, "clear the row", server -> {
            for (int i = 0; i < row.size(); i++) {
                server.overworld().setBlock(new BlockPos(X0 + i * 2, 0, Z), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        });
        script.command("tp @a 10.5 0.0 15.5 0.0 0.0");
        script.awaitPackets();
    }

    /** The client cell at {@code pos} holds exactly these parts (a pure cell its single material, a mixed one its BE). */
    private static boolean synced(net.minecraft.client.multiplayer.ClientLevel level, BlockPos pos, BlockState[] expected) {
        if (level == null) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (OctetCells.single(expected) == null && !(level.getBlockEntity(pos) instanceof MixedOctetBlockEntity)) {
            return false;
        }
        BlockState[] parts = OctetCells.parts(level, pos, state);
        for (int i = 0; i < 8; i++) {
            if (parts[i] != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private static void onServer(Script script, String name, java.util.function.Consumer<MinecraftServer> work) {
        script.act(name, client -> {
            MinecraftServer server = client.getSingleplayerServer();
            if (server == null) {
                throw new AssertionError("There is no integrated server");
            }
            server.execute(() -> work.accept(server));
        });
    }
}
