package com.simplebuilding.modules.simpleend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.TintedParticleLeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * End leaves (owner 2026-10-13): see-through tendril fronds around a small crystal (own model, cutout, never a full
 * cube) with falling tinted leaf particles and now and then a drifting End-rod spark. {@link #DROP} marks the few leaves
 * a growing tree chose to carry its bud (1) or fruit (2); the loot table drops them always. Placed leaves carry nothing.
 * Both End leaves (owner N55, Duskwood too since N65) behave like a cobweb: no collision, whatever moves through is stuck
 * ({@link Entity#makeStuckInBlock}, which also resets the fall distance, so landing in them hurts nothing).
 * {@link #DENSITY} (owner TX22-4, 22.10.; replaces the N65 inner flag) counts the neighbours that are not air, whatever
 * they are: 0-3 loose (airy texture, weakest brake), 4-5 medium (the former outer texture, cobweb brake), 6 dense (the
 * former inner texture, strongest brake) - the deeper in a crown, the more cobweb. The hoe is the tool; anything else
 * breaks them as slowly as it would break a cobweb.
 * {@link #WAXED} (owner W24-4, 23.10.): a honeycomb waxes the leaves like copper (Vanilla wax particles and sound) and
 * freezes their density - it no longer follows the neighbours, so a builder keeps a loose or dense look where he wants
 * it; an axe scrapes the wax off (Vanilla wax-off particles and sound, one durability) and the density follows again.
 */
public class EndLeavesBlock extends TintedParticleLeavesBlock {
    public static final IntegerProperty DROP = IntegerProperty.create("drop", 0, 2);
    public static final int NONE = 0, BUD = 1, FRUIT = 2;

    public static final IntegerProperty DENSITY = IntegerProperty.create("density", 0, 2);
    public static final int LOOSE = 0, MEDIUM = 1, DENSE = 2;

    public static final BooleanProperty WAXED = BooleanProperty.create("waxed");

    public EndLeavesBlock(Properties properties) {
        super(0.01F, properties);
        registerDefaultState(defaultBlockState().setValue(DROP, NONE).setValue(DENSITY, MEDIUM).setValue(WAXED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DROP, DENSITY, WAXED);
    }

    /** Density from the neighbours that are not air (any block): 0-3 loose, 4-5 medium, 6 dense. */
    public static int density(BlockGetter level, BlockPos pos) {
        int n = 0;
        for (Direction dir : Direction.values()) if (!level.getBlockState(pos.relative(dir)).isAir()) n++;
        return n == 6 ? DENSE : n >= 4 ? MEDIUM : LOOSE;
    }

    /** Speed factor inside the leaves (x, y, z): medium is exactly the Vanilla cobweb (also with Weaving, which roughly
     * doubles sideways and multiplies falling by five, like Vanilla). */
    public static Vec3 stuckSpeed(int density, boolean weaving) {
        return switch (density) {
            case LOOSE -> weaving ? new Vec3(1.0, 0.75, 1.0) : new Vec3(0.5, 0.15, 0.5);
            case DENSE -> weaving ? new Vec3(0.3, 0.15, 0.3) : new Vec3(0.15, 0.03, 0.15);
            default -> weaving ? new Vec3(0.5, 0.25, 0.5) : new Vec3(0.25, 0.05F, 0.25);
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(DENSITY, density(context.getLevel(), context.getClickedPos()));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        BlockState next = super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
        return next.is(this) && !next.getValue(WAXED) ? next.setValue(DENSITY, density(level, pos)) : next;
    }

    /** Honeycomb waxes (density frozen), an axe scrapes the wax off (density follows the neighbours again). */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (stack.is(Items.HONEYCOMB) && !state.getValue(WAXED)) {
            // like Vanilla's honeycomb: the event on both sides (the acting client plays it itself, the server for the others)
            level.levelEvent(player, LevelEvent.PARTICLES_WAX_ON, pos, 0);
            if (!level.isClientSide()) {
                stack.consume(1, player);
                level.setBlock(pos, state.setValue(WAXED, true), Block.UPDATE_ALL_IMMEDIATE);
                level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(ItemTags.AXES) && state.getValue(WAXED)) {
            level.playSound(player, pos, SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.levelEvent(player, LevelEvent.PARTICLES_WAX_OFF, pos, 0);
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(WAXED, false).setValue(DENSITY, density(level, pos)), Block.UPDATE_ALL_IMMEDIATE);
                level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
                stack.hurtAndBreak(1, player, hand);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    /** Like Vanilla {@code WebBlock#entityInside}, braking by {@link #stuckSpeed density}. */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        boolean weaving = entity instanceof LivingEntity living && living.hasEffect(MobEffects.WEAVING);
        entity.makeStuckInBlock(state, stuckSpeed(state.getValue(DENSITY), weaving));
    }

    /** A hoe breaks them like leaves; anything else (also the bare hand) as fast as it would break a cobweb. */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (player.getMainHandItem().is(ItemTags.HOES)) return super.getDestroyProgress(state, player, level, pos);
        return Blocks.COBWEB.defaultBlockState().getDestroyProgress(player, level, pos);
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
