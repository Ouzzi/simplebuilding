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

            BlockState originState = world.getBlockState(pos);
            int overrideLevel = com.simplebuilding.util.EnchantmentHelper.getEnchantmentLevel(mainHandItem, world,
                    com.simplebuilding.enchantment.ModEnchantments.OVERRIDE);
            for (BlockPos position : SledgehammerItem.getBlocksToBeDestroyed(1, pos, serverPlayer)) {
                if (pos.equals(position)) {
                    continue;
                }

                BlockState targetState = world.getBlockState(position);

                if (!SledgehammerUtils.shouldBreak(world, position, originState, mainHandItem, overrideLevel)) {
                    continue;
                }
                // Vanilla-Spawnschutz und Weltgrenze: destroyBlock prueft beides nicht.
                if (!world.mayInteract(serverPlayer, position)) {
                    continue;
                }

                HARVESTED_BLOCKS.add(position);
                try {
                    boolean wasBroken = serverPlayer.gameMode.destroyBlock(position);
                    if (wasBroken) {
                        com.simplebuilding.advancement.ModCounters.add(serverPlayer, com.simplebuilding.advancement.ModCounters.HAMMER_BLOCKS, 1);
                        // Die Grundabnutzung (SledgehammerItem#WEAR_PER_BLOCK) hat destroyBlock ueber
                        // mineBlock schon abgezogen; das falsche Werkzeug kostet einen Punkt mehr.
                        boolean isSuitable = mainHandItem.getItem().isCorrectToolForDrops(mainHandItem, targetState);
                        if (!isSuitable && !mainHandItem.isEmpty()) {
                            mainHandItem.hurtAndBreak(1, serverPlayer, EquipmentSlot.MAINHAND);
                        }
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
