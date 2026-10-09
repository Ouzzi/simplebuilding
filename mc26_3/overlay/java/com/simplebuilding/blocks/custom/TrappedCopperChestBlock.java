package com.simplebuilding.blocks.custom;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.blocks.entity.custom.TrappedCopperChestBlockEntity;
import com.simplebuilding.version.McVersion;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CopperChestBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Trapped copper chest (owner N16): a copper chest with Vanilla's trapped-chest signal (as strong as the number of
 * viewers, also strongly through the block below). It oxidizes like the copper chest (four stages, a double chest
 * takes the less oxidized stage of its halves), honeycomb waxes it and an axe scrapes wax or one stage off - by a
 * plain right-click, the way SimpleBuilding's copper pressure plate does, because Vanilla's waxing and weathering
 * tables are fixed. It pairs only with other trapped copper chests. Sounds are the copper chest's of its stage.
 */
public class TrappedCopperChestBlock extends ChestBlock implements WeatheringCopper {
    private final WeatheringCopper.WeatherState weatherState;
    private final boolean waxed;

    public TrappedCopperChestBlock(WeatheringCopper.WeatherState weatherState, boolean waxed, BlockBehaviour.Properties properties) {
        super(() -> ModBlockEntities.TRAPPED_COPPER_CHEST_BE, CopperChestBlock.getHingeSound(weatherState, true),
                CopperChestBlock.getHingeSound(weatherState, false), properties);
        this.weatherState = weatherState;
        this.waxed = waxed;
    }

    /** Unwaxed stages in oxidation order. */
    public static List<Block> stages() {
        return List.of(ModBlocks.TRAPPED_COPPER_CHEST, ModBlocks.EXPOSED_TRAPPED_COPPER_CHEST,
                ModBlocks.WEATHERED_TRAPPED_COPPER_CHEST, ModBlocks.OXIDIZED_TRAPPED_COPPER_CHEST);
    }

    /** Waxed stages in the same order. */
    public static List<Block> waxedStages() {
        return List.of(ModBlocks.WAXED_TRAPPED_COPPER_CHEST, ModBlocks.WAXED_EXPOSED_TRAPPED_COPPER_CHEST,
                ModBlocks.WAXED_WEATHERED_TRAPPED_COPPER_CHEST, ModBlocks.WAXED_OXIDIZED_TRAPPED_COPPER_CHEST);
    }

    private static Block block(int stage, boolean waxed) {
        return (waxed ? waxedStages() : stages()).get(stage);
    }

    @Override
    public WeatheringCopper.WeatherState getAge() {
        return weatherState;
    }

    public boolean isWaxed() {
        return waxed;
    }

    @Override
    public Optional<BlockState> getNext(BlockState state) {
        int index = weatherState.ordinal();
        return !waxed && index + 1 < 4 ? Optional.of(block(index + 1, false).withPropertiesOf(state)) : Optional.empty();
    }

    /** What {@code stack} does to this chest: honeycomb waxes, an axe scrapes wax or else one stage off; empty if nothing. */
    public Optional<BlockState> transformWith(BlockState state, ItemStack stack) {
        int index = weatherState.ordinal();
        if (stack.is(Items.HONEYCOMB)) {
            return waxed ? Optional.empty() : Optional.of(block(index, true).withPropertiesOf(state));
        }
        if (!stack.typeHolder().is(ItemTags.AXES)) {
            return Optional.empty();
        }
        if (waxed) {
            return Optional.of(block(index, false).withPropertiesOf(state));
        }
        return index > 0 ? Optional.of(block(index - 1, false).withPropertiesOf(state)) : Optional.empty();
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        Optional<BlockState> next = transformWith(state, stack);
        if (next.isEmpty() || !com.simplebuilding.util.TransformTargets.mayTransform(level, player, pos, hitResult.getDirection(), stack)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }
        if (!level.isClientSide()) {
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, stack);
            }
            level.setBlock(pos, next.get(), Block.UPDATE_ALL_IMMEDIATE);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, next.get()));
            if (stack.is(Items.HONEYCOMB)) {
                McVersion.waxOnEffects(level, pos);
                stack.consume(1, player);
            } else {
                boolean waxOff = waxed;
                level.playSound(null, pos, waxOff ? SoundEvents.AXE_WAX_OFF : SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1.0f, 1.0f);
                level.levelEvent(null, waxOff ? LevelEvent.PARTICLES_WAX_OFF : LevelEvent.PARTICLES_SCRAPE, pos, 0);
                stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return !waxed && weatherState != WeatheringCopper.WeatherState.OXIDIZED;
    }

    /** Like Vanilla's copper chest: one half ages for both (the other follows in updateShape), never while open. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!waxed && state.getValue(TYPE) != ChestType.RIGHT
                && level.getBlockEntity(pos) instanceof ChestBlockEntity chest && chest.getEntitiesWithContainerOpen().isEmpty()) {
            changeOverTime(state, level, pos, random);
        }
    }

    @Override
    public boolean chestCanConnectTo(BlockState state) {
        return state.getBlock() instanceof TrappedCopperChestBlock && state.hasProperty(TYPE);
    }

    /** A new half joins at the less oxidized stage of the pair; mixed wax loses it (like Vanilla's copper chest). */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null || state.getValue(TYPE) == ChestType.SINGLE) return state;
        BlockState other = context.getLevel().getBlockState(context.getClickedPos().relative(getConnectedDirection(state)));
        if (!(other.getBlock() instanceof TrappedCopperChestBlock partner)) return state;
        int stage = Math.min(weatherState.ordinal(), partner.weatherState.ordinal());
        return block(stage, waxed && partner.waxed).withPropertiesOf(state);
    }

    /** Both halves stay the same block: a changed partner (aged, waxed, scraped) takes this half along. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        BlockState updated = super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
        if (chestCanConnectTo(neighbourState) && updated.getValue(TYPE) != ChestType.SINGLE
                && getConnectedDirection(updated) == directionToNeighbour) {
            return neighbourState.getBlock().withPropertiesOf(updated);
        }
        return updated;
    }

    @Override
    public boolean shouldChangedStateKeepBlockEntity(BlockState oldState) {
        return oldState.getBlock() instanceof TrappedCopperChestBlock;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrappedCopperChestBlockEntity(pos, state);
    }

    @Override
    protected Stat<Identifier> getOpenChestStat() {
        return Stats.CUSTOM.get(Stats.TRIGGER_TRAPPED_CHEST);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int ownSignal(BlockState state, BlockGetter level, BlockPos pos) {
        return Mth.clamp(ChestBlockEntity.getOpenCount(level, pos), 0, 15);
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return direction == Direction.UP ? state.getSignal(level, pos, direction) : 0;
    }
}
