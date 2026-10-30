package com.simplebuilding.modules.simplemobs;

import com.simplebuilding.framework.api.ResonanceContracts;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityTypes;

/**
 * Danger zones for new monsters (WP-22, MOB27-2): after Vanilla finished a natural spawn, Simple Resonance says which level the place
 * has for the player who owns the attempt, and {@link ZoneEffects} draws status effects for it. Only natural spawns, patrols and chunk
 * generation of monsters; no spawner, egg, command, raid, summoned, named or Simple Mobs boss; only effects are added, never health or
 * attributes. Needs the Simple Resonance contract; without it (or switched off) the monster stays as Vanilla made it.
 */
public final class ZoneSpawns {
    /** Test hook: a fixed sequence of rolls instead of the mob's random (null = random). Server thread only. */
    static java.util.function.DoubleSupplier testRng;

    /** Vanilla bosses and Simple Mobs' own: they keep their own values. */
    private static final Set<EntityType<?>> BOSSES = Set.of(EntityTypes.WITHER, EntityTypes.ENDER_DRAGON, EntityTypes.WARDEN, EntityTypes.ELDER_GUARDIAN);

    private ZoneSpawns() {}

    /** Whether a spawn of this kind may get zone effects. */
    public static boolean eligible(Mob mob, EntitySpawnReason reason) {
        if (!MobsConfig.zoneMobsEnabled || MobsConfig.zoneMaxEffects <= 0 || !MobsRegistry.on()) return false;
        if (!MobsConfig.zoneSpawnReasons.contains(reason.name().toLowerCase(java.util.Locale.ROOT))) return false;
        if (mob.getType().getCategory() != MobCategory.MONSTER || mob.hasCustomName()) return false;
        EntityType<?> type = mob.getType();
        return !BOSSES.contains(type) && !"simplemobs".equals(BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace());
    }

    /** The hook ({@code Mob.finalizeSpawn} return): draws and applies the effects; returns what was applied. */
    public static List<ZoneEffects.Rolled> onSpawn(Mob mob, EntitySpawnReason reason) {
        if (!eligible(mob, reason)) return List.of();
        ResonanceContracts.Service service = ResonanceContracts.get();
        if (service == null || !service.enabled() || !(mob.level() instanceof ServerLevel level)) return List.of();
        java.util.function.DoubleSupplier rng = testRng != null ? testRng : () -> mob.getRandom().nextDouble();
        String dimension = level.dimension().identifier().toString();
        ResonanceContracts.Spawn spawn = service.sample(dimension, mob.getX(), mob.getY(), mob.getZ(), rng.getAsDouble(), rng.getAsDouble());
        List<ZoneEffects.Rolled> rolled = ZoneEffects.roll(spawn.level(), MobsConfig.zoneMaxEffects,
                pool -> MobsConfig.zoneChance[pool.ordinal()], pool -> MobsConfig.zoneLevelCap[pool.ordinal()], rng);
        ZoneEffects.apply(mob, rolled);
        return rolled;
    }

    /** Boss kills of Simple Mobs count for the killer's resonance value (+5 each, the service limits repeats). */
    public static void onDeath(LivingEntity entity, DamageSource source) {
        if (!(source.getEntity() instanceof ServerPlayer killer)) return;
        EntityType<?> type = entity.getType();
        if (type != MobsRegistry.HOLLOW_HOWLER && type != WitherMobs.BLIGHTED_WITHER && type != WitherMobs.ABYSSAL_WITHER && type != FearMobs.WATCHER) return;
        ResonanceContracts.Service service = ResonanceContracts.get();
        if (service != null && service.enabled()) service.award(killer.getUUID(), String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(type)), 5.0);
    }
}
