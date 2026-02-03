package com.simplebuilding.util;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.SledgehammerItem;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class SledgehammerUsageEvent implements PlayerBlockBreakEvents.Before {

    private static final Set<BlockPos> HARVESTED_BLOCKS = new HashSet<>();

    @Override
    public boolean beforeBlockBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        ItemStack mainHandItem = player.getMainHandStack();

        if (mainHandItem.getItem() instanceof SledgehammerItem && player instanceof ServerPlayerEntity serverPlayer) {
            if (HARVESTED_BLOCKS.contains(pos)) {return true;}

            var registry = world.getRegistryManager();
            var enchantLookup = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
            var ignoreTypeKey = enchantLookup.getOptional(ModEnchantments.OVERRIDE);

            int ignoreLevel = 0;
            if (ignoreTypeKey.isPresent()) {ignoreLevel = EnchantmentHelper.getLevel(ignoreTypeKey.get(), mainHandItem);}

            BlockState originState = world.getBlockState(pos);

            for (BlockPos position : SledgehammerItem.getBlocksToBeDestroyed(1, pos, serverPlayer)) {
                if (pos.equals(position)) continue;

                BlockState targetState = world.getBlockState(position);

                if (targetState.isAir() || targetState.getHardness(world, position) < 0) {continue;}

                boolean shouldBreak = false;

                if (ignoreLevel == 0) {
                    if (targetState.getBlock() == originState.getBlock()) {shouldBreak = true;}
                }
                else if (ignoreLevel == 1) {
                    if (targetState.getBlock() == originState.getBlock() || mainHandItem.getItem().isCorrectForDrops(mainHandItem, targetState)) {shouldBreak = true;}
                }
                else if (ignoreLevel >= 2) {
                    if (mainHandItem.getItem().isCorrectForDrops(mainHandItem, targetState) || targetState.getBlock() == originState.getBlock()) {
                        shouldBreak = true;
                    }
                }

                if (shouldBreak) {
                    HARVESTED_BLOCKS.add(position);

                    boolean wasBroken = serverPlayer.interactionManager.tryBreakBlock(position);

                    HARVESTED_BLOCKS.remove(position);

                    if (wasBroken) {
                        boolean isSuitable = mainHandItem.getItem().isCorrectForDrops(mainHandItem, targetState);
                        int damageAmount = isSuitable ? 1 : 2;

                        mainHandItem.damage(damageAmount, serverPlayer, EquipmentSlot.MAINHAND);

                        if (mainHandItem.isEmpty()) {break;}
                    }
                }
            }
        }

        return true;
    }
}