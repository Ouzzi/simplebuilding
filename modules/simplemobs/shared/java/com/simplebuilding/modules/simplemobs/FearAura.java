package com.simplebuilding.modules.simplemobs;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * The Shivering aura of the fear mobs: every second, players within 16 blocks get Shivering I when the mob can see them
 * and Shivering II closer than 6 blocks, for 3 s (Milk clears it until the next pulse). Immune players (Focus, Calm
 * Amulet) are skipped and lose Shivering they already have.
 */
public final class FearAura {
    /** GameTest hook: mock players are creative, tests tag a mob as a stand-in target. */
    public static final String TEST_TAG = "simplemobs.fear_test_target";

    private FearAura() {}

    /** Who the aura and the fear mobs' attacks reach: survival/adventure players, or tagged test mobs. */
    public static boolean affects(LivingEntity e) {
        if (e instanceof Player p) return !p.isSpectator() && !p.isCreative() && e.isAlive();
        return e.isAlive() && e.entityTags().contains(TEST_TAG);
    }

    /** One pulse; returns how many entities got Shivering. */
    public static int pulse(ServerLevel level, Mob source, boolean capOne) {
        Holder<MobEffect> shivering = FearMobs.shivering();
        if (shivering == null || !MobsRegistry.on()) return 0;
        int hit = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, source.getBoundingBox().inflate(FearLogic.AURA_RADIUS), FearAura::affects)) {
            if (cure(e)) continue;
            int lvl = FearLogic.auraLevel(e.distanceTo(source), source.hasLineOfSight(e), capOne);
            if (lvl > 0 && give(e, shivering, lvl - 1, FearLogic.AURA_TICKS)) hit++;
        }
        return hit;
    }

    /** Removes Shivering from an immune entity; true if it is immune. */
    public static boolean cure(LivingEntity e) {
        if (!FearMobs.immune(e)) return false;
        Holder<MobEffect> shivering = FearMobs.shivering();
        if (shivering != null && e.hasEffect(shivering)) e.removeEffect(shivering);
        return true;
    }

    /** Adds Shivering unless a stronger or longer one is running. */
    public static boolean give(LivingEntity e, Holder<MobEffect> shivering, int amplifier, int ticks) {
        MobEffectInstance cur = e.getEffect(shivering);
        if (cur != null && (cur.getAmplifier() > amplifier || cur.getAmplifier() == amplifier && cur.getDuration() > ticks)) return false;
        return e.addEffect(new MobEffectInstance(shivering, ticks, amplifier, true, true, true));
    }
}
