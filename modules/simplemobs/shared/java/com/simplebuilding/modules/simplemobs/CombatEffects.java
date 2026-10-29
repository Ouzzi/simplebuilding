package com.simplebuilding.modules.simplemobs;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * MOB27-1 combat effects, levels I-V (amplifier 0-4): Volatile (a hit on the bearer sets off a small explosion without block
 * damage; the bearer is immune), Deflection (10 % per level to negate a hit) and Mirror (coin flip per hit: the bearer takes
 * 120...200 % damage OR the attacker takes 20...100 % back, never lethal). All three react to hits only (a damage source with
 * a direct entity), not to fall, fire or void damage. Apply with {@code /effect give <target> simplemobs:volatile 60 0}.
 */
public final class CombatEffects {
    public static final String ID = MobsRegistry.ID;
    public static final ResourceKey<MobEffect> VOLATILE_KEY = key("volatile"), DEFLECTION_KEY = key("deflection"), MIRROR_KEY = key("mirror");
    public static final MobEffect VOLATILE = new Effect(MobEffectCategory.BENEFICIAL, 0xE0701E);
    public static final MobEffect DEFLECTION = new Effect(MobEffectCategory.BENEFICIAL, 0x9FC8E8);
    public static final MobEffect MIRROR = new Effect(MobEffectCategory.NEUTRAL, 0xC9D3DC);
    private static @Nullable Holder<MobEffect> volatileHolder, deflectionHolder, mirrorHolder;

    /** Volatile: centre damage (half hearts) and radius. Level I deals ~3 hearts to an attacker at melee distance (1.5 blocks). */
    public static final float VOLATILE_DAMAGE = 10.0f, VOLATILE_DAMAGE_PER_LEVEL = 2.0f;
    public static final float VOLATILE_RADIUS = 4.0f, VOLATILE_RADIUS_PER_LEVEL = 0.5f;
    /** Reflected damage never kills: the attacker is left with this much health. */
    public static final float REFLECT_LEAVES = 1.0f;

    /** Test hook: a fixed roll in [0,1) replaces the random rolls (null = random). Server thread only. */
    static @Nullable Double testRoll;
    /** Re-entrancy guard: our own explosion or reflected damage must not trigger further effects. */
    private static boolean busy;

    private CombatEffects() {}

    private static final class Effect extends MobEffect {
        Effect(MobEffectCategory category, int color) { super(category, color); }
    }

    private static ResourceKey<MobEffect> key(String path) {
        return ResourceKey.create(Registries.MOB_EFFECT, Identifier.fromNamespaceAndPath(ID, path));
    }

    public static Holder<MobEffect> volatileEffect() { if (volatileHolder == null) volatileHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(VOLATILE); return volatileHolder; }
    public static Holder<MobEffect> deflection() { if (deflectionHolder == null) deflectionHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(DEFLECTION); return deflectionHolder; }
    public static Holder<MobEffect> mirror() { if (mirrorHolder == null) mirrorHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(MIRROR); return mirrorHolder; }

