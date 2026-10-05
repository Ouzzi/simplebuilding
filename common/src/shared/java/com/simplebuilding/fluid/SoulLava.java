package com.simplebuilding.fluid;

import com.simplebuilding.effect.ModEffects;
import com.simplebuilding.config.ServerTuning;
import com.simplebuilding.config.ServerTuningConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** Server-authoritative soul lava tuning; bucket fuel is read only during item registration. */
public final class SoulLava {
    public static final int IGNITE_ATTEMPTS = 4;
    public static final int IGNITE_RANGE = 2;
    public static final int LAVA_FUEL_TICKS = 20_000;

    public static int flowOverworld() { return ServerTuningConfig.clamp(ServerTuning.get().soulLava.flowOverworld, 1, 4); }
    public static int flowNether() { return ServerTuningConfig.clamp(ServerTuning.get().soulLava.flowNether, 1, 7); }
    public static int tickDelayOverworld() { return ServerTuningConfig.clamp(ServerTuning.get().soulLava.tickDelayOverworld, 20, 200); }
    public static int tickDelayNether() { return ServerTuningConfig.clamp(ServerTuning.get().soulLava.tickDelayNether, 10, 200); }
    public static int burnSeconds() { return ServerTuningConfig.clamp(ServerTuning.get().soulLava.burnSeconds, 5, 60); }
    public static int soulBurnSeconds() { return ServerTuningConfig.clamp(ServerTuning.get().soulLava.soulBurnSeconds, 5, 300); }
    public static int soulBurnIntervalTicks() { return ServerTuningConfig.clamp(ServerTuning.get().soulLava.soulBurnIntervalTicks, 20, 200); }
    public static double soulBurnChance() { return ServerTuningConfig.clamp(ServerTuning.get().soulLava.soulBurnChance, 0.0, 1.0, 0.5); }
    public static int fuelMultiplier() { return ServerTuningConfig.clamp(ServerTuning.local().soulLava.fuelMultiplier, 1, 20); }
    public static double springChance() { return ServerTuningConfig.clamp(ServerTuning.get().soulLava.springChance, 0.0, 0.05, 0.005); }
    public static double fortressChance() { return ServerTuningConfig.clamp(ServerTuning.get().soulLava.fortressChance, 0.0, 0.5, 0.1); }

    public static int soulBurnTicks() { return soulBurnSeconds() * 20; }
    /** Registration-time value, like charge capacities. Requires restart. */
    public static int fuelTicks() { return LAVA_FUEL_TICKS * fuelMultiplier(); }

    /** Contact effect (fluid, reinforced cauldron): configured burning, lava damage and soul burn. */
    public static void touch(Level level, Entity entity) {
        if (!(level instanceof ServerLevel) || entity.getType().fireImmune()) return;
        entity.igniteForSeconds(burnSeconds());
        entity.lavaHurt();
        if (entity instanceof LivingEntity living && ModEffects.SOUL_BURN != null) {
            living.addEffect(new MobEffectInstance(ModEffects.SOUL_BURN, soulBurnTicks()));
        }
    }

    private SoulLava() {}
}
