package com.simplebuilding.woodwork;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.version.BlockCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A wooden cauldron (docs/ai/PLAN-HOLZWERK-2026-10-09.md): a Vanilla cauldron made of wood. Its content (empty,
 * water, lava, powder snow) is mapped onto the matching Vanilla cauldron and the Vanilla interaction table runs
 * (buckets, bottles, washing, rain, dripstone), the result is mapped back - the same approach as SimpleLib's
 * reinforced cauldron. It burns: lava poured into a cauldron of burnable wood sets it alight, and after
 * {@link #BURN_TICKS} the cauldron is gone and the lava is left as a source block. Crimson and warped cauldrons do
 * not burn and keep their lava.
 */
public class WoodenCauldronBlock extends AbstractCauldronBlock {
    public enum Content implements StringRepresentable {
        EMPTY("empty"), WATER("water"), LAVA("lava"), POWDER_SNOW("powder_snow");

        private final String name;

        Content(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public boolean layered() {
            return this == WATER || this == POWDER_SNOW;
        }
    }

    public static final MapCodec<WoodenCauldronBlock> CODEC = BlockCodecs.simple(WoodenCauldronBlock::new);
    public static final EnumProperty<Content> CONTENT = EnumProperty.create("content", Content.class);
    public static final IntegerProperty LEVEL = LayeredCauldronBlock.LEVEL;
    /** How long lava burns a wooden cauldron before it is left as a source block (3 s). */
    public static final int BURN_TICKS = 60;
    private static final VoxelShape[] FILLED = new VoxelShape[4];

    static {
        for (int level = 1; level <= 3; level++) {
            FILLED[level] = Shapes.or(AbstractCauldronBlock.SHAPE, Block.column(12.0, 4.0, 6.0 + level * 3.0));
        }
    }

    private final WoodKind wood;

    public WoodenCauldronBlock(Properties properties) {
        this(WoodKind.OAK, properties);
    }

    public WoodenCauldronBlock(WoodKind wood, Properties properties) {
        super(properties, new CauldronInteraction.Dispatcher());
        this.wood = wood;
        registerDefaultState(this.stateDefinition.any().setValue(CONTENT, Content.EMPTY).setValue(LEVEL, 3));
    }

    // No @Override: MC 26.3 removed block codecs; this only overrides on 26.2.
    protected MapCodec<? extends AbstractCauldronBlock> codec() {
        return CODEC;
    }

    public WoodKind wood() {
        return this.wood;
    }

    /** Lava burns this cauldron (every wood except crimson and warped). */
    public boolean burns() {
        return !this.wood.nether();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONTENT, LEVEL);
    }

    public BlockState with(Content content, int level) {
        return defaultBlockState().setValue(CONTENT, content).setValue(LEVEL, content.layered() ? Math.max(1, Math.min(3, level)) : 3);
    }

    public static int levelOf(BlockState state) {
        return state.getValue(CONTENT).layered() ? state.getValue(LEVEL) : 3;
    }

    @Override
    public boolean isFull(BlockState state) {
        return state.getValue(CONTENT) != Content.EMPTY && levelOf(state) == 3;
    }

    @Override
    protected double getContentHeight(BlockState state) {
        Content content = state.getValue(CONTENT);
        if (content == Content.EMPTY) {
            return 0.0;
        }
        return content.layered() ? (6.0 + 3.0 * levelOf(state)) / 16.0 : 0.9375;
    }

    // ------------------------------------------------------------------ Vanilla mapping

    public static BlockState toVanilla(BlockState state) {
        return switch (state.getValue(CONTENT)) {
            case EMPTY -> Blocks.CAULDRON.defaultBlockState();
            case WATER -> Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, levelOf(state));
            case LAVA -> Blocks.LAVA_CAULDRON.defaultBlockState();
            case POWDER_SNOW -> Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, levelOf(state));
        };
    }

    public @Nullable BlockState fromVanilla(BlockState vanilla) {
        if (vanilla.is(Blocks.CAULDRON)) return with(Content.EMPTY, 3);
        if (vanilla.is(Blocks.WATER_CAULDRON)) return with(Content.WATER, vanilla.getValue(LayeredCauldronBlock.LEVEL));
        if (vanilla.is(Blocks.LAVA_CAULDRON)) return with(Content.LAVA, 3);
        if (vanilla.is(Blocks.POWDER_SNOW_CAULDRON)) return with(Content.POWDER_SNOW, vanilla.getValue(LayeredCauldronBlock.LEVEL));
        return null;
    }

    private static CauldronInteraction.Dispatcher interactions(Content content) {
        return switch (content) {
            case WATER -> CauldronInteractions.WATER;
            case LAVA -> CauldronInteractions.LAVA;
            case POWDER_SNOW -> CauldronInteractions.POWDER_SNOW;
            default -> CauldronInteractions.EMPTY;
        };
    }

    /** After a Vanilla behaviour wrote a Vanilla cauldron at {@code pos}, turns it back into this cauldron. */
    public void adopt(Level level, BlockPos pos) {
        BlockState now = level.getBlockState(pos);
        if (now.getBlock() == this) return;
        BlockState back = fromVanilla(now);
        if (back != null) {
            level.setBlockAndUpdate(pos, back);
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        InteractionResult result = interactions(state.getValue(CONTENT)).get(stack).interact(toVanilla(state), level, pos, player, hand, stack);
        if (!level.isClientSide()) adopt(level, pos);
        return result;
    }

    @Override
    public void handlePrecipitation(BlockState state, Level level, BlockPos pos, Biome.Precipitation precipitation) {
        BlockState vanilla = toVanilla(state);
        vanilla.getBlock().handlePrecipitation(vanilla, level, pos, precipitation);
        adopt(level, pos);
    }

    @Override
    protected boolean canReceiveStalactiteDrip(Fluid fluid) {
        return fluid == Fluids.WATER || fluid == Fluids.LAVA;
    }

    @Override
    protected void receiveStalactiteDrip(BlockState state, Level level, BlockPos pos, Fluid fluid) {
        Content content = state.getValue(CONTENT);
        BlockState next = null;
        if (content == Content.EMPTY) {
            next = fluid == Fluids.WATER ? with(Content.WATER, 1) : fluid == Fluids.LAVA ? with(Content.LAVA, 3) : null;
        } else if (content == Content.WATER && fluid == Fluids.WATER && levelOf(state) < 3) {
            next = with(Content.WATER, levelOf(state) + 1);
        }
        if (next == null) return;
        level.setBlockAndUpdate(pos, next);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(next));
        level.levelEvent(fluid == Fluids.LAVA ? 1046 : 1047, pos, 0);
    }

    // ------------------------------------------------------------------ burning

    /** Lava in a burnable cauldron: start the fire timer. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (burns() && state.getValue(CONTENT) == Content.LAVA && !level.isClientSide()) {
            level.scheduleTick(pos, this, BURN_TICKS);
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8F, 0.8F);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (burns() && state.getValue(CONTENT) == Content.LAVA) {
            burnOut(level, pos);
            return;
        }
        super.tick(state, level, pos, random);
    }

    /** The cauldron is burnt: the lava stays behind as a source block, nearby air may catch fire. */
    public static void burnOut(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.6F);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.02);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(Blocks.LAVA.defaultBlockState()));
        BlockPos above = pos.above();
        if (level.isEmptyBlock(above)) {
            level.setBlockAndUpdate(above, BaseFireBlock.getState(level, above));
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (burns() && state.getValue(CONTENT) == Content.LAVA) {
            for (int i = 0; i < 3; i++) {
                level.addParticle(ParticleTypes.FLAME, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble() * 0.9,
                        pos.getZ() + random.nextDouble(), 0.0, 0.02, 0.0);
            }
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0.0, 0.05, 0.0);
        }
    }

    // ------------------------------------------------------------------ entities

    @Override
    protected VoxelShape getEntityInsideCollisionShape(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return state.getValue(CONTENT) == Content.EMPTY ? super.getEntityInsideCollisionShape(state, level, pos, entity) : FILLED[levelOf(state)];
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean precise) {
        switch (state.getValue(CONTENT)) {
            case LAVA -> {
                effects.apply(InsideBlockEffectType.CLEAR_FREEZE);
                effects.apply(InsideBlockEffectType.LAVA_IGNITE);
                effects.runAfter(InsideBlockEffectType.LAVA_IGNITE, Entity::lavaHurt);
            }
            case WATER, POWDER_SNOW -> {
                if (level instanceof ServerLevel server) {
                    BlockPos at = pos.immutable();
                    effects.runBefore(InsideBlockEffectType.EXTINGUISH, e -> {
                        if (e.isOnFire() && e.mayInteract(server, at)) lowerLevel(level.getBlockState(at), level, at);
                    });
                }
                effects.apply(InsideBlockEffectType.EXTINGUISH);
                if (state.getValue(CONTENT) == Content.POWDER_SNOW) effects.apply(InsideBlockEffectType.FREEZE);
            }
            default -> {
            }
        }
    }

    private void lowerLevel(BlockState state, Level level, BlockPos pos) {
        if (state.getBlock() != this || !state.getValue(CONTENT).layered()) return;
        int next = levelOf(state) - 1;
        BlockState after = next <= 0 ? with(Content.EMPTY, 3) : with(state.getValue(CONTENT), next);
        level.setBlockAndUpdate(pos, after);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(after));
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return state.getValue(CONTENT) == Content.EMPTY ? 0 : levelOf(state);
    }
}
