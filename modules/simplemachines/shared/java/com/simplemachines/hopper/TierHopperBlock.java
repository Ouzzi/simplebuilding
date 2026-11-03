package com.simplemachines.hopper;

import com.simplelib.api.InWorldUpgrades;
import com.simplelib.api.SimpleLibApi;
import com.simplemachines.SimpleMachines;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * A mod hopper (SimpleBuilding's {@code ModHopperBlock}, stage 3). Vanilla's hopper block with the tier's block entity
 * ({@link TierHopperBlockEntity}) and menu; the hammer/axe upgrade click (SimpleLib in-world upgrades, files in
 * {@code data/simplemachines/in_world_upgrades}) passes the menu by.
 */
public class TierHopperBlock extends HopperBlock {
    private final HopperTier tier;

    public TierHopperBlock(HopperTier tier, Properties settings) {
        super(settings);
        this.tier = tier;
    }

    public HopperTier tier() {
        return tier;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TierHopperBlockEntity(pos, state);
    }

    /**
     * An upgrade stance (axe alone, or a hammer mod's sledgehammer with the material in the off hand) does not open the menu
     * (like Simple Ovens' {@code OvenUse}).
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (SimpleMachines.on()) {
            if (InWorldUpgrades.axeWants(state, player, hand)) {
                if (level instanceof ServerLevel server) InWorldUpgrades.axeStrike(server, pos, player);
                return InteractionResult.SUCCESS;
            }
            if (SimpleLibApi.toolWants(state, level, pos, player, hand)) return InteractionResult.PASS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    /**
     * A tier change within the family (the hammer upgrade, or {@code /setblock}) keeps the block entity with its contents and
     * filter: all tiers share one block entity type. Without this {@code LevelChunk#setBlockState} removes it and drops the items.
     */
    @Override
    protected boolean shouldChangedStateKeepBlockEntity(BlockState oldState) {
        return oldState.getBlock() instanceof TierHopperBlock;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof TierHopperBlockEntity hopper) {
            serverPlayer.openMenu(hopper);
            serverPlayer.awardStat(Stats.INSPECT_HOPPER);
        }
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, Hoppers.BLOCK_ENTITY, TierHopperBlockEntity::serverTick);
    }
}
