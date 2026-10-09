package com.simplebuilding.clientgametest;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.CheckerOctetBlock;
import com.simplebuilding.blocks.custom.ChessPiecesBlock;
import com.simplebuilding.blocks.entity.custom.ChessPiecesBlockEntity;
import com.simplebuilding.chess.ChessColor;
import com.simplebuilding.chess.ChessItems;
import com.simplebuilding.chess.ChessPiece;
import com.simplebuilding.client.render.ChessPiecesRenderer;
import com.simplebuilding.items.custom.ChessPieceItem;
import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Chess pieces and octets in the real client (queue N15, docs/ai/PLAN-SCHACH-2026-10-06.md): a 4 x 4 lapis checker
 * board with the starting position in 3D pieces (quartz against blackstone, two knights turned), the same on a resin
 * board with the flat pieces, and a gallery of all 13 colours with every piece (3D and flat) plus one octet block each.
 *
 * <p>Asserted, for every piece the scene holds: the real renderer's pose plus the item model's own transform put the
 * model inside its quarter of the block, standing on the block below (bottom at y 0). That is the bug the owner saw on
 * 2026-10-08: the renderer subtracted the half block the item transform subtracts as well, so every piece sat half a
 * block deep in the board and half a block off its field. A missing item model (the full purple cube) fails the same
 * check. The three pictures are documentary.
 */
public final class ChessClientTest {
    /** 3D board (x 5-8) and flat board (x 12-15), both z 11-14, white in the south. */
    private static final int BOARD_Z = 11;
    private static final int BOARD_3D_X = 5;
    private static final int BOARD_FLAT_X = 12;
    /** Gallery: one column per colour from x 4, three piece cells at z 17-19, an octet block at z 16. */
    private static final int GALLERY_X = 4;
    private static final int GALLERY_Z = 17;
    private static final int[] OCTET_MASKS = {0x01, 0x03, 0x05, 0x0F, 0x33, 0x55, 0x3F, 0x77, 0x7F, 0xFF, 0x8B, 0x17, 0xE8};
    private static final ChessPiece[] BACK = {ChessPiece.ROOK, ChessPiece.KNIGHT, ChessPiece.BISHOP, ChessPiece.QUEEN,
            ChessPiece.KING, ChessPiece.BISHOP, ChessPiece.KNIGHT, ChessPiece.ROOK};

    private ChessClientTest() {
    }

    public static void inWorld(Script script) {
        if (!McVersion.CHESS) {
            return;
        }
        TestScene.build(script, "minecraft:stone", "creative");
        List<BlockPos> cells = new java.util.concurrent.CopyOnWriteArrayList<>();
        int[] pieces = new int[1];
        onServer(script, "set up two boards and the gallery of every colour", server -> {
            ServerLevel level = server.overworld();
            pieces[0] = board(level, BOARD_3D_X, ModBlocks.LAPIS_QUARTZ_CHECKER, ChessColor.QUARTZ, ChessColor.BLACKSTONE, false, cells)
                    + board(level, BOARD_FLAT_X, ModBlocks.RESIN_QUARTZ_CHECKER, ChessColor.QUARTZ, ChessColor.NETHER_BRICK, true, cells);
            ChessColor[] colors = ChessColor.values();
            for (int i = 0; i < colors.length; i++) {
                ChessColor color = colors[i];
                int x = GALLERY_X + i;
                Block floor = color.checker() != null ? color.checker() : Blocks.QUARTZ_BLOCK;
                List<ItemStack> all = new ArrayList<>();
                for (boolean flat : new boolean[]{false, true}) {
                    for (ChessPiece piece : ChessPiece.values()) {
                        all.add(new ItemStack(ChessItems.piece(color, piece, flat)));
                    }
                }
                for (int cell = 0; cell < 3; cell++) {
                    BlockPos pos = new BlockPos(x, 0, GALLERY_Z + cell);
                    level.setBlock(pos.below(), floor.defaultBlockState(), Block.UPDATE_ALL);
                    pieces[0] += cell(level, pos, Direction.NORTH, all.subList(cell * 4, cell * 4 + 4), cells);
                }
                level.setBlock(new BlockPos(x, 0, GALLERY_Z - 1), CheckerOctetBlock.withMask(ModBlocks.CHECKER_OCTET.defaultBlockState()
                        .setValue(CheckerOctetBlock.COLOR, color), OCTET_MASKS[i % OCTET_MASKS.length]), Block.UPDATE_ALL);
            }
        });
        script.awaitPackets();
        script.await("the client has every chess cell with its pieces", 200,
                client -> cells.size() == 8 * 2 + ChessColor.values().length * 3 && countPieces(client, cells) == pieces[0],
                client -> "the client shows " + countPieces(client, cells) + " of " + pieces[0] + " pieces in " + cells.size() + " cells");
        script.act("every piece stands on its own quarter, on top of the block below", client -> {
            int checked = 0;
            for (BlockPos pos : cells) {
                checked += assertOnQuarters(client, pos);
            }
            if (checked != pieces[0]) {
                throw new AssertionError("Checked " + checked + " pieces, the scene holds " + pieces[0]);
            }
        });

        fly(script);
        look(script, "tp @a 7.0 0.8 16.6 180.0 32.0");
        script.shot("chess-board-pieces");
        look(script, "tp @a 14.0 1.5 16.0 180.0 48.0");
        script.shot("chess-board-flat");
        look(script, "tp @a 10.5 2.0 11.0 0.0 30.0");
        script.shot("chess-gallery");
        land(script);
    }

