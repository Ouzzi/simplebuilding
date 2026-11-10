package com.simplebuilding.modules.simplelayers.vault.block;

import com.simplebuilding.modules.simplelayers.vault.VaultPuzzles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Seal Socket (UNDER21): one of the Ascent Altar's three settings; takes the matching seal part of this vault ({@link VaultPuzzles#insert}). */
public class SealSocketBlock extends Block {
    public static final EnumProperty<SealPart> PART = SealPedestalBlock.PART;
    public static final BooleanProperty FILLED = BooleanProperty.create("filled");
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 10, 13);

    public SealSocketBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, SealPart.LIGHT).setValue(FILLED, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, FILLED);
    }

    public static int light(BlockState state) {
        return state.getValue(FILLED) ? 12 : 0;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level instanceof ServerLevel server) VaultPuzzles.insert(server, pos, state, player, stack);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) VaultPuzzles.insert(server, pos, state, player, ItemStack.EMPTY);
        return InteractionResult.SUCCESS;
    }
}
