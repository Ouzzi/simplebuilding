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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Sculk-Kiefer (Besitzer N24, N34): lautlose Falle. Wer hineingeraet, wird staerker gebremst als im Spinnennetz
 * ({@link #STUCK}) und bekommt Dunkelheit; die offene Kiefer klappt beim ersten Opfer zu ({@link #SNAPPED}) und beisst
 * einmal ({@link #BITE_DAMAGE}). Danach bleibt sie geschlossen und still, solange jemand drinsteht - kein weiterer
 * Schaden, kein Auf-und-Zu. Erst {@link #REOPEN_TICKS} nachdem niemand mehr drin ist, oeffnet sie sich wieder.
 * Weder Beissen noch Oeffnen machen ein Geraeusch (Klangtyp {@code EMPTY}); Wardens ignoriert sie, Zuschauer auch.
 */
public class SculkJawBlock extends WebBlock {
    public static final BooleanProperty SNAPPED = BlockStateProperties.TRIGGERED;
    /** Schaden des einen Bisses: 1 Herz. */
    public static final float BITE_DAMAGE = 2.0F;
    /** Ticks zwischen zwei Pruefungen der geschlossenen Kiefer; ist dann niemand mehr drin, oeffnet sie sich. */
    public static final int REOPEN_TICKS = 20;
    /** Bremse in der Kiefer (Spinnennetz: 0,25 / 0,05). */
    public static final Vec3 STUCK = new Vec3(0.12, 0.02, 0.12);
    /** Dunkelheit in Ticks, aufgefrischt solange das Opfer drinsteht. */
    public static final int DARKNESS_TICKS = 60;
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
        if (entity instanceof Warden || entity.isSpectator()) {
            return;
        }
        entity.makeStuckInBlock(state, STUCK);
        if (level instanceof ServerLevel server) {
            bite(state, server, pos, entity);
            if (entity instanceof LivingEntity living) {
                MobEffectInstance dark = living.getEffect(MobEffects.DARKNESS);
                if (dark == null || dark.getDuration() < DARKNESS_TICKS - 20) {
                    living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DARKNESS_TICKS, 0, false, false, true));
                }
            }
        }
    }

    /**
     * Beisst zu, wenn die Kiefer offen ist und {@code entity} ein beissbares Lebewesen: die Kiefer schliesst sich und
     * bleibt zu. Eine geschlossene Kiefer beisst nicht. Oeffentlich fuer die Spieltests.
     */
    public static boolean bite(BlockState state, ServerLevel level, BlockPos pos, Entity entity) {
        if (state.getValue(SNAPPED) || !isVictim(entity)) {
            return false;
        }
        level.setBlock(pos, state.setValue(SNAPPED, true), Block.UPDATE_ALL);
        level.scheduleTick(pos, state.getBlock(), REOPEN_TICKS);
        ((LivingEntity) entity).hurtServer(level, level.damageSources().generic(), BITE_DAMAGE);
        return true;
    }

    private static boolean isVictim(Entity entity) {
        return entity instanceof LivingEntity living && living.isAlive() && !living.isSpectator() && !(living instanceof Warden);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(SNAPPED)) {
            return;
        }
        if (level.getEntitiesOfClass(LivingEntity.class, SHAPE.bounds().move(pos), SculkJawBlock::isVictim).isEmpty()) {
            level.setBlock(pos, state.setValue(SNAPPED, false), Block.UPDATE_ALL);
        } else {
            // Noch jemand drin: geschlossen bleiben (Besitzer N34, kein Flackern vor jedem Biss).
            level.scheduleTick(pos, this, REOPEN_TICKS);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SNAPPED);
    }
}
