package com.simplebuilding.util;

import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public final class StripMinerUsageEvent {

    // Verhindert, dass das Event durch sich selbst (tryBreakBlock) rekursiv ausgelöst wird
    private static final Set<BlockPos> MINING_BLOCKS = new HashSet<>();

    private StripMinerUsageEvent() {
    }

    public static boolean handleBeforeBlockBreak(Level world, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        // --- ÄNDERUNG: Nur ausführen, wenn Spieler sneakt ---
        if (!player.isShiftKeyDown()) {
            return true;
        }

        ItemStack stack = player.getMainHandItem();

        if (!(player instanceof ServerPlayer serverPlayer) || !stack.is(ItemTags.PICKAXES)) {return true;}

        if (MINING_BLOCKS.contains(pos)) {return true;}

        if (!stack.getItem().isCorrectToolForDrops(stack, state)) {return true;}

        var registry = world.registryAccess();
        var enchantLookup = registry.lookupOrThrow(Registries.ENCHANTMENT);
        var stripMinerKey = enchantLookup.get(ModEnchantments.STRIP_MINER);

        if (stripMinerKey.isEmpty()) return true;

        int level = EnchantmentHelper.getItemEnchantmentLevel(stripMinerKey.get(), stack);
        if (level <= 0) return true;

        // --- Logik Start ---
        int depth = (level == 3) ? 4 : level;

        Direction miningDirection = getMiningDirection(player);

        int brokenBlocks = 0;
        for (int i = 1; i <= depth; i++) {
            BlockPos targetPos = pos.relative(miningDirection, i);
            BlockState targetState = world.getBlockState(targetPos);

            if (targetState.isAir() || targetState.getDestroySpeed(world, targetPos) < 0) {break;}
            if (!stack.getItem().isCorrectToolForDrops(stack, targetState)) {break;}

            MINING_BLOCKS.add(targetPos);
            boolean broken = serverPlayer.gameMode.destroyBlock(targetPos);
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
                int currentDamage = stack.getDamageValue();
                // Sicherstellen, dass wir nicht unter 0 gehen (Item reparieren über Max hinaus)
                stack.setDamageValue(Math.max(0, currentDamage - damageRefund));
            }
        }

        return true;
    }

    private static Direction getMiningDirection(Player player) {
        float pitch = player.getXRot();
        if (pitch < -60) return Direction.UP;
        if (pitch > 60) return Direction.DOWN;
        return player.getDirection();
    }
}