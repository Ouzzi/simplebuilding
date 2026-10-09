package com.simplebuilding.dummy;

import com.simplebuilding.config.ServerTuning;
import com.simplebuilding.mixin.ArmorStandAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Ruestung tauschen (Besitzer 2026-10-08, docs/ai/PLAN-STAENDER-2026-10-09.md), wie das Vanilla-Regal mit der Hotbar:
 * Schleich-Rechtsklick mit leerer Haupthand, oder jeder Rechtsklick auf einen bestromten Ruestungsstaender, tauscht
 * Kopf, Brust, Beine und Fuesse mit den Ruestungsslots des Spielers. Gilt fuer alle Ruestungsstaender (Vanilla, Stroh,
 * Puppe, mittel, klein); Haken in {@code ArmorStandStandsMixin#interact}.
 */
public final class ArmorStandSwap {
    public static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private ArmorStandSwap() {
    }

    /** Ob dieser Rechtsklick die ganze Ruestung tauscht (statt Vanillas Einzelteil). */
    public static boolean wants(ArmorStand stand, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || stand.isMarker() || player.isSpectator()
                || !ServerTuning.get().features.armorStandSwap) {
            return false;
        }
        boolean sneakEmpty = player.isSecondaryUseActive() && player.getItemInHand(hand).isEmpty();
        return sneakEmpty || stand.level().hasNeighborSignal(stand.blockPosition());
    }

    /**
     * Tauscht jedes Ruestungsteil, das der Staender annehmen und der Spieler hergeben darf: gesperrte Slots des
     * Staenders bleiben, Fluch der Bindung bleibt am Spieler (ausser Kreativ), und ein Teil, das nicht in den
     * Spieler-Slot passt (z. B. ein Block auf dem Kopf), bleibt am Staender. Liefert die Zahl getauschter Slots.
     */
    public static int swap(ArmorStand stand, Player player) {
        int disabled = ((ArmorStandAccessor) stand).simplebuilding$disabledSlots();
        int swapped = 0;
        for (EquipmentSlot slot : ARMOR) {
            if ((disabled & (1 << slot.getFilterBit(0))) != 0) {
                continue;
            }
            ItemStack worn = player.getItemBySlot(slot);
            ItemStack shown = stand.getItemBySlot(slot);
            if (worn.isEmpty() && shown.isEmpty()) {
                continue;
            }
            if (!player.hasInfiniteMaterials() && EnchantmentHelper.has(worn, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
                continue;
            }
            if (!shown.isEmpty() && !player.isEquippableInSlot(shown, slot)) {
                continue;
            }
            if ((disabled & (1 << slot.getFilterBit(ArmorStand.DISABLE_TAKING_OFFSET))) != 0 && !shown.isEmpty()
                    || (disabled & (1 << slot.getFilterBit(ArmorStand.DISABLE_PUTTING_OFFSET))) != 0 && !worn.isEmpty()) {
                continue;
            }
            stand.setItemSlot(slot, worn.copy());
            player.setItemSlot(slot, shown.copy());
            swapped++;
        }
        if (swapped > 0 && stand.level() instanceof ServerLevel level) {
            level.playSound(null, stand.getX(), stand.getY(), stand.getZ(), SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
            stand.gameEvent(GameEvent.EQUIP, player);
        }
        return swapped;
    }
}
