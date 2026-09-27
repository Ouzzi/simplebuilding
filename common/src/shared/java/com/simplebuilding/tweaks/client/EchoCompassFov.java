package com.simplebuilding.tweaks.client;

import com.simplebuilding.tweaks.item.EchoCompassItem;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * FOV-Sog beim Aufladen des Echo-Kompasses: das Sichtfeld zieht sich mit der Ladung sanft zusammen
 * (bis 12 % enger) und pulsiert leicht; nicht voll repariert pulsiert es staerker und schneller als
 * Warnzeichen. Skaliert mit Vanillas Barrierefreiheits-Regler "FOV-Effekte" - auf 0 ist der Effekt
 * aus. Loader-neutral; angebunden per Mixin (Fabric, NeoForge) bzw. ComputeFovModifierEvent (Forge).
 */
public final class EchoCompassFov {
    private static final float MAX_ZOOM = 0.12f;

    private EchoCompassFov() {
    }

    /** Faktor fuer den FOV-Modifikator (1 = unveraendert), bereits mit {@code effectScale} gewichtet. */
    public static float factor(Player player, float effectScale) {
        if (effectScale <= 0.0f || !player.isUsingItem()) {
            return 1.0f;
        }
        ItemStack stack = player.getUseItem();
        if (!(stack.getItem() instanceof EchoCompassItem)) {
            return 1.0f;
        }
        return Mth.lerp(effectScale, 1.0f, raw(player.getTicksUsingItem(), EchoCompassItem.chargeTicks(stack),
                EchoCompassItem.isCracked(stack)));
    }

    /** Ungewichteter Faktor nach {@code ticksUsing} von {@code total} Ladeticks. */
    static float raw(int ticksUsing, int total, boolean cracked) {
        float progress = Mth.clamp(ticksUsing / (float) total, 0.0f, 1.0f);
        float eased = progress * progress * (3.0f - 2.0f * progress);
        float pulseAmplitude = (cracked ? 0.03f : 0.012f) * progress;
        float pulse = Mth.sin(ticksUsing * (cracked ? 0.9f : 0.5f)) * pulseAmplitude;
        return 1.0f - MAX_ZOOM * eased + pulse;
    }
}
