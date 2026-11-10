package com.simplebuilding.modules.simplelayers.block;

import com.simplebuilding.modules.simplelayers.LayersRegistry;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Chalk Mark (BACK20, plan docs/ai/PLAN-B-BACK20-2026-11-10.md): a mark drawn with chalk on a wall, floor or ceiling - an arrow
 * (pointing where the drawer clicked on the face), a cross (clicked in the middle) or a circle (drawn sneaking), in white, red,
 * blue or yellow. It faces {@link #FACING} away from the block it is drawn on, has no item and no collision, breaks at once, drops
 * nothing, gives way to any block placed there and falls off with its support. A glow ink sac makes it glow (light 4), an ink sac
 * puts it out - like on a sign.
 */
public class ChalkMarkBlock extends Block {
    public enum Color implements StringRepresentable {
        WHITE, RED, BLUE, YELLOW;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum Symbol implements StringRepresentable {
        ARROW_UP, ARROW_RIGHT, ARROW_DOWN, ARROW_LEFT, CROSS, CIRCLE;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final EnumProperty<Color> COLOR = EnumProperty.create("color", Color.class);
    public static final EnumProperty<Symbol> SYMBOL = EnumProperty.create("symbol", Symbol.class);
    public static final BooleanProperty GLOWING = BooleanProperty.create("glowing");
    public static final int GLOW_LIGHT = 4;
    /** Clicks within this distance of the face centre draw a cross. */
    public static final double CENTRE = 0.2;
    private static final VoxelShape UP = Block.box(0, 0, 0, 16, 0.5, 16), DOWN = Block.box(0, 15.5, 0, 16, 16, 16),
            NORTH = Block.box(0, 0, 15.5, 16, 16, 16), SOUTH = Block.box(0, 0, 0, 16, 16, 0.5),
            EAST = Block.box(0, 0, 0, 0.5, 16, 16), WEST = Block.box(15.5, 0, 0, 16, 16, 16);

    public ChalkMarkBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(COLOR, Color.WHITE).setValue(SYMBOL, Symbol.CROSS)
                .setValue(GLOWING, Boolean.FALSE));
    }

    public static int light(BlockState state) {
        return state.getValue(GLOWING) ? GLOW_LIGHT : 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, COLOR, SYMBOL, GLOWING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case DOWN -> DOWN;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            default -> UP;
        };
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos support = pos.relative(facing.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighbourPos,
                                     BlockState neighbour, RandomSource random) {
        return direction == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        boolean glow = stack.is(Items.GLOW_INK_SAC), ink = stack.is(Items.INK_SAC);
        if (!glow && !ink || state.getValue(GLOWING) == glow) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(GLOWING, glow), 3);
            level.playSound(null, pos, glow ? SoundEvents.GLOW_INK_SAC_USE : SoundEvents.INK_SAC_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /**
     * The symbol a click draws: sneaking a circle, the middle of the face a cross, else an arrow towards the clicked side. {@code d}
     * is the click relative to the centre of the mark's block; on walls "right" is the drawer's right hand, on floors and ceilings
     * the arrow points along x / z (up = north on the floor, south on the ceiling, as the texture lies).
     */
    public static Symbol symbolFor(Direction facing, Vec3 d, boolean sneaking) {
        if (sneaking) return Symbol.CIRCLE;
        double u, v;
        if (facing == Direction.UP) {
            u = d.x;
            v = -d.z;
        } else if (facing == Direction.DOWN) {
            u = d.x;
            v = d.z;
        } else {
            Direction right = facing.getCounterClockWise();
            u = d.x * right.getStepX() + d.z * right.getStepZ();
            v = d.y;
        }
        if (Math.max(Math.abs(u), Math.abs(v)) < CENTRE) return Symbol.CROSS;
        if (Math.abs(u) > Math.abs(v)) return u > 0 ? Symbol.ARROW_RIGHT : Symbol.ARROW_LEFT;
        return v > 0 ? Symbol.ARROW_UP : Symbol.ARROW_DOWN;
    }

    /**
     * Draws a mark for a click on {@code clicked} (face {@code face}, at {@code hit}): over an existing mark it redraws that one,
     * else into the free space in front of a sturdy face. Server and client agree; returns whether a mark was drawn.
     */
    public static boolean draw(Level level, BlockPos clicked, Direction face, Vec3 hit, Color color, boolean glow, boolean sneaking) {
        BlockState there = level.getBlockState(clicked);
        BlockPos target;
        Direction facing;
        if (there.is(LayersRegistry.CHALK_MARK)) {
            target = clicked;
            facing = there.getValue(FACING);
            glow |= there.getValue(GLOWING); // redrawing keeps the glow
        } else {
            target = clicked.relative(face);
            facing = face;
            BlockState free = level.getBlockState(target);
            boolean replaceable = free.is(LayersRegistry.CHALK_MARK) || free.canBeReplaced() && free.getFluidState().isEmpty();
            if (!replaceable || !there.isFaceSturdy(level, clicked, face)) return false;
        }
        Symbol symbol = symbolFor(facing, hit.subtract(Vec3.atCenterOf(target)), sneaking);
        BlockState mark = LayersRegistry.CHALK_MARK.defaultBlockState().setValue(FACING, facing).setValue(COLOR, color).setValue(SYMBOL, symbol)
                .setValue(GLOWING, glow);
        if (!level.isClientSide()) {
            level.setBlock(target, mark, 3);
            level.playSound(null, target, SoundEvents.CALCITE_HIT, SoundSource.BLOCKS, 0.6F, 1.4F + level.getRandom().nextFloat() * 0.2F);
        }
        return true;
    }
}
