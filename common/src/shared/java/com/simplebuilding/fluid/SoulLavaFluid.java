package com.simplebuilding.fluid;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

/**
 * Soul lava (plan section 9, owner round 2 answers 25-31): lava's behaviour with
 * <ul>
 *   <li>a short, slow flow: {@link SoulLava#FLOW_OVERWORLD} blocks (Nether {@link SoulLava#FLOW_NETHER}), every
 *       {@link SoulLava#TICK_DELAY_OVERWORLD} (Nether {@link SoulLava#TICK_DELAY_NETHER}) ticks - drop-off 1 plus a
 *       minimum level, so any reach 1..7 works;</li>
 *   <li>nothing replaces it, not even water flowing in (the water side turns, see {@link SoulLavaBlock});</li>
 *   <li>touching burns twice as long plus Seelenbrand ({@link SoulLava#touch});</li>
 *   <li>4x the fire attempts of lava with twice the reach.</li>
 * </ul>
 * It is in the {@code minecraft:lava} fluid tag (swimming, pathfinding, striders, item burning work like
 * lava); everything that differs is overridden here and in {@link SoulLavaBlock}. Loader parts (NeoForge/Forge
 * fluid type) come from subclasses created through {@link ModFluids#factory}.
 */
public abstract class SoulLavaFluid extends FlowingFluid {
    @Override
    public Fluid getFlowing() {
        return ModFluids.FLOWING_SOUL_LAVA;
    }

    @Override
    public Fluid getSource() {
        return ModFluids.SOUL_LAVA;
    }

    @Override
    public Item getBucket() {
        return ModFluids.SOUL_LAVA_BUCKET;
    }

    static boolean nether(LevelReader level) {
        return level.environmentAttributes().getDimensionValue(net.minecraft.world.attribute.EnvironmentAttributes.FAST_LAVA);
    }

    /** Horizontal reach in blocks in this level. */
    public static int reach(LevelReader level) {
        return nether(level) ? SoulLava.FLOW_NETHER : SoulLava.FLOW_OVERWORLD;
    }

