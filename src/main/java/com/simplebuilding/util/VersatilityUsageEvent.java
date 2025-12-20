package com.simplebuilding.util;

import com.simplebuilding.enchantment.ModEnchantments;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class VersatilityUsageEvent implements AttackBlockCallback {

    @Override
    public ActionResult interact(PlayerEntity player, World world, Hand hand, BlockPos pos, Direction direction) {
        // Nur Server-seitig und nur wenn geschlichen wird
        if (world.isClient() || !player.isSneaking()) return ActionResult.PASS;

        ItemStack currentStack = player.getMainHandStack();

        // Hat das Item das Enchantment?
        var registry = world.getRegistryManager();
        var enchantLookup = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var versatilityKey = enchantLookup.getOptional(ModEnchantments.VERSATILITY);

        if (versatilityKey.isEmpty()) return ActionResult.PASS;

        int level = EnchantmentHelper.getLevel(versatilityKey.get(), currentStack);
        if (level <= 0) return ActionResult.PASS;

        BlockState state = world.getBlockState(pos);

        float currentSpeed = currentStack.getMiningSpeedMultiplier(state);
        if (!currentStack.isSuitableFor(state)) currentSpeed = 0;

        int bestSlot = -1;
        float bestSpeed = currentSpeed;

        PlayerInventory inv = player.getInventory();

        // Level 1: Nur Hotbar (0-8)
        // Level 2: Gesamtes Inventar (0-35)
        int searchRange = (level >= 2) ? 36 : 9;

        for (int i = 0; i < searchRange; i++) {
            if (i == inv.getSelectedSlot()) continue;

            ItemStack stackInSlot = inv.getStack(i);
            if (stackInSlot.isEmpty()) continue;

            float speed = stackInSlot.getMiningSpeedMultiplier(state);
            boolean correctDrops = stackInSlot.isSuitableFor(state);

            if (correctDrops && speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }

        // Wenn ein besseres Werkzeug gefunden wurde
        if (bestSlot != -1) {

            // Fall 1: Das bessere Item ist in der Hotbar (0-8)
            if (bestSlot < 9) {
                inv.setSelectedSlot(bestSlot);

                // Client über den Slot-Wechsel informieren
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    serverPlayer.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(bestSlot));
                }
            }
            // Fall 2: Das bessere Item ist im Inventar (9-35) -> Nur bei Level 2 möglich
            else {
                int currentSlot = inv.getSelectedSlot();
                ItemStack stackInHand = inv.getStack(currentSlot);
                ItemStack stackInStorage = inv.getStack(bestSlot);

                // Items tauschen
                inv.setStack(currentSlot, stackInStorage);
                inv.setStack(bestSlot, stackInHand);

                // Inventar-Updates an den Client senden
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    serverPlayer.currentScreenHandler.sendContentUpdates();
                    // Optional: Explizites Senden der Slot-Updates, falls sendContentUpdates verzögert
                    // serverPlayer.onContentChanged(serverPlayer.currentScreenHandler);
                }
            }
        }

        return ActionResult.PASS;
    }
}