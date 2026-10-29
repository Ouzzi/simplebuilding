package com.simpleblocks.saw;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Shapecutter: the stonecutter's shape and facing, its own menu ({@link ShapecutterMenu}). {@link #METAL} (BS25-3) is
 * the finish: wood by default, plates on top and bottom after the conversion with 2 heavy pressure plates
 * ({@link SawUpgrade#tryMetal}); it changes the look only.
 */
public class ShapecutterBlock extends StonecutterBlock {
    public static final BooleanProperty METAL = BooleanProperty.create("metal");
    private static final Component TITLE = Component.translatable("container.simpleblocks.shapecutter");

    public ShapecutterBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(METAL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(METAL);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) player.openMenu(state.getMenuProvider(level, pos));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inventory, player) -> new ShapecutterMenu(id, inventory, ContainerLevelAccess.create(level, pos)), TITLE);
    }
}
