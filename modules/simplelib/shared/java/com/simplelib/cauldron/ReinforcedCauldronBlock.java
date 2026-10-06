package com.simplelib.cauldron;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The reinforced cauldron (owner 30 / 56 B): a Vanilla cauldron that also holds an extreme fluid
 * (SimpleBuilding's soul lava, which a normal cauldron refuses). As a crucible heat source it counts
 * like its content's source block (owner F30): lava high, extreme fluid extreme.
 *
 * <p>It inherits the Vanilla cauldron (owner addition 11): its content is mapped onto the equivalent
 * Vanilla cauldron (empty, water with level, lava, powder snow with level) and the Vanilla interaction
 * table of that content runs - bottles, washing dyed items and banners, buckets, rain, dripstone and
 * whatever other mods add to those tables (milk from Simple Sandwiches). The block the interaction
 * leaves behind is mapped back; a partner block with a boolean property {@code reinforced} (the milk
 * cauldron) gets it set, so emptying it returns this cauldron. Partner containers (copper/Enderite
 * buckets, soul lava) come first through {@link ReinforcedCauldrons#BUCKETS}.
 * Built in the world from a Vanilla cauldron: without SimpleBuilding with an axe and 8 diamonds,
 * with it by sledgehammer and 8 cracked diamonds (both doubled 2026-10-06).
 */
public class ReinforcedCauldronBlock extends AbstractCauldronBlock {
    public enum Content implements StringRepresentable {
        EMPTY("empty"), WATER("water"), LAVA("lava"), POWDER_SNOW("powder_snow"), EXTREME("extreme");

        private final String name;

        Content(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public boolean hot() {
            return this == LAVA || this == EXTREME;
        }

        /** Water and powder snow fill in three levels like Vanilla; the rest is always full. */
        public boolean layered() {
            return this == WATER || this == POWDER_SNOW;
        }
    }

    public static final EnumProperty<Content> CONTENT = EnumProperty.create("content", Content.class);
    public static final IntegerProperty LEVEL = LayeredCauldronBlock.LEVEL;
    /** Name of the boolean property a partner cauldron block uses to remember the reinforced base. */
    public static final String REINFORCED_PROPERTY = "reinforced";
    private static final VoxelShape[] FILLED = new VoxelShape[4];

    static {
        for (int level = 1; level <= 3; level++) {
            FILLED[level] = Shapes.or(AbstractCauldronBlock.SHAPE, Block.column(12.0, 4.0, 6.0 + level * 3.0));
        }
    }

    public ReinforcedCauldronBlock(Properties properties) {
        super(properties, new CauldronInteraction.Dispatcher());
        registerDefaultState(stateDefinition.any().setValue(CONTENT, Content.EMPTY).setValue(LEVEL, 3));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONTENT, LEVEL);
    }

    /** {@code content} at {@code level} (only water and powder snow keep a level below 3). */
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
        if (content == Content.EMPTY) return 0.0;
        return content.layered() ? (6.0 + 3.0 * levelOf(state)) / 16.0 : 0.9375;
    }

    // ------------------------------------------------------------------ Vanilla mapping

    /** The Vanilla cauldron state equal to {@code state}, or null for the extreme content (no Vanilla equivalent). */
    public static @Nullable BlockState toVanilla(BlockState state) {
        return switch (state.getValue(CONTENT)) {
            case EMPTY -> Blocks.CAULDRON.defaultBlockState();
            case WATER -> Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, levelOf(state));
            case LAVA -> Blocks.LAVA_CAULDRON.defaultBlockState();
            case POWDER_SNOW -> Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, levelOf(state));
            case EXTREME -> null;
        };
    }

    /** The reinforced state equal to a Vanilla cauldron state, or null for anything else. */
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

    /**
     * After a Vanilla behaviour wrote its result at {@code pos}: a Vanilla cauldron becomes the reinforced
     * one again, a partner cauldron with a {@code reinforced} property remembers the reinforced base.
     */
    public void adopt(Level level, BlockPos pos) {
        BlockState now = level.getBlockState(pos);
        if (now.getBlock() == this) return;
        BlockState back = fromVanilla(now);
        if (back != null) {
            level.setBlockAndUpdate(pos, back);
            return;
        }
        Property<?> property = now.getBlock().getStateDefinition().getProperty(REINFORCED_PROPERTY);
        if (property instanceof BooleanProperty flag && !now.getValue(flag)) level.setBlockAndUpdate(pos, now.setValue(flag, true));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        Content content = state.getValue(CONTENT);
        for (ReinforcedCauldrons.Bucket bucket : ReinforcedCauldrons.BUCKETS) {
            if (content == Content.EMPTY) {
                Content poured = bucket.pours(stack);
                if (poured == null) continue;
                if (!level.isClientSide()) {
                    player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, bucket.afterPour(stack)));
                    player.awardStat(Stats.FILL_CAULDRON);
                    level.setBlockAndUpdate(pos, with(poured, 3));
                    level.playSound(null, pos, pourSound(poured), SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
                }
                return InteractionResult.SUCCESS;
            }
            if (!isFull(state)) break;
            ItemStack filled = bucket.take(stack, content);
            if (filled == null) continue;
            if (!level.isClientSide()) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, filled));
                player.awardStat(Stats.USE_CAULDRON);
                level.setBlockAndUpdate(pos, with(Content.EMPTY, 3));
                level.playSound(null, pos, takeSound(content), SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return InteractionResult.SUCCESS;
        }
        BlockState vanilla = toVanilla(state);
        if (vanilla == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        InteractionResult result = interactions(content).get(stack).interact(vanilla, level, pos, player, hand, stack);
        if (!level.isClientSide()) adopt(level, pos);
        return result;
    }

    private static SoundEvent pourSound(Content content) {
        return switch (content) {
            case LAVA, EXTREME -> SoundEvents.BUCKET_EMPTY_LAVA;
            case POWDER_SNOW -> SoundEvents.BUCKET_EMPTY_POWDER_SNOW;
            default -> SoundEvents.BUCKET_EMPTY;
        };
    }

    private static SoundEvent takeSound(Content content) {
        return switch (content) {
            case LAVA, EXTREME -> SoundEvents.BUCKET_FILL_LAVA;
            case POWDER_SNOW -> SoundEvents.BUCKET_FILL_POWDER_SNOW;
            default -> SoundEvents.BUCKET_FILL;
        };
    }

    /** Rain and snow fill it like the Vanilla cauldron. */
    @Override
    public void handlePrecipitation(BlockState state, Level level, BlockPos pos, Biome.Precipitation precipitation) {
        BlockState vanilla = toVanilla(state);
        if (vanilla == null) return;
        vanilla.getBlock().handlePrecipitation(vanilla, level, pos, precipitation);
        adopt(level, pos);
    }

    @Override
    protected boolean canReceiveStalactiteDrip(Fluid fluid) {
        return fluid == Fluids.WATER || fluid == Fluids.LAVA;
    }

    /** Dripstone: an empty cauldron takes water or lava, a water cauldron fills up (Vanilla rules). */
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
            case EXTREME -> {
                effects.apply(InsideBlockEffectType.CLEAR_FREEZE);
                effects.apply(InsideBlockEffectType.LAVA_IGNITE);
                effects.runAfter(InsideBlockEffectType.LAVA_IGNITE, e -> ReinforcedCauldrons.extremeInside.accept(level, e));
            }
            case WATER, POWDER_SNOW -> {
                // Vanilla: a burning entity is put out and uses up one level (powder snow melts to water first).
                if (level instanceof ServerLevel server) {
                    BlockPos at = pos.immutable();
                    effects.runBefore(InsideBlockEffectType.EXTINGUISH, e -> {
                        if (e.isOnFire() && e.mayInteract(server, at)) lowerLevel(level.getBlockState(at), level, at);
                    });
                }
                effects.apply(InsideBlockEffectType.EXTINGUISH);
                if (state.getValue(CONTENT) == Content.POWDER_SNOW) effects.apply(InsideBlockEffectType.FREEZE);
            }
            default -> {}
        }
    }

    private void lowerLevel(BlockState state, Level level, BlockPos pos) {
        if (state.getBlock() != this || !state.getValue(CONTENT).layered()) return;
        int next = levelOf(state) - 1;
        BlockState after = next <= 0 ? with(Content.EMPTY, 3) : with(Content.WATER, next);
        level.setBlockAndUpdate(pos, after);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(after));
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return state.getValue(CONTENT) == Content.EMPTY ? 0 : levelOf(state);
    }
}
