package com.simplebuilding.util;

import com.simplebuilding.enchantment.ModEnchantments;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class StripMinerUsageEvent implements PlayerBlockBreakEvents.Before {

    // Verhindert, dass das Event durch sich selbst (tryBreakBlock) rekursiv ausgelöst wird
    private static final Set<BlockPos> MINING_BLOCKS = new HashSet<>();

    @Override
    public boolean beforeBlockBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        ItemStack stack = player.getMainHandStack();

        if (!(player instanceof ServerPlayerEntity serverPlayer) || !stack.isIn(ItemTags.PICKAXES)) {return true;}

        if (MINING_BLOCKS.contains(pos)) {return true;}

        if (!stack.getItem().isCorrectForDrops(stack, state)) {return true;}

        var registry = world.getRegistryManager();
        var enchantLookup = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var stripMinerKey = enchantLookup.getOptional(ModEnchantments.STRIP_MINER);

        if (stripMinerKey.isEmpty()) return true;

        int level = EnchantmentHelper.getLevel(stripMinerKey.get(), stack);
        if (level <= 0) return true;

        // --- Logik Start ---
        int depth = (level == 3) ? 4 : level;

        Direction miningDirection = getMiningDirection(player);

        int brokenBlocks = 0;
        for (int i = 1; i <= depth; i++) {
            BlockPos targetPos = pos.offset(miningDirection, i);
            BlockState targetState = world.getBlockState(targetPos);

            if (targetState.isAir() || targetState.getHardness(world, targetPos) < 0) {break;}
            if (!stack.getItem().isCorrectForDrops(stack, targetState)) {break;}

            MINING_BLOCKS.add(targetPos);
            boolean broken = serverPlayer.interactionManager.tryBreakBlock(targetPos);
            MINING_BLOCKS.remove(targetPos);

            if (broken) {
                brokenBlocks++;
                if (stack.isEmpty()) {break;}
            }
        }

        if (brokenBlocks > 0) {
            // Beispiel: Wir wollen ca. 33% Durability sparen (nur 66% Schaden nehmen).
            // Formel: Wir berechnen den Rabatt.
            // (brokenBlocks + 1) / 3 sorgt dafür, dass bei 2 Extra-Blöcken 1 Schaden geheilt wird.
            int damageRefund = (brokenBlocks + 1) / 3;

            if (damageRefund > 0) {
                // Wir "heilen" das Item, indem wir den Damage-Wert verringern.
                int currentDamage = stack.getDamage();
                // Sicherstellen, dass wir nicht unter 0 gehen (Item reparieren über Max hinaus)
                stack.setDamage(Math.max(0, currentDamage - damageRefund));
            }
        }

        return true;
    }

    private Direction getMiningDirection(PlayerEntity player) {
        float pitch = player.getPitch();
        if (pitch < -60) return Direction.UP;
        if (pitch > 60) return Direction.DOWN;
        return player.getHorizontalFacing();
    }
}