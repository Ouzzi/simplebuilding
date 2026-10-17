package com.simplecrucibles.fluid;

import com.simplecrucibles.effect.CrucibleEffects;
import com.simplecrucibles.config.CrucibleConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** Server-authoritative soul lava tuning (simplecrucibles-server.json, group soulLava); bucket fuel is read only during item registration. */
public final class SoulLava {
    public static final int IGNITE_ATTEMPTS = 4;
    public static final int IGNITE_RANGE = 2;
    public static final int LAVA_FUEL_TICKS = 20_000;

    // Values of the soulLava group of simplecrucibles-server.json, clamped there to their hard bounds (CrucibleConfig).
    public static int flowOverworld() { return CrucibleConfig.soulLava.flowOverworld; }
    public static int flowNether() { return CrucibleConfig.soulLava.flowNether; }
    public static int tickDelayOverworld() { return CrucibleConfig.soulLava.tickDelayOverworld; }
    public static int tickDelayNether() { return CrucibleConfig.soulLava.tickDelayNether; }
    public static int burnSeconds() { return CrucibleConfig.soulLava.burnSeconds; }
    public static int soulBurnSeconds() { return CrucibleConfig.soulLava.soulBurnSeconds; }
    public static int soulBurnIntervalTicks() { return CrucibleConfig.soulLava.soulBurnIntervalTicks; }
    public static double soulBurnChance() { return CrucibleConfig.soulLava.soulBurnChance; }
    public static int fuelMultiplier() { return CrucibleConfig.soulLava.fuelMultiplier; }
    public static double springChance() { return CrucibleConfig.soulLava.springChance; }
    public static double fortressChance() { return CrucibleConfig.soulLava.fortressChance; }

    public static int soulBurnTicks() { return soulBurnSeconds() * 20; }
    /** Registration-time value, like charge capacities. Requires restart. */
    public static int fuelTicks() { return LAVA_FUEL_TICKS * fuelMultiplier(); }

    /** Contact effect (fluid, reinforced cauldron): configured burning, lava damage and soul burn. */
    public static void touch(Level level, Entity entity) {
        if (!(level instanceof ServerLevel) || entity.getType().fireImmune()) return;
        entity.igniteForSeconds(burnSeconds());
        entity.lavaHurt();
        if (entity instanceof LivingEntity living && CrucibleEffects.SOUL_BURN != null) {
            living.addEffect(new MobEffectInstance(CrucibleEffects.SOUL_BURN, soulBurnTicks()));
        }
    }

    private SoulLava() {}
}
