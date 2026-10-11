package com.simplebuilding.modules.simplemobs;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Silence (Potion of Silence, brewed from the Echo Core): the bearer's vibrations are dampened like wool, so sculk
 * sensors, Wardens and Howlers do not hear it. Melee and sight still work. The work is done by the Entity mixin.
 */
public final class SilenceEffect extends MobEffect {
    public static final int COLOR = 0x0E5A63;

    public SilenceEffect() { super(MobEffectCategory.BENEFICIAL, COLOR); }
}
