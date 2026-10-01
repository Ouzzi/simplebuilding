package com.simplebuilding.client;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.VelocityGaugeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemStack;

/**
 * Messuhr-Autowalk (Besitzer 2026-10-01): Rechtsklick mit der Messuhr in der Haupthand schaltet das
 * Vorwaertslaufen an und aus. Ein Wechsel des Haupthand-Slots oder -Items schaltet es ab, ein Wechsel
 * der Nebenhand nicht. Ein offener Bildschirm laesst die Taste los. Rueckmeldung nur per Klickton.
 *
 * <p>Mit Beruehrung des Konstrukteurs folgt der Autowalk zusaetzlich Trampelpfaden und Schienen: endet
 * der Weg geradeaus und fuehrt genau eine Seite weiter, dreht sich der Blick in diese Richtung.
 * Reine Eingabe des eigenen Clients (wie gehaltene Vorwaertstaste), keine Server-Regel.
 */
public final class GaugeAutowalk {
    private static boolean walking;
    private static int slot = -1;
    private static ItemStack held = ItemStack.EMPTY;
    private static int turnCooldown;

    private GaugeAutowalk() {
    }

    /** Vom {@link VelocityGaugeItem#use} auf dem Client aufgerufen. */
    public static void toggle(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (player != mc.player) {
            return;
        }
        walking = !walking;
        slot = player.getInventory().getSelectedSlot();
        held = player.getMainHandItem();
        if (!walking) {
            mc.options.keyUp.setDown(false);
        }
        player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.4F, walking ? 1.2F : 0.8F);
    }

    public static boolean isWalking() {
        return walking;
    }

    public static void tick(Minecraft mc) {
        if (!walking) {
            return;
        }
        Player player = mc.player;
        if (player == null || player.getInventory().getSelectedSlot() != slot || player.getMainHandItem() != held
                || !(held.getItem() instanceof VelocityGaugeItem)) {
            stop(mc);
            return;
        }
        if (mc.gui.screen() != null) {
            mc.options.keyUp.setDown(false);
            return;
        }
        mc.options.keyUp.setDown(true);
        if (turnCooldown > 0) {
            turnCooldown--;
        } else if (com.simplebuilding.util.EnchantmentHelper.getEnchantmentLevel(held, player.level(), ModEnchantments.CONSTRUCTORS_TOUCH) > 0) {
            Direction turn = VelocityGaugeItem.followTurn(player.level(), player.getVehicle() instanceof AbstractMinecart cart ? cart.blockPosition()
                    : player.getOnPos(), player.getDirection());
            if (turn != null) {
                player.setYRot(turn.toYRot());
                turnCooldown = 10;
            }
        }
    }

    private static void stop(Minecraft mc) {
        walking = false;
        held = ItemStack.EMPTY;
        mc.options.keyUp.setDown(false);
    }
}
