package com.simplebuilding.blocks.custom;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.blocks.entity.custom.ChessPiecesBlockEntity;
import com.simplebuilding.chess.ChessItems;
import com.simplebuilding.chess.ChessPiece;
import com.simplebuilding.version.BlockCodecs;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Bis zu vier Schachfiguren auf einem Block (docs/ai/PLAN-SCHACH-2026-10-06.md): je eine auf einem Viertel der
 * Oberseite - genau ein Feld des Quarz-Schachbretts darunter. Die Figuren (Items) liegen im
 * {@link ChessPiecesBlockEntity}; gezeichnet werden sie vom {@code ChessPiecesRenderer}, das Blockmodell traegt nur die
 * Partikeltextur. {@link #FACING} ist die Blickrichtung der Zelle, jede Figur dreht sich davon weiter.
 *
 * <p>Gesetzt wird ueber die Figuren-Items ({@link com.simplebuilding.items.custom.ChessPieceItem}). Leere Hand: dreht die
 * angeklickte Figur um 90 Grad; mit Schleichen nimmt sie sie auf. Braucht einen tragfaehigen Boden, wasserfuellbar,
 * von Kolben zerstoert; Abbauen droppt alle Figuren ({@link #getDrops}, keine Loot-Tabelle).
 */
public class ChessPiecesBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    /** Form einer Zelle ohne Figuren (kommt nur kurz vor, z. B. beim Setzen). */
    public static final VoxelShape EMPTY_SHAPE = Block.box(4, 0, 4, 12, 1, 12);
    public static final MapCodec<ChessPiecesBlock> CODEC = BlockCodecs.simple(ChessPiecesBlock::new);

    public ChessPiecesBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
    }

    // No @Override: MC 26.3 removed block codecs; this only overrides on 26.2.
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    /** Platz (Viertel) unter einem Punkt (Weltkoordinaten) in der Zelle {@code pos}: {@code qx | qz << 1}. */
    public static int slotAt(BlockPos pos, Vec3 point) {
        int qx = point.x - pos.getX() >= 0.5 ? 1 : 0;
        int qz = point.z - pos.getZ() >= 0.5 ? 1 : 0;
        return qx | qz << 1;
    }

    /** Der Platz unter dem Trefferpunkt (ein Stueck in die getroffene Flaeche hinein). */
    public static int hitSlot(BlockPos pos, BlockHitResult hit) {
        Direction face = hit.getDirection();
        return slotAt(pos, hit.getLocation().add(face.getStepX() * -0.01, 0, face.getStepZ() * -0.01));
    }

    /** Fuss mal Hoehe einer Figur auf einem Platz (Pixel). */
    public static VoxelShape slotBox(int slot, int height) {
        double cx = (slot & 1) * 8 + 4;
        double cz = (slot >> 1) * 8 + 4;
        double half = ChessPiece.FOOT / 2.0;
        return Block.box(cx - half, 0, cz - half, cx + half, Math.max(1, height), cz + half);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean water = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection()).setValue(WATERLOGGED, water);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return level.getBlockEntity(pos) instanceof ChessPiecesBlockEntity be ? be.shape() : EMPTY_SHAPE;
    }

    /** Braucht einen Boden mit voller Oberseite (ein Schachbrett, ein Stein, ein voller Achtelblock ...). */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChessPiecesBlockEntity(pos, state);
    }

    /** Alle Figuren der Zelle. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        return be instanceof ChessPiecesBlockEntity pieces ? pieces.all() : List.of();
    }

    /** Mittlere Maustaste: die erste Figur der Zelle. */
    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        if (level.getBlockEntity(pos) instanceof ChessPiecesBlockEntity be) {
            List<ItemStack> all = be.all();
            if (!all.isEmpty()) {
                return all.getFirst();
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Leere Hand auf eine Figur: Schleichen nimmt sie auf (in die Hand), sonst dreht sie sich eine Vierteldrehung im
     * Uhrzeigersinn. Die letzte aufgenommene Figur raeumt die Zelle (Wasser bleibt).
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.mayBuild() || !(level.getBlockEntity(pos) instanceof ChessPiecesBlockEntity be)) {
            return InteractionResult.PASS;
        }
        int slot = hitSlot(pos, hit);
        if (be.piece(slot).isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive()) {
            ItemStack taken = be.take(slot);
            ChessItems.give(player, taken);
            clearIfEmpty(level, pos, state, be);
            level.playSound(null, pos, state.getSoundType().getHitSound(), SoundSource.BLOCKS, 0.8F, 1.4F);
        } else {
            be.rotate(slot);
            level.playSound(null, pos, state.getSoundType().getStepSound(), SoundSource.BLOCKS, 0.6F, 1.6F);
        }
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        return InteractionResult.SUCCESS;
    }

    /** Mit einer Figur in der Hand entscheidet das Item (setzen bzw. tauschen), die Figur darunter dreht sich nicht. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof com.simplebuilding.items.custom.ChessPieceItem) {
            return InteractionResult.PASS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    /** Ohne Figuren verschwindet die Zelle (eine Wasserquelle bleibt). */
    public static void clearIfEmpty(Level level, BlockPos pos, BlockState state, ChessPiecesBlockEntity be) {
        if (be.count() == 0) {
            level.setBlock(pos, state.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
