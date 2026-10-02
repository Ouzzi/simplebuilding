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
 * der Nebenhand nicht. Unter nicht pausierenden Bildschirmen (Inventar, Chat, Truhe) laeuft er weiter (Besitzer
 * 2026-10-02, {@code KeyboardInputMixin}), ein Pausenmenue laesst los. Rueckmeldung nur per Klickton.
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
    /** Ob {@code KeyboardInputMixin} in diesem Tick "vorwaerts" setzen soll. */
    private static boolean forcing;

    private GaugeAutowalk() {
    }

    /** Vom {@link VelocityGaugeItem#use} auf dem Client aufgerufen. */
    public static void toggle(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (player != mc.player) {
            return;
        }
        walking = !walking;
        forcing = walking;
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

    /** Fuer {@code KeyboardInputMixin}: der Autowalk laeuft und kein pausierender Bildschirm ist offen. */
    public static boolean forcesForward() {
        return walking && forcing;
    }

    public static void tick(Minecraft mc) {
        if (!walking) {
            return;
        }
        Player player = mc.player;
        if (player == null || !VelocityGaugeItem.autowalkKeepsGauge(player.getInventory().getSelectedSlot(), slot,
                player.getMainHandItem())) {
            stop(mc);
            return;
        }
        held = player.getMainHandItem();
        net.minecraft.client.gui.screens.Screen screen = mc.gui.screen();
        forcing = VelocityGaugeItem.autowalkContinuesUnder(screen != null, screen != null && screen.isPauseScreen());
        if (!forcing) {
            mc.options.keyUp.setDown(false);
            return;
        }
        if (screen == null) {
            // Ohne Bildschirm wie bisher ueber die Taste; unter Bildschirmen setzt KeyboardInputMixin "vorwaerts".
            mc.options.keyUp.setDown(true);
        }
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
        forcing = false;
        held = ItemStack.EMPTY;
        mc.options.keyUp.setDown(false);
    }
}
