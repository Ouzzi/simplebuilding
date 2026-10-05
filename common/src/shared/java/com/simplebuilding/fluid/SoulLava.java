package com.simplebuilding.fluid;

import com.simplebuilding.effect.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Soul lava numbers (plan PLAN-CRUCIBLE-2026-10-04 section 9/13, owner answers round 2/3). Fixed values
 * inside the plan's hard limits; a server config section is still open (memory crucible-offen).
 */
public final class SoulLava {
    /** Horizontal reach in blocks: Overworld 2, Nether 5 (owner 25; lava 3/7). */
    public static final int FLOW_OVERWORLD = 2;
    public static final int FLOW_NETHER = 5;
    /** Spread ticks: slower than lava (30/10). */
    public static final int TICK_DELAY_OVERWORLD = 45;
    public static final int TICK_DELAY_NETHER = 20;
    /** Touching burns twice as long as lava (15 s -> 30 s, owner 25). */
    public static final float BURN_SECONDS = 30.0F;
    /** Seelenbrand: 1 minute, every 3 s a 50 % chance of 1 fire damage (owner 28). */
    public static final int SOUL_BURN_TICKS = 60 * 20;
    public static final int SOUL_BURN_INTERVAL_TICKS = 60;
    public static final float SOUL_BURN_CHANCE = 0.5F;
    /** Fire: 4x the attempts of lava, twice the reach (owner wish round 2). */
    public static final int IGNITE_ATTEMPTS = 4;
    public static final int IGNITE_RANGE = 2;
    /** Fuel: 10x a lava bucket (owner 57 A). */
    public static final int FUEL_TICKS = 20_000 * 10;
    /** World generation: Nether lava springs 0.5 %, fortress well 10 % (owner addition 11 + answer 31). */
    public static final float SPRING_CHANCE = 0.005F;
    public static final float FORTRESS_CHANCE = 0.10F;

    /** What touching soul lava does (fluid, reinforced cauldron): burn twice as long as lava, lava damage, Seelenbrand. */
    public static void touch(Level level, Entity entity) {
        if (!(level instanceof ServerLevel) || entity.getType().fireImmune()) return;
        entity.igniteForSeconds(BURN_SECONDS);
        entity.lavaHurt();
        if (entity instanceof LivingEntity living && ModEffects.SOUL_BURN != null) {
            living.addEffect(new MobEffectInstance(ModEffects.SOUL_BURN, SOUL_BURN_TICKS));
        }
    }

    private SoulLava() {}
}