    @Override
    public void animateTick(Level level, BlockPos pos, FluidState fluidState, RandomSource random) {
        BlockPos above = pos.above();
        if (level.getBlockState(above).isAir() && !level.getBlockState(above).isSolidRender()) {
            if (random.nextInt(100) == 0) {
                double x = pos.getX() + random.nextDouble();
                double y = pos.getY() + 1.0;
                double z = pos.getZ() + random.nextDouble();
                level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0.0, 0.04, 0.0);
                level.playLocalSound(x, y, z, SoundEvents.LAVA_POP, SoundSource.AMBIENT, 0.2F + random.nextFloat() * 0.2F,
                        0.7F + random.nextFloat() * 0.15F, false);
            }
            if (random.nextInt(200) == 0) {
                level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.AMBIENT,
                        0.3F + random.nextFloat() * 0.2F, 0.8F + random.nextFloat() * 0.15F, false);
            }
        }
    }

    /** Lava's fire spread, {@link SoulLava#IGNITE_ATTEMPTS} times per tick and with {@link SoulLava#IGNITE_RANGE}x the reach. */
    @Override
    public void randomTick(ServerLevel level, BlockPos pos, FluidState fluidState, RandomSource random) {
        if (!level.canSpreadFireAround(pos)) return;
        for (int attempt = 0; attempt < SoulLava.IGNITE_ATTEMPTS; attempt++) {
            igniteOnce(level, pos, random);
        }
    }

    private void igniteOnce(ServerLevel level, BlockPos pos, RandomSource random) {
        int range = SoulLava.IGNITE_RANGE;
        int passes = random.nextInt(3) * range;
        if (passes > 0) {
            BlockPos test = pos;
            for (int pass = 0; pass < passes; pass++) {
                test = test.offset(random.nextInt(3) - 1, 1, random.nextInt(3) - 1);
                if (!level.isLoaded(test)) return;
                BlockState state = level.getBlockState(test);
                if (state.isAir()) {
                    if (hasFlammableNeighbours(level, test)) {
                        level.setBlockAndUpdate(test, BaseFireBlock.getState(level, test));
                        return;
                    }
                } else if (state.isCollisionShapeFullBlock(level, test)) {
                    return;
                }
            }
        } else {
            int spread = 1 + range;
            for (int i = 0; i < 3 * range; i++) {
                BlockPos test = pos.offset(random.nextInt(2 * spread - 1) - (spread - 1), 0, random.nextInt(2 * spread - 1) - (spread - 1));
                if (!level.isLoaded(test)) return;
                if (level.isEmptyBlock(test.above()) && isFlammable(level, test)) {
                    level.setBlockAndUpdate(test.above(), BaseFireBlock.getState(level, test));
                }
            }
        }
    }

    private boolean hasFlammableNeighbours(LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (isFlammable(level, pos.relative(direction))) return true;
        }
        return false;
    }

    private boolean isFlammable(LevelReader level, BlockPos pos) {
        return (!level.isInsideBuildHeight(pos.getY()) || level.hasChunkAt(pos)) && level.getBlockState(pos).ignitedByLava();
    }

    @Override
    protected void entityInside(Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects) {
        effects.apply(InsideBlockEffectType.CLEAR_FREEZE);
        effects.apply(InsideBlockEffectType.LAVA_IGNITE);
        effects.runAfter(InsideBlockEffectType.LAVA_IGNITE, e -> SoulLava.touch(level, e));
    }

    @Override
    public @Nullable ParticleOptions getDripParticle() {
        return ParticleTypes.DRIPPING_LAVA;
    }

    @Override
    protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
        level.levelEvent(1501, pos, 0);
    }

    @Override
    public int getSlopeFindDistance(LevelReader level) {
        return Math.min(4, reach(level));
    }

    @Override
    public BlockState createLegacyBlock(FluidState fluidState) {
        return ModFluids.SOUL_LAVA_BLOCK.defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(fluidState));
    }

    @Override
    public boolean isSame(Fluid other) {
        return other == ModFluids.SOUL_LAVA || other == ModFluids.FLOWING_SOUL_LAVA;
    }

    @Override
    public int getDropOff(LevelReader level) {
        return 1;
    }

    /** Nothing flows into soul lava (owner 26: neither source nor flowing block is replaceable). */
    @Override
    public boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid other, Direction direction) {
        return false;
    }

    @Override
    public int getTickDelay(LevelReader level) {
        return nether(level) ? SoulLava.TICK_DELAY_NETHER : SoulLava.TICK_DELAY_OVERWORLD;
    }

    @Override
    protected boolean canConvertToSource(ServerLevel level) {
        return false;
    }

    /**
     * Drop-off 1 with a minimum level: a flowing block k steps from its source has level 8 - k, so a
     * reach of N blocks allows levels down to 8 - N. Falling soul lava restarts at 7 like every fluid.
     */
    @Override
    protected void spreadTo(LevelAccessor level, BlockPos pos, BlockState state, Direction direction, FluidState target) {
        if (direction != Direction.DOWN && !target.isEmpty() && !target.isSource() && !target.getValue(FALLING)
                && target.getAmount() < 8 - reach(level)) {
            return;
        }
        super.spreadTo(level, pos, state, direction, target);
    }

    @Override
    protected boolean isRandomlyTicking() {
        return true;
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0F;
    }

    @Override
    public Optional<SoundEvent> getPickupSound() {
        return Optional.of(SoundEvents.BUCKET_FILL_LAVA);
    }

    public static class Flowing extends SoulLavaFluid {
        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState fluidState) {
            return fluidState.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState fluidState) {
            return false;
        }
    }

    public static class Source extends SoulLavaFluid {
        @Override
        public int getAmount(FluidState fluidState) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState fluidState) {
            return true;
        }
    }
}
