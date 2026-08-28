package com.simplebuilding.util;

import com.simplebuilding.items.custom.SledgehammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public final class SledgehammerUsageEvent {
    private static final Set<BlockPos> HARVESTED_BLOCKS = new HashSet<>();

    private SledgehammerUsageEvent() {
    }

    public static boolean handleBeforeBlockBreak(Level world, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        ItemStack mainHandItem = player.getMainHandItem();

        if (mainHandItem.getItem() instanceof SledgehammerItem && player instanceof ServerPlayer serverPlayer) {
            if (HARVESTED_BLOCKS.contains(pos)) {
                return true;
            }

            for (BlockPos position : SledgehammerItem.getBlocksToBeDestroyed(1, pos, serverPlayer)) {
                if (pos.equals(position)) {
                    continue;
                }

                BlockState targetState = world.getBlockState(position);

                if (!SledgehammerUtils.shouldBreak(world, position, pos, mainHandItem)) {
                    continue;
                }

                HARVESTED_BLOCKS.add(position);
                try {
                    boolean wasBroken = serverPlayer.gameMode.destroyBlock(position);
                    if (wasBroken) {
                        boolean isSuitable = mainHandItem.getItem().isCorrectToolForDrops(mainHandItem, targetState);
                        int damageAmount = isSuitable ? 1 : 2;
                        mainHandItem.hurtAndBreak(damageAmount, serverPlayer, EquipmentSlot.MAINHAND);
                        if (mainHandItem.isEmpty()) {
                            break;
                        }
                    }
                } finally {
                    HARVESTED_BLOCKS.remove(position);
                }
            }
        }

        return true;
    }
}
