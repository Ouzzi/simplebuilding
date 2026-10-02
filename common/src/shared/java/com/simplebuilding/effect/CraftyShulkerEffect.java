package com.simplebuilding.effect;

import com.simplebuilding.config.ServerTuning;
import com.simplebuilding.version.McVersion;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Listiger Shulker (Besitzer 2026-10-02): wird der Traeger von einem Wesen getroffen (Nahkampf, Geschoss,
 * Explosion eines Wesens), springt er wie mit einer Chorusfrucht an eine sichere Stelle in der Naehe. Fall-, Feuer-,
 * Hunger- und Leere-Schaden loesen nichts aus.
 *
 * <p>Sicher ({@link #findSafeSpot}): fester Boden (Oberseite tragfaehig), Fuesse und Kopf frei, keine Fluessigkeit
 * (Lava, Wasser), kein Block aus {@link ModEffects#UNSAFE_LANDING} unter oder in der Stelle, innerhalb der
 * Weltgrenze und in geladenen Chunks, mindestens zwei Bloecke weg. Ohne sichere Stelle in {@value #ATTEMPTS}
 * Versuchen bleibt er stehen (keine Leere: der Boden muss existieren). Abklingzeit und Reichweite:
 * {@code server.craftyShulker} (20..1200 Ticks, 2..16 Bloecke).
 */
public class CraftyShulkerEffect extends MobEffect {
    public static final int ATTEMPTS = 16;
    private static final Map<LivingEntity, Long> LAST_TELEPORT = new WeakHashMap<>();

    public CraftyShulkerEffect(int color) {
        super(MobEffectCategory.BENEFICIAL, color);
    }

    @Override
    public void onMobHurt(ServerLevel level, LivingEntity mob, int amplifier, DamageSource source, float damage) {
        if (!triggers(source) || !mob.isAlive()) {
            return;
        }
        long now = level.getGameTime();
        Long last = LAST_TELEPORT.get(mob);
        if (last != null && now - last < ServerTuning.craftyShulkerCooldown()) {
            return;
        }
        if (teleport(level, mob, mob.getRandom(), ServerTuning.craftyShulkerRadius())) {
            LAST_TELEPORT.put(mob, now);
        }
    }

    /** Nur Schaden, hinter dem ein Wesen steht, und nichts, was Unverwundbarkeit durchschlaegt (Leere, /kill). */
    public static boolean triggers(DamageSource source) {
        return (source.getEntity() != null || source.getDirectEntity() != null)
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    /** Nur fuer Tests: die Abklingzeit eines Wesens vergessen. */
    public static void resetCooldown(LivingEntity mob) {
        LAST_TELEPORT.remove(mob);
    }

    /** Sucht eine sichere Stelle und springt dorthin (Shulker-Klang, Portal-Partikel). */
    public static boolean teleport(ServerLevel level, LivingEntity mob, RandomSource random, int radius) {
        Vec3 from = mob.position();
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            Vec3 target = findSafeSpot(level, mob, random, radius);
            if (target == null) {
                continue;
            }
            if (mob.isPassenger()) {
                mob.stopRiding();
            }
            if (McVersion.randomTeleport(mob, target.x, target.y, target.z, true, ModEffects.UNSAFE_LANDING)) {
                level.gameEvent(GameEvent.TELEPORT, from, GameEvent.Context.of(mob));
                level.playSound(null, from.x, from.y, from.z, SoundEvents.SHULKER_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.SHULKER_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                return true;
            }
        }
        return false;
    }

    /** Eine zufaellige sichere Stelle im Umkreis {@code radius}, oder null (ein Versuch). */
    public static @Nullable Vec3 findSafeSpot(ServerLevel level, LivingEntity mob, RandomSource random, int radius) {
        double x = mob.getX() + (random.nextDouble() * 2.0 - 1.0) * radius;
        double z = mob.getZ() + (random.nextDouble() * 2.0 - 1.0) * radius;
        int minY = level.getMinY() + 1;
        int maxY = level.getMinY() + level.getLogicalHeight() - 1;
        int top = Mth.clamp(Mth.floor(mob.getY()) + random.nextInt(2 * radius + 1) - radius, minY, maxY);
        int bottom = Mth.clamp(Mth.floor(mob.getY()) - radius, minY, maxY);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Mth.floor(x), top, Mth.floor(z));
        if (!level.getWorldBorder().isWithinBounds(pos) || !level.hasChunkAt(pos)) {
            return null;
        }
        for (int y = top; y >= bottom; y--) {
            pos.setY(y);
            BlockPos below = pos.below();
            BlockState ground = level.getBlockState(below);
            if (!ground.isFaceSturdy(level, below, Direction.UP)) {
                continue;
            }
            Vec3 spot = new Vec3(x, y, z);
            AABB box = mob.getBoundingBox().move(spot.subtract(mob.position()));
            if (!level.noCollision(mob, box)) {
                continue; // im Gestein: weiter unten nach einer Hoehle suchen
            }
            if (spot.distanceToSqr(mob.position()) < 4.0 || ground.is(ModEffects.UNSAFE_LANDING) || level.containsAnyLiquid(box)
                    || BlockPos.betweenClosedStream(box).anyMatch(p -> level.getBlockState(p).is(ModEffects.UNSAFE_LANDING))) {
                return null;
            }
            return spot;
        }
        return null;
    }
}