    /** Starting position on a 4 x 4 checker board: black in the north cells (facing south), white in the south. */
    private static int board(ServerLevel level, int x0, Block checker, ChessColor white, ChessColor black, boolean flat,
                             List<BlockPos> cells) {
        int count = 0;
        for (int bx = 0; bx < 4; bx++) {
            for (int bz = 0; bz < 4; bz++) {
                level.setBlock(new BlockPos(x0 + bx, -1, BOARD_Z + bz), checker.defaultBlockState(), Block.UPDATE_ALL);
            }
            ItemStack pawnW = new ItemStack(ChessItems.piece(white, ChessPiece.PAWN, flat));
            ItemStack pawnB = new ItemStack(ChessItems.piece(black, ChessPiece.PAWN, flat));
            // Places: 0 north-west, 1 north-east, 2 south-west, 3 south-east.
            count += cell(level, new BlockPos(x0 + bx, 0, BOARD_Z), Direction.SOUTH, List.of(
                    new ItemStack(ChessItems.piece(black, BACK[bx * 2], flat)),
                    new ItemStack(ChessItems.piece(black, BACK[bx * 2 + 1], flat)), pawnB, pawnB), cells);
            count += cell(level, new BlockPos(x0 + bx, 0, BOARD_Z + 3), Direction.NORTH, List.of(pawnW, pawnW,
                    new ItemStack(ChessItems.piece(white, BACK[bx * 2], flat)),
                    new ItemStack(ChessItems.piece(white, BACK[bx * 2 + 1], flat))), cells);
        }
        return count;
    }

    /** One figure cell; a knight is turned a quarter, so the check covers a turned model too. */
    private static int cell(ServerLevel level, BlockPos pos, Direction facing, List<ItemStack> stacks, List<BlockPos> cells) {
        level.setBlock(pos, ModBlocks.CHESS_PIECES.defaultBlockState().setValue(ChessPiecesBlock.FACING, facing), Block.UPDATE_ALL);
        if (!(level.getBlockEntity(pos) instanceof ChessPiecesBlockEntity be)) {
            throw new AssertionError("No chess block entity at " + pos);
        }
        for (int slot = 0; slot < stacks.size(); slot++) {
            ItemStack stack = stacks.get(slot);
            boolean knight = stack.getItem() instanceof ChessPieceItem item && item.kind().piece() == ChessPiece.KNIGHT;
            be.put(slot, stack, knight ? 1 : 0);
        }
        cells.add(pos);
        return be.count();
    }

