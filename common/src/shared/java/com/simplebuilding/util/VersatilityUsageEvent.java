package com.simplebuilding.util;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.ChiselItem;
import com.simplebuilding.items.custom.SledgehammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class VersatilityUsageEvent {

    private VersatilityUsageEvent() {
    }

    public static InteractionResult handleAttackBlock(Player player, Level world, InteractionHand hand, BlockPos pos, Direction direction) {
        // Nur Server-seitig und nur wenn geschlichen wird
        if (world.isClientSide() || !player.isShiftKeyDown()) return InteractionResult.PASS;

        ItemStack currentStack = player.getMainHandItem();

        // Hat das Item das Enchantment?
        var registry = world.registryAccess();
        var enchantLookup = registry.lookupOrThrow(Registries.ENCHANTMENT);
        var versatilityKey = enchantLookup.get(ModEnchantments.VERSATILITY);

        if (versatilityKey.isEmpty()) return InteractionResult.PASS;

        int level = EnchantmentHelper.getItemEnchantmentLevel(versatilityKey.get(), currentStack);
        if (level <= 0) return InteractionResult.PASS;

        BlockState state = world.getBlockState(pos);

        // 1. Bewerte das aktuelle Werkzeug
        float currentScore = getToolScore(currentStack, state);

        int bestSlot = -1;
        float bestScore = currentScore;

        Inventory inv = player.getInventory();

        // Level 1: Nur Hotbar (0-8)
        // Level 2: Gesamtes Inventar (0-35)
        int searchRange = (level >= 2) ? 36 : 9;

        for (int i = 0; i < searchRange; i++) {
            if (i == inv.getSelectedSlot()) continue;

            ItemStack stackInSlot = inv.getItem(i);
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
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.connection.send(new ClientboundSetHeldSlotPacket(bestSlot));
                }
            }
            // Fall 2: Das bessere Item ist im Inventar (9-35) -> Nur bei Level 2 möglich
            else {
                int currentSlot = inv.getSelectedSlot();
                ItemStack stackInHand = inv.getItem(currentSlot);
                ItemStack stackInStorage = inv.getItem(bestSlot);

                // Items tauschen
                inv.setItem(currentSlot, stackInStorage);
                inv.setItem(bestSlot, stackInHand);

                // Inventar-Updates an den Client senden
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.containerMenu.broadcastChanges();
                }
            }
        }

        return InteractionResult.PASS;
    }

    /**
     * Berechnet einen Score für ein Werkzeug basierend auf Effektivität und Typ-Präferenz.
     * Score = Speed + Bonus
     */
    private static float getToolScore(ItemStack stack, BlockState state) {
        if (stack.isEmpty()) return -1f;

        // Wenn das Item den Block nicht abbauen kann (keine Drops/falsches Tool), ist es nutzlos.
        if (!stack.isCorrectToolForDrops(state)) return -1f;

        float speed = stack.getDestroySpeed(state);
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
        else if (stack.is(ItemTags.PICKAXES) ||
                 stack.is(ItemTags.AXES) ||
                 stack.is(ItemTags.SHOVELS)) {
            score += 1000f;
        }
        // 4. Sonstige geeignete Items (z.B. Schere, Schwert): Mittlere Priorität.
        else {
            score += 500f;
        }

        return score;
    }
}