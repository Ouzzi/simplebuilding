package com.simplebuilding.tweaks.xp;

import com.simplebuilding.tweaks.SimpleTweaks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Stapelgrenzen aus der Config (Simple Tweaks: nur Raketen, {@code balancing.rocketStackSize}). */
public final class StackLimits {
    private StackLimits() {
    }

    /** Die Grenze fuer diesen Stapel; {@code vanilla} ist, was das Spiel sonst sagen wuerde. */
    public static int limit(ItemStack stack, int vanilla) {
        if (stack.is(Items.FIREWORK_ROCKET)) {
            int limit = SimpleTweaks.config().balancing.rocketStackSize;
            if (limit >= 1 && limit < vanilla) {
                return limit;
            }
        }
        return vanilla;
    }
}
