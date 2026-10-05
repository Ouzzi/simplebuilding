package com.simplebuilding.effect;

import com.simplebuilding.fluid.SoulLava;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Seelenbrand (owner round 2 answer 28 + round 3 answer 55): after touching soul lava, for one minute,
 * every {@link SoulLava#SOUL_BURN_INTERVAL_TICKS} ticks a {@link SoulLava#SOUL_BURN_CHANCE} chance of 1
 * fire damage. Fire resistance only protects against the burning and the fire damage while it lasts
 * - the effect itself keeps running, so damage comes back when the protection ends first. The
 * damage is Vanilla's on_fire type (tag is_fire): the Fire Resistance effect blocks it, the Fire
 * Protection enchantment reduces it.
 */
public class SoulBurnEffect extends MobEffect {
    /** Soul fire turquoise. */
    public static final int COLOR = 0x3FD9E0;

    public SoulBurnEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplification) {
        if (mob.hasEffect(MobEffects.FIRE_RESISTANCE) || mob.getType().fireImmune()) return true;
        if (mob.getRandom().nextFloat() < SoulLava.SOUL_BURN_CHANCE) {
            mob.hurtServer(level, mob.damageSources().onFire(), 1.0F);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
        return tickCount % SoulLava.SOUL_BURN_INTERVAL_TICKS == 0;
    }
}
