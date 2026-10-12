package com.simplebuilding.modules.simplemobs;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** Pure rules of the Shellker, High Gravity and the Soulfire Blaze (tested without a world). */
public final class EndNetherLogic {
    /** 4x a Shulker (30). */
    public static final double SHELLKER_HEALTH = 120.0;
    /** Shulker bullet speed 0.15 x 1.5. */
    public static final double BULLET_SPEED = 0.225;
    /** High Gravity from a bullet (owner N45): 30 s, 1 min when the target is already under High Gravity. */
    public static final int BULLET_EFFECT_TICKS = 600, BULLET_EFFECT_REPEAT_TICKS = 1200;
    /** A bullet cannot be hit away or shot down during its first 2 s (owner N45). */
    public static final int BULLET_SHIELD_TICKS = 40;
    /** Fires 1.2x as often as a Shulker (owner N45) and almost at once after opening, so hit-spamming cannot hold it. */
    public static final double FIRE_RATE = 1.2;
    public static final int FIRST_SHOT_TICKS = 2;
    public static final int POTION_TICKS = 3600, LONG_POTION_TICKS = 9600;
    /** Upward forces (jump, Levitation, Elytra lift) keep 10 %. */
    public static final double UPWARD_FACTOR = 0.1;
    /** Elytra under High Gravity: extra sink per tick and horizontal drag (roughly a ten times worse glide). */
    public static final double ELYTRA_SINK = 0.07, ELYTRA_DRAG = 0.92;
    public static final int HELMET_DURABILITY = 15, HELMET_ARMOR = 3;
    public static final float HELMET_TOUGHNESS = 1.0f, HELMET_KNOCKBACK = 0.1f;
    public static final double HELMET_FALL_REDUCTION = 0.2;
    public static final int GATEWAY_GUARDS = 4;
    /** Soulfire Blaze natural spawning: a roll every 10 s per player in a Soul Sand Valley. */
    public static final int SOUL_ROLL_INTERVAL = 200, SOUL_NEAR_CAP = 3;
    public static final float SOUL_CHANCE = 0.2f;
    /** Soul fireball: like the Blaze's small fireball, a bit hotter. */
    public static final float SOUL_FIREBALL_DAMAGE = 6.0f, SOUL_FIRE_SECONDS = 6.0f;

    private EndNetherLogic() {}

    /** Duration of the bullet's High Gravity: longer when the target already has it. */
    public static int bulletEffectTicks(boolean alreadyHeavy) {
        return alreadyHeavy ? BULLET_EFFECT_REPEAT_TICKS : BULLET_EFFECT_TICKS;
    }

    /** True while a bullet of this age ignores hits (no knocking it away, no shooting it down). */
    public static boolean bulletShielded(int ageTicks) {
        return ageTicks < BULLET_SHIELD_TICKS;
    }

    /** Ticks until the next shot: Vanilla Shulker's 20 + roll x 10 (roll 0..9), divided by {@link #FIRE_RATE}. */
    public static int shotDelay(int roll) {
        return (int) Math.round((20 + roll * 10) / FIRE_RATE);
    }

    /** The four Shellker places of an End gateway: the open cells next to the portal block, at portal height. */
    public static List<BlockPos> gatewayGuardCells(BlockPos gateway) {
        return List.of(gateway.north(), gateway.south(), gateway.east(), gateway.west());
    }

    /** A closed Shellker takes no damage (like a rolled-up armadillo), unless the source ignores invulnerability. */
    public static boolean shellBlocks(boolean closed, boolean bypassesInvulnerability) {
        return closed && !bypassesInvulnerability;
    }

    /** Clone chance when a Shellker bullet hits an open Shellker: Vanilla's Shulker rule (fails more often in crowds). */
    public static boolean cloneSucceeds(int nearbyShellkers, float roll) {
        return roll >= (nearbyShellkers - 1) / 5.0f;
    }

    /** Motion after one tick of travel under High Gravity. */
    public static Vec3 heavyMotion(Vec3 v, boolean fallFlying, boolean levitating) {
        if (fallFlying) {
            double y = (v.y > 0 ? v.y * UPWARD_FACTOR : v.y) - ELYTRA_SINK;
            return new Vec3(v.x * ELYTRA_DRAG, y, v.z * ELYTRA_DRAG);
        }
        if (levitating && v.y > 0) return new Vec3(v.x, v.y * UPWARD_FACTOR, v.z);
        return v;
    }

    /** Shellker attach face: keep the current one while its block still carries it, else any carrying face, else stay. */
    public static Direction attachFace(Direction current, java.util.function.Predicate<Direction> carries) {
        if (carries.test(current)) return current;
        for (Direction d : Direction.values()) if (carries.test(d)) return d;
        return current;
    }

    /** Soulfire Blaze natural spawn roll. */
    public static boolean maySoulSpawn(boolean enabled, boolean soulValley, boolean soulGround, int nearby, float roll, float chance) {
        return enabled && soulValley && soulGround && nearby < SOUL_NEAR_CAP && roll < chance;
    }
}
