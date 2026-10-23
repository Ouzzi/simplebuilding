package com.simplesandwiches.registry;

import com.simplesandwiches.Sandwiches;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Slippery (owner 24.10., NA24-5): walking on a butter block or butter carpet makes every ground as slippery as butter for
 * {@link #SLIPPERY_TICKS}. The friction itself is applied by {@code SlipperyFrictionMixin}; this class registers the effect
 * (Fabric directly, NeoForge/Forge in the MOB_EFFECT RegisterEvent) and hands it out.
 */
public final class ModEffects {
    /** 3 s (owner). */
    public static final int SLIPPERY_TICKS = 60;
    /** The effect is renewed once less than this is left, so standing on butter sends no packet every tick. */
    private static final int RENEW_BELOW = 20;
    public static @Nullable Holder<MobEffect> SLIPPERY;

    public static void register() {
        if (SLIPPERY == null) SLIPPERY = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Sandwiches.id("slippery"),
                new MobEffect(MobEffectCategory.NEUTRAL, 0xF3D860) {});
    }

    /** Friction of a slippery wearer on any ground: at least the butter block's. */
    public static float friction(LivingEntity entity, float friction) {
        return SLIPPERY != null && friction < ModBlocks.BUTTER_FRICTION && entity.hasEffect(SLIPPERY) ? ModBlocks.BUTTER_FRICTION : friction;
    }

    /** Server side {@code stepOn} of butter: gives or renews the effect (icon, no particles). */
    public static void slip(Level level, Entity entity) {
        if (level.isClientSide() || SLIPPERY == null || !Sandwiches.on() || !(entity instanceof LivingEntity living)) return;
        MobEffectInstance now = living.getEffect(SLIPPERY);
        if (now == null || now.getDuration() < RENEW_BELOW) living.addEffect(new MobEffectInstance(SLIPPERY, SLIPPERY_TICKS, 0, false, false, true));
    }

    private ModEffects() {}
}
