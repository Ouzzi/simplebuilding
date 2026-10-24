package com.simplebuilding.modules.simpleoptserver;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.npc.villager.AbstractVillager;

/**
 * S5b activation range, own implementation (ServerCore describes "activation ranges: entities far from players tick less
 * often"). Changes gameplay, so the area defaults off. A mob with no living player within {@code range} blocks runs its
 * full tick only every {@code interval}-th tick; Vanilla's common tick (age counter, invulnerability) always runs.
 * Mobs whose state would visibly drift stay fully active: vehicles, leashed, with a target, hurt, airborne, in water or
 * lava, burning, babies, with active potion effects, in love or on breeding cooldown, hens, tamed, villagers and traders (farms, trading), bosses.
 */
public final class ActivationRange {
    public static final int MIN_RANGE = 16, MAX_RANGE = 128, MAX_INTERVAL = 20;

    public record Params(int range, int interval) {
        public static final Params DEFAULTS = new Params(32, 4);
    }

    private ActivationRange() {}

    /** Ticks are spread over the interval by entity id, so not every far mob runs in the same tick. */
    public static boolean offBeat(int tickCount, int id, int interval) {
        return Math.floorMod(tickCount + id, interval) != 0;
    }

    /** Stays fully active whatever the distance. */
    public static boolean exempt(Entity e) {
        if (!(e instanceof Mob mob)) return true;
        if (mob.isVehicle() || mob.isLeashed() || mob.getTarget() != null || mob.hurtTime > 0 || !mob.onGround()
                || mob.isInWater() || mob.isInLava() || mob.isOnFire() || mob.isBaby()
                || !mob.getActiveEffects().isEmpty()) return true;
        // adults with a breeding cooldown (age > 0) and hens (egg timer runs in aiStep) must not slow down
        return mob instanceof Animal a && (a.isInLove() || a.getAge() != 0) || mob instanceof Chicken || mob instanceof TamableAnimal t && t.isTame()
                || mob instanceof AbstractVillager || mob instanceof WitherBoss || mob instanceof EnderDragon;
    }

    /** Whether this tick of {@code e} is skipped (cheap checks first). */
    public static boolean skip(Entity e, ServerLevel level, Params p) {
        return offBeat(e.tickCount, e.getId(), p.interval()) && !exempt(e)
                && !level.hasNearbyAlivePlayer(e.getX(), e.getY(), e.getZ(), p.range());
    }
}
