package com.simplebuilding.modules.simplelayers.block;

import com.simplelib.api.portal.PortalDoorBlock;
import com.simplelib.api.portal.PortalShape;
import com.simplelib.api.portal.PortalTransit;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Maintenance box (BACK22 core §5.3, owner fg16 a): sits in the frame of every door that can be dormant or broken; used once, it
 * activates or repairs its door directly (no item comes out, it does not refill). Telegraphs its state: {@code used} shows the empty
 * box (A14). Only generated, unbreakable.
 */
public class MaintenanceBoxBlock extends Block {
    public static final BooleanProperty USED = BooleanProperty.create("used");

    public MaintenanceBoxBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(USED, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(USED);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (state.getValue(USED)) {
            if (player instanceof ServerPlayer sp) sp.sendOverlayMessage(Component.translatable("message.simplelayers.box_empty"));
            return InteractionResult.SUCCESS;
        }
        PortalShape door = doorNear(server, pos);
        if (door != null && PortalTransit.mend(server, door, player)) {
            level.setBlock(pos, state.setValue(USED, Boolean.TRUE), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.8F, 1.0F);
        } else if (player instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(Component.translatable("message.simplelayers.box_not_needed"));
        }
        return InteractionResult.SUCCESS;
    }

    /** The portal door next to the box (the box is its upper left frame block: the door is right of it, one block lower or level). */
    public static @Nullable PortalShape doorNear(ServerLevel level, BlockPos box) {
        for (BlockPos p : BlockPos.betweenClosed(box.offset(-1, -1, -1), box.offset(1, 0, 1))) {
            if (level.getBlockState(p).getBlock() instanceof PortalDoorBlock) return PortalShape.of(level, p);
        }
        return null;
    }
}
