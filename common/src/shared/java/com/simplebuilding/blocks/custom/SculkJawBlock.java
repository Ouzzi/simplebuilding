package com.simplebuilding.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

/**
 * Sculk-Kiefer (Besitzer N24): lautlose Falle. Wer hineingeraet, wird wie im Spinnennetz gebremst ({@link WebBlock}); die
 * offene Kiefer klappt dabei zu ({@link #SNAPPED}) und beisst zu ({@link #BITE_DAMAGE}). Nach {@link #REOPEN_TICKS} oeffnet sie
 * sich wieder und beisst erneut, solange jemand drinsteht. Weder Beissen noch Oeffnen machen ein Geraeusch (Klangtyp
 * {@code EMPTY}); Wardens ignoriert sie, Zuschauer auch.
 */
public class SculkJawBlock extends WebBlock {
    public static final BooleanProperty SNAPPED = BlockStateProperties.TRIGGERED;
    /** Schaden je Biss: 1,5 Herzen. */
    public static final float BITE_DAMAGE = 3.0F;
    /** Ticks, bis die zugeschnappte Kiefer sich wieder oeffnet. */
    public static final int REOPEN_TICKS = 20;
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);

    public SculkJawBlock(Properties settings) {
        super(settings);
        registerDefaultState(stateDefinition.any().setValue(SNAPPED, false));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier effects, boolean intersects) {
        super.entityInside(state, level, pos, entity, effects, intersects);
        if (level instanceof ServerLevel server) {
            bite(state, server, pos, entity);
        }
    }

    /** Beisst zu, wenn die Kiefer offen ist und {@code entity} ein beissbares Lebewesen. Oeffentlich fuer die Spieltests. */
    public static boolean bite(BlockState state, ServerLevel level, BlockPos pos, Entity entity) {
        if (state.getValue(SNAPPED) || !(entity instanceof LivingEntity living) || living.isSpectator() || living instanceof Warden) {
            return false;
        }
        level.setBlock(pos, state.setValue(SNAPPED, true), Block.UPDATE_ALL);
        level.scheduleTick(pos, state.getBlock(), REOPEN_TICKS);
        living.hurtServer(level, level.damageSources().generic(), BITE_DAMAGE);
        return true;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(SNAPPED)) {
            level.setBlock(pos, state.setValue(SNAPPED, false), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SNAPPED);
    }
}
