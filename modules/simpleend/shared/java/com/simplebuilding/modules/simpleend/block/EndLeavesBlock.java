package com.simplebuilding.modules.simpleend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TintedParticleLeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;

/**
 * End leaves (owner 2026-10-13): see-through tendril fronds around a small crystal (own model, cutout, never a full
 * cube) with falling tinted leaf particles and now and then a drifting End-rod spark. {@link #DROP} marks the few leaves
 * a growing tree chose to carry its bud (1) or fruit (2); the loot table drops them always. Placed leaves carry nothing.
 * Both End leaves (owner N55, Duskwood too since N65) behave like a cobweb: no collision, whatever moves through is stuck
 * ({@link Entity#makeStuckInBlock}, which also resets the fall distance, so landing in them hurts nothing).
 * {@link #INNER} (owner N65) is set when all six neighbours are leaves, logs or stick scrub: such leaves use a denser
 * model and texture than the ones that face the air, so a crown has a ragged skin and a thicker heart.
 */
public class EndLeavesBlock extends TintedParticleLeavesBlock {
    public static final IntegerProperty DROP = IntegerProperty.create("drop", 0, 2);
    public static final int NONE = 0, BUD = 1, FRUIT = 2;

    public static final BooleanProperty INNER = BooleanProperty.create("inner");

    public EndLeavesBlock(Properties properties) {
        super(0.01F, properties);
        registerDefaultState(defaultBlockState().setValue(DROP, NONE).setValue(INNER, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DROP, INNER);
    }

    /** True when no side faces open air: every neighbour is End leaves, a log or stick scrub. */
    public static boolean isInner(BlockGetter level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockState n = level.getBlockState(pos.relative(dir));
            if (!(n.getBlock() instanceof EndLeavesBlock || n.getBlock() instanceof EndSticksBlock || n.is(BlockTags.LOGS))) return false;
        }
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(INNER, isInner(context.getLevel(), context.getClickedPos()));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        BlockState next = super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
        return next.is(this) ? next.setValue(INNER, isInner(level, pos)) : next;
    }

    /** Same numbers as Vanilla {@code WebBlock#entityInside}. */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        Vec3 speed = new Vec3(0.25, 0.05F, 0.25);
        if (entity instanceof LivingEntity living && living.hasEffect(MobEffects.WEAVING)) speed = new Vec3(0.5, 0.25, 0.5);
        entity.makeStuckInBlock(state, speed);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (random.nextInt(160) == 0 && level.getBlockState(pos.below()).isAir()) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + random.nextDouble(), pos.getY() - 0.05, pos.getZ() + random.nextDouble(),
                    0.0, -0.01, 0.0);
        }
    }
}
