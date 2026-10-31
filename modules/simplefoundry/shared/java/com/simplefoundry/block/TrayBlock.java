package com.simplefoundry.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** The casting tray: takes up to nine ingots of one metal from a spout; {@link #CONTENT} (0 empty, else metal ordinal + 1) shows the metal. Empty hand takes the set ingots. */
public class TrayBlock extends BaseEntityBlock {
    public static final IntegerProperty CONTENT = IntegerProperty.create("content", 0, 3);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 5, 15);

    public TrayBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CONTENT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONTENT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrayBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof TrayBlockEntity tray) {
            if (tray.count() == 0) return InteractionResult.PASS;
            if (!tray.take(level, pos)) {
                player.sendSystemMessage(Component.translatable("simplefoundry.tray.hot"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Breaking the tray gives its ingots back, hot or not. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TrayBlockEntity tray) tray.drop(level, pos);
        return super.playerWillDestroy(level, pos, state, player);
    }
}
