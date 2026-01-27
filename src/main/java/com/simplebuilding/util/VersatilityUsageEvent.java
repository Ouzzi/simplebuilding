package com.simplebuilding.util;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.ChiselItem;
import com.simplebuilding.items.custom.SledgehammerItem;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
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

        // 1. Bewerte das aktuelle Werkzeug
        float currentScore = getToolScore(currentStack, state);

        int bestSlot = -1;
        float bestScore = currentScore;

        PlayerInventory inv = player.getInventory();

        // Level 1: Nur Hotbar (0-8)
        // Level 2: Gesamtes Inventar (0-35)
        int searchRange = (level >= 2) ? 36 : 9;

        for (int i = 0; i < searchRange; i++) {
            if (i == inv.getSelectedSlot()) continue;

            ItemStack stackInSlot = inv.getStack(i);
            if (stackInSlot.isEmpty()) continue;

            // 2. Bewerte jedes Item im Inventar
            float score = getToolScore(stackInSlot, state);

            // Wenn das Item im Inventar besser ist als das aktuelle Beste -> Merken
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        // Wenn ein besseres Werkzeug gefunden wurde -> Tauschen
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
                }
            }
        }

        return ActionResult.PASS;
    }

    /**
     * Berechnet einen Score für ein Werkzeug basierend auf Effektivität und Typ-Präferenz.
     * Score = Speed + Bonus
     */
    private float getToolScore(ItemStack stack, BlockState state) {
        if (stack.isEmpty()) return -1f;

        // Wenn das Item den Block nicht abbauen kann (keine Drops/falsches Tool), ist es nutzlos.
        if (!stack.isSuitableFor(state)) return -1f;

        float speed = stack.getMiningSpeedMultiplier(state);
        // Fallback: Manchmal ist speed 1.0 trotz suitability, wir nehmen den Speed als Basis.
        float score = speed;

        // --- PRIORITÄTEN SYSTEM ---

        // 1. Chisel (Meißel): Niedrigste Priorität (Bonus 0).
        // WICHTIG: Zuerst prüfen, da Chisel oft auch im Pickaxe-Tag ist.
        if (stack.getItem() instanceof ChiselItem) {
            score += 0f;
        }
        // 2. Sledgehammer: Höchste Priorität für Stein (Bonus 2000).
        else if (stack.getItem() instanceof SledgehammerItem) {
            score += 2000f;
        }
        // 3. Standard-Werkzeuge (Spitzhacke, Axt, Schaufel): Hohe Priorität (Bonus 1000).
        else if (stack.isIn(ItemTags.PICKAXES) ||
                 stack.isIn(ItemTags.AXES) ||
                 stack.isIn(ItemTags.SHOVELS)) {
            score += 1000f;
        }
        // 4. Sonstige geeignete Items (z.B. Schere, Schwert): Mittlere Priorität.
        else {
            score += 500f;
        }

        return score;
    }
}