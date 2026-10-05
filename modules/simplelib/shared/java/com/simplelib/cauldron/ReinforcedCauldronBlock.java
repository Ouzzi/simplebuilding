package com.simplelib.cauldron;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.cauldron.CauldronInteraction;
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
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The reinforced cauldron (owner 30 / 56 B): holds water, lava, powder snow and - with a partner that
 * owns one - an extreme fluid (SimpleBuilding's soul lava, which a normal cauldron refuses). Always
 * a full bucket in, a full bucket out (no bottle levels). As a crucible heat source it counts like
 * its content's source block (owner F30): lava high, extreme fluid extreme.
 * Built in the world from a Vanilla cauldron: without SimpleBuilding with an axe and 4 diamonds,
 * with it by sledgehammer and 4 cracked diamonds.
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
    }

    public static final EnumProperty<Content> CONTENT = EnumProperty.create("content", Content.class);
    private static final VoxelShape FILLED = Shapes.or(AbstractCauldronBlock.SHAPE, Block.column(12.0, 4.0, 15.0));

    public ReinforcedCauldronBlock(Properties properties) {
        super(properties, new CauldronInteraction.Dispatcher());
        registerDefaultState(stateDefinition.any().setValue(CONTENT, Content.EMPTY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONTENT);
    }

    @Override
    public boolean isFull(BlockState state) {
        return state.getValue(CONTENT) != Content.EMPTY;
    }

    @Override
    protected double getContentHeight(BlockState state) {
        return isFull(state) ? 0.9375 : 0.0;
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
                    level.setBlockAndUpdate(pos, state.setValue(CONTENT, poured));
                    level.playSound(null, pos, pourSound(poured), SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
                }
                return InteractionResult.SUCCESS;
            }
            ItemStack filled = bucket.take(stack, content);
            if (filled == null) continue;
            if (!level.isClientSide()) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, filled));
                player.awardStat(Stats.USE_CAULDRON);
                level.setBlockAndUpdate(pos, state.setValue(CONTENT, Content.EMPTY));
                level.playSound(null, pos, takeSound(content), SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
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

    @Override
    protected VoxelShape getEntityInsideCollisionShape(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return state.getValue(CONTENT).hot() ? FILLED : super.getEntityInsideCollisionShape(state, level, pos, entity);
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
            case WATER -> {
                if (level instanceof ServerLevel && entity.isOnFire()) effects.apply(InsideBlockEffectType.EXTINGUISH);
            }
            case POWDER_SNOW -> effects.apply(InsideBlockEffectType.FREEZE);
            default -> {}
        }
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return isFull(state) ? 3 : 0;
    }
}