    /** Fabric: direct registration. */
    public static void registerAll() {
        volatileHolder = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, VOLATILE_KEY, VOLATILE);
        deflectionHolder = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, DEFLECTION_KEY, DEFLECTION);
        mirrorHolder = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, MIRROR_KEY, MIRROR);
    }

    // --- pure rules (unit-tested) ---

    /** Deflection: 10 % per level (amplifier 0 = level I), at most 50 %. */
    public static float deflectChance(int amplifier) { return Math.min(0.5f, 0.1f * (amplifier + 1)); }
    /** Mirror, bearer side: 120 % (I) ... 200 % (V). */
    public static float bearerFactor(int amplifier) { return 1.2f + 0.2f * Math.min(amplifier, 4); }
    /** Mirror, attacker side: 20 % (I) ... 100 % (V) of the original damage. */
    public static float reflectFactor(int amplifier) { return 0.2f + 0.2f * Math.min(amplifier, 4); }
    public static float volatileRadius(int amplifier) { return VOLATILE_RADIUS + VOLATILE_RADIUS_PER_LEVEL * Math.min(amplifier, 4); }
    /** Volatile damage at a distance: linear falloff from the centre damage to 0 at the radius. */
    public static float volatileDamage(int amplifier, double distance) {
        float radius = volatileRadius(amplifier);
        if (distance >= radius) return 0f;
        return (VOLATILE_DAMAGE + VOLATILE_DAMAGE_PER_LEVEL * Math.min(amplifier, 4)) * (float) (1.0 - distance / radius);
    }
    /** Reflected damage is capped so that the victim keeps at least {@link #REFLECT_LEAVES} health (0 when already there). */
    public static float safeReflect(float health, float damage) { return Math.max(0f, Math.min(damage, health - REFLECT_LEAVES)); }

    // --- the hit hook (called by CombatEffectsMixin around LivingEntity.hurtServer) ---

    private static boolean isHit(DamageSource source) {
        return source.getDirectEntity() != null && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    private static double roll(LivingEntity e) { return testRoll != null ? testRoll : e.getRandom().nextDouble(); }

    private static int amp(LivingEntity e, Holder<MobEffect> effect) {
        var inst = e.getEffect(effect);
        return inst == null ? -1 : inst.getAmplifier();
    }

    /**
     * Wraps one hurtServer call. {@code original} runs the real hit with the given damage. Order: Deflection (hit gone) ->
     * Mirror coin (bearer damage scaled now, reflection after a landed hit) -> the hit -> Volatile explosion.
     */
    public static boolean hurt(LivingEntity self, ServerLevel level, DamageSource source, float damage, java.util.function.Function<Float, Boolean> original) {
        if (busy || damage <= 0f || !MobsRegistry.on() || !isHit(source)
                || !(self.hasEffect(deflection()) || self.hasEffect(mirror()) || self.hasEffect(volatileEffect()))) return original.apply(damage);
        int deflect = amp(self, deflection());
        if (deflect >= 0 && roll(self) < deflectChance(deflect)) {
            level.sendParticles(ParticleTypes.CRIT, self.getX(), self.getY(0.5), self.getZ(), 8, 0.3, 0.4, 0.3, 0.1);
            level.playSound(null, self.blockPosition(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS, 0.8f, 1.4f);
            return false;
        }
        int mirror = amp(self, mirror());
        boolean reflect = false;
        float dealt = damage;
        if (mirror >= 0) {
            reflect = roll(self) < MobsConfig.mirrorReflectChance;
            if (!reflect) dealt = damage * bearerFactor(mirror);
        }
        boolean hit = original.apply(dealt);
        if (!hit) return false;
        busy = true;
        try {
            if (reflect) reflect(self, level, source, damage * reflectFactor(mirror));
            int vol = amp(self, volatileEffect());
            if (vol >= 0) explode(self, level, vol);
        } finally {
            busy = false;
        }
        return true;
    }

    private static void reflect(LivingEntity bearer, ServerLevel level, DamageSource source, float damage) {
        if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == bearer || !attacker.isAlive()) return;
        float safe = safeReflect(attacker.getHealth(), damage);
        if (safe <= 0f) return;
        attacker.hurtServer(level, level.damageSources().thorns(bearer), safe);
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, attacker.getX(), attacker.getY(0.5), attacker.getZ(), 10, 0.3, 0.4, 0.3, 0.2);
        level.playSound(null, attacker.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.6f, 1.6f);
    }

    /** A small explosion at the bearer: damage and push for everyone nearby except the bearer, no block damage. */
    static void explode(LivingEntity bearer, ServerLevel level, int amplifier) {
        Vec3 c = bearer.position().add(0, bearer.getBbHeight() * 0.5, 0);
        float radius = volatileRadius(amplifier);
        DamageSource src = level.damageSources().explosion(bearer, bearer);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(radius), x -> x != bearer && x.isAlive())) {
            double d = e.position().add(0, e.getBbHeight() * 0.5, 0).distanceTo(c);
            float dmg = volatileDamage(amplifier, d);
            if (dmg <= 0f) continue;
            if (e.hurtServer(level, src, dmg)) {
                Vec3 away = e.position().subtract(bearer.position()).multiply(1, 0, 1);
                if (away.lengthSqr() > 1.0E-4) e.push(away.normalize().scale(0.4).add(0, 0.2, 0));
            }
        }
        level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.SMOKE, c.x, c.y, c.z, 12, 0.4, 0.4, 0.4, 0.05);
        level.playSound(null, bearer.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7f, 1.2f);
    }
}
