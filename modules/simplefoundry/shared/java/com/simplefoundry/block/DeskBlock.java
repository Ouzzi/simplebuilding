package com.simplefoundry.block;

import com.simplefoundry.FoundryRegistry;
import com.simplefoundry.FoundryScan;
import com.simplefoundry.Fuels;
import com.simplefoundry.Melts;
import com.simplefoundry.Metal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The foundry desk (control block): right click with fuel or an ore/ingot throws it in, an empty hand reads the status. The structure
 * around it is checked on use and periodically while the foundry works ({@link DeskBlockEntity}).
 */
public class DeskBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Fuel gauge on the front: 0 empty to 4 full (owner 02.11.). */
    public static final IntegerProperty FUEL = IntegerProperty.create("fuel", 0, 4);

    public DeskBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FUEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FUEL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DeskBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, FoundryRegistry.DESK_BE, (w, pos, st, be) -> DeskBlockEntity.tick((net.minecraft.server.level.ServerLevel) w, pos, st, be));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof DeskBlockEntity upgradable && upgradable.tier().next() != null && stack.is(upgradable.tier().next().upgradeItem())) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (upgradable.upgrade(stack) && !player.hasInfiniteMaterials()) stack.shrink(1);
            player.sendSystemMessage(Component.translatable("simplefoundry.desk.tier", upgradable.tier().slots));
            return InteractionResult.SUCCESS;
        }
        boolean fuel = Fuels.of(stack) != null;
        if (!fuel && Melts.of(stack) == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof DeskBlockEntity desk)) return InteractionResult.PASS;
        int taken = fuel ? desk.addFuel(stack) : desk.insert(stack);
        if (taken > 0 && fuel && stack.is(Items.LAVA_BUCKET)) {
            // lava is the main fuel: the bucket pours into the tank and comes back empty
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
            return InteractionResult.SUCCESS;
        }
        if (taken > 0 && !player.hasInfiniteMaterials()) stack.shrink(taken);
        if (taken == 0) player.sendSystemMessage(Component.translatable(fuel ? "simplefoundry.desk.fuel_full" : "simplefoundry.desk.refused", status(desk)));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof DeskBlockEntity desk) {
            desk.revalidate();
            if (player.isShiftKeyDown()) {
                // owner SA4: the arrow of the desk moves the selected layer one down
                int layer = desk.cycleLevel();
                Metal metal = desk.selectedMetal();
                player.sendSystemMessage(Component.translatable("simplefoundry.desk.layer", layer + 1, metal == null ? Component.translatable("simplefoundry.desk.layer_empty") : Component.translatable("simplefoundry.metal." + metal.id)));
                return InteractionResult.SUCCESS;
            }
            // FOUNDRY32-UI = A: the desk window (slots left, section in the middle, stack right)
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new FoundryMenu(id, inventory, desk), Component.translatable("block.simplefoundry.foundry_desk")));
        }
        return InteractionResult.SUCCESS;
    }

    private static Component status(DeskBlockEntity desk) {
        FoundryScan.Result r = desk.structure();
        return Component.translatable("simplefoundry.status." + (r == null ? "no_chamber" : r.reason()));
    }

    /** Breaking the desk gives the fuel back. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DeskBlockEntity desk) desk.dropContents(level, pos);
        return super.playerWillDestroy(level, pos, state, player);
    }
}
