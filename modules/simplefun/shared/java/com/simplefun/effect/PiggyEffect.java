package com.simplefun.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class PiggyEffect extends MobEffect {

    public PiggyEffect() {
        // Cosmetic only (renders a pig head) -> NEUTRAL, not removed by milk as a debuff.
        super(MobEffectCategory.NEUTRAL, 0xF0A6A6);
    }

    // No tick logic: default shouldApplyEffectTickThisTick (false) keeps applyEffectTick from running every tick.
}
