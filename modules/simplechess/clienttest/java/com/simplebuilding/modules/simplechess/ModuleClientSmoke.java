package com.simplebuilding.modules.simplechess;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplechess.ChessColor;
import com.simplechess.ChessPiece;
import com.simplechess.block.ChessPiecesBlock;
import com.simplechess.block.ChessPiecesBlockEntity;
import com.simplechess.client.ChessPiecesRenderer;
import com.simplechess.item.ChessPieceItem;
import com.simplechess.registry.ChessItems;
import com.simplechess.registry.ChessRegistry;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Client smoke (moved with chess from SimpleBuilding's ChessClientTest, b-chess47): a 4 x 4 lapis checker board with the
 * starting position in 3D pieces (quartz against blackstone, knights turned), the same on a resin board with the flat pieces,
 * and a gallery of every registered colour with all twelve pieces.
 *
 * <p>Asserted for every piece in the scene: the real renderer's pose plus the item model's own transform put the model inside
 * its quarter of the block, standing on the block below (bug 2026-10-08: pieces half a block deep and off their field). A missing
 * item model fails the same check. The pictures are documentary.
 */
public final class ModuleClientSmoke implements FabricClientGameTest {
    private static final ChessPiece[] BACK = {ChessPiece.ROOK, ChessPiece.KNIGHT, ChessPiece.BISHOP, ChessPiece.QUEEN,
            ChessPiece.KING, ChessPiece.BISHOP, ChessPiece.KNIGHT, ChessPiece.ROOK};

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplechess")) throw new AssertionError("Module did not boot");
        context.takeScreenshot("simplechess-title");
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            List<BlockPos> cells = new java.util.concurrent.CopyOnWriteArrayList<>();
            int[] pieces = new int[1];
            world.getServer().runOnServer(server -> {
                run(server, "gamemode creative @a");
                run(server, "time set noon");
                var player = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = (ServerLevel) player.level();
                BlockPos at = player.blockPosition().offset(-8, 0, -12);
                for (int x = -1; x < 18; x++) for (int z = -1; z < 10; z++) for (int y = -1; y < 4; y++) {
                    level.setBlock(at.offset(x, y, z), (y == -1 ? Blocks.STONE : Blocks.AIR).defaultBlockState(), Block.UPDATE_ALL);
                }
                pieces[0] = board(level, at, ChessRegistry.checker(ChessColor.LAPIS), ChessColor.QUARTZ, ChessColor.BLACKSTONE, false, cells)
                        + board(level, at.east(5), ChessRegistry.checker(ChessColor.RESIN), ChessColor.QUARTZ, ChessColor.NETHER_BRICK, true, cells);
                int column = 0;
                for (ChessColor color : ChessColor.values()) {
                    if (!color.available()) continue;
                    Block floor = color.checker() != null ? color.checker() : Blocks.QUARTZ_BLOCK;
                    List<ItemStack> all = new ArrayList<>();
                    for (boolean flat : new boolean[]{false, true}) {
                        for (ChessPiece piece : ChessPiece.values()) all.add(new ItemStack(ChessItems.piece(color, piece, flat)));
                    }
                    for (int cell = 0; cell < 3; cell++) {
                        BlockPos pos = at.offset(column, 0, 6 + cell);
                        level.setBlock(pos.below(), floor.defaultBlockState(), Block.UPDATE_ALL);
                        pieces[0] += cell(level, pos, Direction.NORTH, all.subList(cell * 4, cell * 4 + 4), cells);
                    }
                    column++;
                }
                run(server, "tp @a " + (at.getX() + 4.5) + " " + (at.getY() + 3.0) + " " + (at.getZ() + 12.0) + " 180 40");
            });
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            context.waitTicks(20);
            context.takeScreenshot("simplechess-boards");
            context.runOnClient(client -> {
                int shown = 0;
                int checked = 0;
                for (BlockPos pos : cells) {
                    if (client.level.getBlockEntity(pos) instanceof ChessPiecesBlockEntity be) shown += be.count();
                    checked += assertOnQuarters(client, pos);
                }
                if (shown != pieces[0] || checked != pieces[0]) {
                    throw new AssertionError("The client shows " + shown + " and checked " + checked + " of " + pieces[0] + " pieces");
                }
            });
        }
    }

    /** Starting position on a 4 x 4 checker board: black in the north cells (facing south), white in the south. */
    private static int board(ServerLevel level, BlockPos at, Block checker, ChessColor white, ChessColor black, boolean flat, List<BlockPos> cells) {
        int count = 0;
        for (int bx = 0; bx < 4; bx++) {
            for (int bz = 0; bz < 4; bz++) level.setBlock(at.offset(bx, -1, bz), checker.defaultBlockState(), Block.UPDATE_ALL);
            ItemStack pawnW = new ItemStack(ChessItems.piece(white, ChessPiece.PAWN, flat));
            ItemStack pawnB = new ItemStack(ChessItems.piece(black, ChessPiece.PAWN, flat));
            // Places: 0 north-west, 1 north-east, 2 south-west, 3 south-east.
            count += cell(level, at.offset(bx, 0, 0), Direction.SOUTH, List.of(new ItemStack(ChessItems.piece(black, BACK[bx * 2], flat)),
                    new ItemStack(ChessItems.piece(black, BACK[bx * 2 + 1], flat)), pawnB, pawnB), cells);
            count += cell(level, at.offset(bx, 0, 3), Direction.NORTH, List.of(pawnW, pawnW, new ItemStack(ChessItems.piece(white, BACK[bx * 2], flat)),
                    new ItemStack(ChessItems.piece(white, BACK[bx * 2 + 1], flat))), cells);
        }
        return count;
    }

    /** One piece cell; a knight is turned a quarter, so the check covers a turned model too. */
    private static int cell(ServerLevel level, BlockPos pos, Direction facing, List<ItemStack> stacks, List<BlockPos> cells) {
        level.setBlock(pos, ChessRegistry.CHESS_PIECES.defaultBlockState().setValue(ChessPiecesBlock.FACING, facing), Block.UPDATE_ALL);
        if (!(level.getBlockEntity(pos) instanceof ChessPiecesBlockEntity be)) throw new AssertionError("No chess block entity at " + pos);
        for (int slot = 0; slot < stacks.size(); slot++) {
            ItemStack stack = stacks.get(slot);
            Item item = stack.getItem();
            boolean knight = item instanceof ChessPieceItem piece && piece.kind().piece() == ChessPiece.KNIGHT;
            be.put(slot, stack, knight ? 1 : 0);
        }
        cells.add(pos);
        return be.count();
    }

    /**
     * Extracts the cell with the real renderer and walks each piece's model corners through the renderer's pose and the item's own
     * transform: they have to lie in the piece's quarter (x/z) and between y 0 and 1, with the lowest corner on y 0.
     */
    private static int assertOnQuarters(Minecraft client, BlockPos pos) {
        if (!(client.level.getBlockEntity(pos) instanceof ChessPiecesBlockEntity be)) {
            throw new AssertionError("The client has no chess block entity at " + pos);
        }
        BlockEntityRenderer<ChessPiecesBlockEntity, ChessPiecesRenderer.State> renderer = client.getBlockEntityRenderDispatcher().getRenderer(be);
        if (renderer == null) throw new AssertionError("No renderer for the chess pieces block entity");
        ChessPiecesRenderer.State state = renderer.createRenderState();
        renderer.extractRenderState(be, state, 0.0F, Vec3.atCenterOf(pos).add(0.0, 2.0, 3.0), null);
        int checked = 0;
        for (int slot = 0; slot < ChessPiecesBlockEntity.SLOTS; slot++) {
            if (be.piece(slot).isEmpty()) continue;
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

    private static void run(MinecraftServer server, String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
    }
}
