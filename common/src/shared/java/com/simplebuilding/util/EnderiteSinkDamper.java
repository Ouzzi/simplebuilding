package com.simplebuilding.util;

import com.simplebuilding.items.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Sinkdaempfer der Enderit-Ruestung (Besitzer 2026-10-02, ersetzt den Gleitflug mit Springen): wer im Fall schleicht,
 * faellt mit gedaempfter Beschleunigung - die Schwerkraft sinkt je getragenem Teil, die Fallgeschwindigkeit wird nicht
 * abrupt gebremst - und nimmt im selben Mass weniger Fallschaden. Laeuft auf Client und Server gleich (Bewegung des
 * eigenen Spielers rechnet der Client), deshalb nur aus Ruestung und Schleich-Zustand, ohne Effekte oder Pakete.
 */
public final class EnderiteSinkDamper {
    /** Daempfung je Anzahl getragener Enderit-Teile (Index = Teile): Anteil der Schwerkraft und des Fallwegs, der wegfaellt. */
    public static final double[] DAMPING = {0.0, 0.25, 0.45, 0.65, 0.8};

    private EnderiteSinkDamper() {
    }

    public static int pieces(Player player) {
        int count = 0;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
            if (isEnderiteArmor(player.getItemBySlot(slot))) {
                count++;
            }
        }
        return count;
    }

    public static boolean isEnderiteArmor(ItemStack stack) {
        return stack.is(ModItems.ENDERITE_BOOTS) || stack.is(ModItems.ENDERITE_LEGGINGS)
                || stack.is(ModItems.ENDERITE_CHESTPLATE) || stack.is(ModItems.ENDERITE_HELMET);
    }

    /** Daempfung jetzt: 0, wenn der Spieler nicht schleichend faellt (Boden, Elytra, Fliegen, Wasser, Steigen). */
    public static double damping(Player player) {
        if (!player.isShiftKeyDown() || player.onGround() || player.isFallFlying() || player.getAbilities().flying
                || player.isInWater() || player.isInLava() || player.getDeltaMovement().y >= 0.0) {
            return 0.0;
        }
        return DAMPING[pieces(player)];
    }
}
