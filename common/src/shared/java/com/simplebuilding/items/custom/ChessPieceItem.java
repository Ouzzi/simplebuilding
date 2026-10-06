package com.simplebuilding.items.custom;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.ChessPiecesBlock;
import com.simplebuilding.blocks.entity.custom.ChessPiecesBlockEntity;
import com.simplebuilding.chess.ChessItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Eine Schachfigur (Farbe, Art, 3D oder flach; docs/ai/PLAN-SCHACH-2026-10-06.md). Rechtsklick stellt sie auf das
 * angeklickte Viertel - ein Feld des Quarz-Schachbretts: auf die Oberseite eines tragfaehigen Blocks oder auf einen
 * freien Platz einer Figurenzelle. Sie schaut in die Blickrichtung des Spielers. Schleichen + Rechtsklick auf eine
 * stehende Figur tauscht: die neue steht, die alte kommt in die Hand (wenn es die letzte Figur im Stapel war), sonst
 * ins Inventar.
 */
public class ChessPieceItem extends Item {
    private final ChessItems.PieceKind kind;

    public ChessPieceItem(ChessItems.PieceKind kind, Item.Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public ChessItems.PieceKind kind() {
        return this.kind;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Block block = ModBlocks.CHESS_PIECES;
        Player player = context.getPlayer();
        if (block == null || (player != null && !player.mayBuild())) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        Direction face = context.getClickedFace();
        Direction look = player != null ? player.getDirection() : Direction.NORTH;
        ItemStack stack = context.getItemInHand();
        BlockState clickedState = level.getBlockState(clicked);
        if (clickedState.is(block) && level.getBlockEntity(clicked) instanceof ChessPiecesBlockEntity be) {
            int slot = ChessPiecesBlock.slotAt(clicked, context.getClickLocation().add(face.getStepX() * -0.01, 0, face.getStepZ() * -0.01));
            if (!be.piece(slot).isEmpty()) {
                if (player == null || !player.isSecondaryUseActive()) {
                    return InteractionResult.FAIL;
                }
                return swap(level, clicked, clickedState, be, slot, player, context.getHand(), stack, look);
            }
            return put(level, clicked, clickedState, be, slot, player, stack, look);
        }
        Vec3 target = context.getClickLocation().add(face.getStepX() * 0.25, face.getStepY() * 0.25, face.getStepZ() * 0.25);
        BlockPos cell = BlockPos.containing(target);
        int slot = ChessPiecesBlock.slotAt(cell, target);
        if (player != null && !level.mayInteract(player, cell)) {
            return InteractionResult.FAIL;
        }
        BlockState there = level.getBlockState(cell);
        if (there.is(block) && level.getBlockEntity(cell) instanceof ChessPiecesBlockEntity be) {
            return be.piece(slot).isEmpty() ? put(level, cell, there, be, slot, player, stack, look) : InteractionResult.FAIL;
        }
        BlockState fresh = block.defaultBlockState().setValue(ChessPiecesBlock.FACING, look)
                .setValue(ChessPiecesBlock.WATERLOGGED, level.getFluidState(cell).getType() == Fluids.WATER);
        if (!there.canBeReplaced() || !fresh.canSurvive(level, cell)
                || !level.isUnobstructed(null, ChessPiecesBlock.slotBox(slot, this.kind.piece().height(this.kind.flat()))
                .move(cell.getX(), cell.getY(), cell.getZ()))) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!level.setBlock(cell, fresh, Block.UPDATE_ALL) || !(level.getBlockEntity(cell) instanceof ChessPiecesBlockEntity be)) {
            return InteractionResult.FAIL;
        }
        return put(level, cell, fresh, be, slot, player, stack, look);
    }

    /** Vierteldrehungen von der Blickrichtung der Zelle zur Blickrichtung des Spielers. */
    private static int rotation(BlockState cell, Direction look) {
        return Math.floorMod(look.get2DDataValue() - cell.getValue(ChessPiecesBlock.FACING).get2DDataValue(), 4);
    }

    private InteractionResult put(Level level, BlockPos pos, BlockState state, ChessPiecesBlockEntity be, int slot,
                                  @Nullable Player player, ItemStack stack, Direction look) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!be.put(slot, stack, rotation(state, look))) {
            return InteractionResult.FAIL;
        }
        placed(level, pos, state, player);
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }

    private InteractionResult swap(Level level, BlockPos pos, BlockState state, ChessPiecesBlockEntity be, int slot,
                                   Player player, InteractionHand hand, ItemStack stack, Direction look) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack old = be.take(slot);
        be.put(slot, stack, rotation(state, look));
        placed(level, pos, state, player);
        if (!player.getAbilities().instabuild && stack.getCount() == 1) {
            player.setItemInHand(hand, old);
        } else {
            stack.consume(1, player);
            ChessItems.give(player, old);
        }
        return InteractionResult.SUCCESS;
    }

    private static void placed(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        SoundType sound = state.getSoundType();
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 1.3F);
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, state));
    }
}
