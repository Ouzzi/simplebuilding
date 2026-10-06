package com.simplebuilding.blocks.custom;

import com.simplebuilding.chess.ChessColor;
import com.simplebuilding.chess.ChessItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Eine Blockzelle aus bis zu acht Achtelbloecken (0,5 x 0,5 x 0,5) einer Farbe (docs/ai/PLAN-SCHACH-2026-10-06.md).
 * Jedes Achtel ist ein Bit ({@link #OCTETS}, Name {@code o<x><y><z>}); die Farbe steht in {@link #COLOR}. Alles im
 * Blockzustand, kein Block-Entity: die Modelle sind gebacken (Multipart je Farbe und Bit), auch grosse Bauten kosten
 * nichts beim Zeichnen.
 *
 * <p>Gesetzt wird ueber die Achtel-Items ({@link com.simplebuilding.items.custom.CheckerOctetItem}) nach Trefferpunkt.
 * Wasserbindbar, solange die Zelle nicht voll ist; voll (8/8) ist sie ein ganzer Wuerfel ohne Wasser und bleibt
 * Achtelzelle. Abbauen droppt jedes Achtel ({@link #getDrops}, keine Loot-Tabelle); Schleichen + leere Hand nimmt das
 * angeklickte Achtel heraus.
 */
public class CheckerOctetBlock extends Block implements SimpleWaterloggedBlock {
    public static final EnumProperty<ChessColor> COLOR = EnumProperty.create("color", ChessColor.class);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    /** Die acht Achtel; Index {@code x | y << 1 | z << 2} (0 = unten, Nord, West). */
    public static final BooleanProperty[] OCTETS = new BooleanProperty[8];
    public static final int FULL = 0xFF;
    private static final VoxelShape[] SHAPES = new VoxelShape[256];

    static {
        for (int i = 0; i < 8; i++) {
            OCTETS[i] = BooleanProperty.create("o" + x(i) + y(i) + z(i));
        }
        for (int mask = 0; mask < 256; mask++) {
            VoxelShape shape = Shapes.empty();
            for (int i = 0; i < 8; i++) {
                if ((mask & (1 << i)) != 0) {
                    shape = Shapes.or(shape, box(i));
                }
            }
            SHAPES[mask] = shape.optimize();
        }
    }

    public CheckerOctetBlock(BlockBehaviour.Properties properties) {
        super(properties);
        BlockState state = this.stateDefinition.any().setValue(COLOR, ChessColor.QUARTZ).setValue(WATERLOGGED, false);
        for (int i = 0; i < 8; i++) {
            state = state.setValue(OCTETS[i], i == 0);
        }
        this.registerDefaultState(state);
    }

    public static int index(int x, int y, int z) {
        return (x & 1) | (y & 1) << 1 | (z & 1) << 2;
    }

    public static int x(int index) {
        return index & 1;
    }

    public static int y(int index) {
        return (index >> 1) & 1;
    }

    public static int z(int index) {
        return (index >> 2) & 1;
    }

    /** Die Box eines Achtels in Blockkoordinaten. */
    public static VoxelShape box(int index) {
        return Block.box(x(index) * 8, y(index) * 8, z(index) * 8, x(index) * 8 + 8, y(index) * 8 + 8, z(index) * 8 + 8);
    }

    /** Welche Achtel gesetzt sind, als Bitmaske. */
    public static int mask(BlockState state) {
        int mask = 0;
        for (int i = 0; i < 8; i++) {
            if (state.getValue(OCTETS[i])) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    /** Wie viele Achtel gesetzt sind. */
    public static int count(BlockState state) {
        return Integer.bitCount(mask(state));
    }

    /** Der Zustand mit genau diesen Achteln; volle Zellen halten kein Wasser. */
    public static BlockState withMask(BlockState state, int mask) {
        for (int i = 0; i < 8; i++) {
            state = state.setValue(OCTETS[i], (mask & (1 << i)) != 0);
        }
        return mask == FULL ? state.setValue(WATERLOGGED, false) : state;
    }

    /** Das Achtel, in dem ein Punkt (Weltkoordinaten) in der Zelle {@code pos} liegt. */
    public static int indexAt(BlockPos pos, Vec3 point) {
        return index(half(point.x - pos.getX()), half(point.y - pos.getY()), half(point.z - pos.getZ()));
    }

    private static int half(double local) {
        return local >= 0.5 ? 1 : 0;
    }

    /** Das Achtel unter dem Trefferpunkt (ein Stueck in die getroffene Flaeche hinein). */
    public static int hitIndex(BlockPos pos, BlockHitResult hit) {
        Direction face = hit.getDirection();
        Vec3 inside = hit.getLocation().add(face.getStepX() * -0.25, face.getStepY() * -0.25, face.getStepZ() * -0.25);
        return indexAt(pos, inside);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COLOR, WATERLOGGED);
        builder.add(OCTETS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[mask(state)];
    }

    /** Teilweise gefuellte Zellen lassen Licht durch wie Stufen. */
    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return mask(state) != FULL;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    /** Wasser nur in nicht volle Zellen. */
    @Override
    public boolean canPlaceLiquid(@Nullable LivingEntity owner, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return mask(state) != FULL && SimpleWaterloggedBlock.super.canPlaceLiquid(owner, level, pos, state, fluid);
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
        return mask(state) != FULL && SimpleWaterloggedBlock.super.placeLiquid(level, pos, state, fluid);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    /** Je gesetztes Achtel ein Achtel-Item seiner Farbe. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        int count = count(state);
        return count == 0 ? List.of() : List.of(new ItemStack(ChessItems.octet(state.getValue(COLOR)), count));
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(ChessItems.octet(state.getValue(COLOR)));
    }

    /** Schleichen + leere Hand: das angeklickte Achtel kommt in die Hand. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isSecondaryUseActive() || !player.mayBuild()) {
            return InteractionResult.PASS;
        }
        int index = hitIndex(pos, hit);
        int mask = mask(state);
        if ((mask & (1 << index)) == 0) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            int rest = mask & ~(1 << index);
            BlockState after = rest == 0
                    ? (state.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState())
                    : withMask(state, rest);
            level.setBlock(pos, after, Block.UPDATE_ALL);
            ChessItems.give(player, new ItemStack(ChessItems.octet(state.getValue(COLOR))));
            level.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.8F, 1.2F);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
