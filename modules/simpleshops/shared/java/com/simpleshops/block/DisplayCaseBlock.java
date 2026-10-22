package com.simpleshops.block;

import com.simpleshops.logic.ShopActions;
import com.simpleshops.logic.ShopGuard;
import com.simpleshops.logic.ShopLedger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** The display case (and its glow variant): holds the goods of a shop. Placeable anywhere; part of a shop (protected, selling) only on a sales block of the same owner (SP22-7). */
public class DisplayCaseBlock extends BaseEntityBlock {
    /** Wooden base (2 px) and the glass box above it. */
    private static final VoxelShape SHAPE = Shapes.or(net.minecraft.world.level.block.Block.box(0, 0, 0, 16, 2, 16),
            net.minecraft.world.level.block.Block.box(1, 2, 1, 15, 15, 15));
    private final boolean glow;

    public DisplayCaseBlock(BlockBehaviour.Properties properties, boolean glow) {
        super(properties);
        this.glow = glow;
    }

    /** The glow variant draws its goods fully bright (and gives off weak light). */
    public boolean glow() {
        return this.glow;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return ShopActions.mayPlace(context, false) ? defaultBlockState() : null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DisplayCaseBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level instanceof ServerLevel server && placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof DisplayCaseBlockEntity be) {
            be.initFor(player);
            ShopLedger.record(server, pos, player.getUUID(), false);
        }
    }

    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.@Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
            net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, com.simpleshops.ShopsRegistry.CASE_BE, DisplayCaseBlockEntity::tick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) return ShopActions.quickStock(sp, server, pos, hand);
        if (!stack.isEmpty() && level.getBlockEntity(pos) instanceof DisplayCaseBlockEntity be && be.isOwner(player.getUUID())) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) return ShopActions.useCase(sp, server, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level instanceof Level l && !ShopGuard.mayBreak(player, l, pos)) return 0.0F;
        return super.getDestroyProgress(state, player, level, pos);
    }
}
