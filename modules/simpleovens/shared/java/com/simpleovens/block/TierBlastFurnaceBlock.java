package com.simpleovens.block;

import com.simpleovens.OvenTier;
import com.simpleovens.OvensRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BlastFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** A tiered BlastFurnace (Vanilla menu and recipes, faster per tier); all tiers share one block entity type. */
public class TierBlastFurnaceBlock extends BlastFurnaceBlock implements OvenBlock {
    private final OvenTier tier;

    public TierBlastFurnaceBlock(OvenTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public OvenTier tier() {
        return this.tier;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TierBlastFurnaceBlockEntity(pos, state);
    }

    /** An upgrade stance (hammer or axe in the main hand, the material in the off hand) does not open the menu. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult upgrade = OvenUse.useItemOn(state, level, pos, player, hand);
        return upgrade != null ? upgrade : super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    /**
     * A tier change within the family (the in-world upgrade, but also /setblock) keeps the block entity with its
     * contents and progress: all tiers share one block entity type.
     */
    @Override
    protected boolean shouldChangedStateKeepBlockEntity(BlockState oldState) {
        return oldState.getBlock() instanceof TierBlastFurnaceBlock;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        this.openContainer(level, pos, player);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void openContainer(Level level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof TierBlastFurnaceBlockEntity entity) player.openMenu((MenuProvider) entity);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, OvensRegistry.BLAST_FURNACE_BE, (w, pos, st, be) -> TierBlastFurnaceBlockEntity.tick((ServerLevel) w, pos, st, be));
    }
}
