package com.simplebuilding.modules.simplelayers.block;

import com.simplebuilding.modules.simplelayers.LayersRegistry;
import com.simplebuilding.modules.simplelayers.world.BackroomsTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Exit Door (concept §2.1 "Ausgang"): only in the exit room of the Backrooms (no item). Using it leads back to the Overworld
 * surface above the place one came in; with a Backdoor Key in hand the key is used up and the door opens far away instead
 * (1500-3000 blocks, a travel shortcut as the reward). Two blocks high ({@code half}).
 */
public class ExitDoorBlock extends BackroomBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    public ExitDoorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(LayersRegistry.BACKDOOR_KEY)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (player instanceof ServerPlayer sp && BackroomsTravel.exit(sp, true) && !player.isCreative()) stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) BackroomsTravel.exit(sp, false);
        return InteractionResult.SUCCESS;
    }
}