    private static int countPieces(Minecraft client, List<BlockPos> cells) {
        int count = 0;
        for (BlockPos pos : cells) {
            if (client.level.getBlockEntity(pos) instanceof ChessPiecesBlockEntity be) {
                count += be.count();
            }
        }
        return count;
    }

    /**
     * Extracts the cell with the real renderer and walks each piece's model corners through the renderer's pose and
     * the item's own transform: they have to lie in the piece's quarter (x/z) and between y 0 and 1, with the lowest
     * corner on y 0.
     */
    private static int assertOnQuarters(Minecraft client, BlockPos pos) {
        if (!(client.level.getBlockEntity(pos) instanceof ChessPiecesBlockEntity be)) {
            throw new AssertionError("The client has no chess block entity at " + pos);
        }
        BlockEntityRenderer<ChessPiecesBlockEntity, ChessPiecesRenderer.State> renderer =
                client.getBlockEntityRenderDispatcher().getRenderer(be);
        if (renderer == null) {
            throw new AssertionError("No renderer for the chess pieces block entity");
        }
        ChessPiecesRenderer.State state = renderer.createRenderState();
        renderer.extractRenderState(be, state, 0.0F, Vec3.atCenterOf(pos).add(0.0, 2.0, 3.0), null);
        int checked = 0;
        for (int slot = 0; slot < ChessPiecesBlockEntity.SLOTS; slot++) {
            if (be.piece(slot).isEmpty()) {
                continue;
            }
            if (state.items[slot].isEmpty()) {
                throw new AssertionError("Piece " + be.piece(slot) + " at " + pos + " slot " + slot + " has no model to draw");
            }
            PoseStack pose = new PoseStack();
            ChessPiecesRenderer.placePiece(pose, slot, state.yaw[slot]);
            float[] box = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
            state.items[slot].visitExtents(corner -> {
                Vector3f p = new Vector3f(corner).mulPosition(pose.last().pose());
                box[0] = Math.min(box[0], p.x);
                box[1] = Math.min(box[1], p.y);
                box[2] = Math.min(box[2], p.z);
                box[3] = Math.max(box[3], p.x);
                box[4] = Math.max(box[4], p.y);
                box[5] = Math.max(box[5], p.z);
            });
            float qx = (slot & 1) * 0.5F;
            float qz = (slot >> 1) * 0.5F;
            float e = 0.001F;
            boolean inside = box[0] >= qx - e && box[3] <= qx + 0.5F + e && box[2] >= qz - e && box[5] <= qz + 0.5F + e
                    && Math.abs(box[1]) <= e && box[4] <= 1.0F + e && box[4] > box[1];
            if (!inside) {
                throw new AssertionError("Piece " + be.piece(slot).getItem() + " at " + pos + " slot " + slot + " (yaw " + state.yaw[slot]
                        + ") is drawn at x " + box[0] + ".." + box[3] + ", y " + box[1] + ".." + box[4] + ", z " + box[2] + ".." + box[5]
                        + " - expected inside x " + qx + ".." + (qx + 0.5F) + ", z " + qz + ".." + (qz + 0.5F) + " with the foot on y 0");
            }
            checked++;
        }
        return checked;
    }

    /** Creative flight on both sides, so the camera can hang above the boards. */
    private static void fly(Script script) {
        script.act("fly, so the camera stays where it is put", client -> {
            client.player.getAbilities().flying = true;
            client.player.onUpdateAbilities();
        });
        onServer(script, "fly on the server too", server -> server.getPlayerList().getPlayers().forEach(player -> {
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }));
        script.awaitPackets();
    }

    private static void land(Script script) {
        script.act("stop flying", client -> {
            client.player.getAbilities().flying = false;
            client.player.onUpdateAbilities();
        });
        onServer(script, "stop flying on the server", server -> {
            server.getPlayerList().getPlayers().forEach(player -> {
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            });
        });
        script.command("tp @a 10.5 0.0 16.5 0.0 0.0");
        script.awaitPackets();
    }

    private static void look(Script script, String tp) {
        script.command(tp);
        script.awaitPackets();
        script.awaitChunks();
        script.idle("let the pieces settle in the picture", 20);
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
