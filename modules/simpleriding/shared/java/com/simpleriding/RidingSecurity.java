package com.simpleriding;

import net.minecraft.core.BlockPos;
import net.minecraft.core.PositionAndRotation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/** Per connection, never per mount: switching vehicles cannot refill a tick's budget. */
public final class RidingSecurity {
    /**
     * simpleriding-3 Option B: with {@code safety.lenientMovementBudget} the unused budget of a tick (also of an empty tick, when a
     * packet arrives late and bunches up with the next) carries over, capped at {@code BUDGET_WINDOW_TICKS - 1} ticks of budget.
     * Sustained movement stays at the per-tick cap; a single packet never exceeds one tick's cap.
     */
    public static final int BUDGET_WINDOW_TICKS = 4;
    private long tick = Long.MIN_VALUE;
    private double distance;
    /** Envelope use this tick: horizontal for ground mounts, 3D for happy ghasts and nautiluses (checked in 3D). */
    private double envelope;
    private double distanceCredit;
    private double envelopeCredit;
    private double lastCap;
    private double lastLimit;
    private int packets;
    private long jumpTick = Long.MIN_VALUE;
    private long dashTick = Long.MIN_VALUE;
    private double dashImpulse;
    private int dashMountId = -1;
    private long horseJumpTick = Long.MIN_VALUE;
    private double jumpCeiling;
    private int jumpMountId = -1;
    private long correctionTick = Long.MIN_VALUE;

    public boolean shouldCorrect(ServerPlayer player) {
        long now=player.level().getGameTime();
        if (correctionTick == now) return false;
        correctionTick=now;
        return true;
    }

    public static boolean supported(Entity e) {
        return e instanceof AbstractHorse || e instanceof AbstractNautilus
            || e.getType() == EntityTypes.PIG || e.getType() == EntityTypes.STRIDER
            || e.getType() == EntityTypes.HAPPY_GHAST;
    }

    public boolean acceptMove(ServerPlayer player, PositionAndRotation claim) {
        Entity mount = player.getRootVehicle();
        if (!supported(mount)) return mount == player; // Vanilla handles other vehicles.
        if (!mount.isAlive() || mount.getControllingPassenger() != player) return false;
        long now = player.level().getGameTime();
        if (now != tick) {
            if (Riding.CONFIG.safety.lenientMovementBudget && tick != Long.MIN_VALUE && now > tick) {
                long elapsed = Math.min(BUDGET_WINDOW_TICKS, now - tick);
                distanceCredit = carry(distanceCredit, lastCap, elapsed, distance);
                envelopeCredit = carry(envelopeCredit, lastLimit, elapsed, envelope);
            } else { distanceCredit = 0; envelopeCredit = 0; }
            tick = now; distance = 0; envelope = 0; packets = 0;
        }
        int maxPackets = Math.max(1, Math.min(20, Riding.CONFIG.safety.movementPacketsPerTick));
        if (++packets > maxPackets) return false;
        Vec3 target = claim.position();
        if (!finite(target) || !Float.isFinite(claim.yRot()) || !Float.isFinite(claim.xRot())
            || Math.abs(claim.xRot()) > 90) return false;
        double step = target.distanceTo(mount.position());
        double cap = Math.max(.5, RidingConfig.bounded(Riding.CONFIG.safety.movementDistancePerTick, 4, 4));
        var living = (LivingEntity)mount;
        double speed = living.getAttributeValue(Attributes.MOVEMENT_SPEED);
        double horizontalCap = .125 + 3 * speed;
        if (mount.getType() == EntityTypes.HAPPY_GHAST) {
            double flight=living.getAttributeValue(Attributes.FLYING_SPEED);
            // Looking straight up, ascending (+0.5) and strafing gives sqrt(1 + 1.5²).
            horizontalCap=.125+(5.0/3.0)*flight*Math.min(1,Math.sqrt(3.25)*3.9*flight)/.09;
        }
        if (mount instanceof AbstractNautilus) {
            double dash = mount.getId() == dashMountId && now >= dashTick && now - dashTick < 40 ? dashImpulse * Math.pow(.9, now - dashTick) : 0;
            horizontalCap = .125 + .325 * speed + dash;
        }
        if (mount instanceof net.minecraft.world.entity.animal.camel.Camel) {
            double dash=mount.getId()==dashMountId && now>=dashTick && now-dashTick<55 ? dashImpulse*Math.pow(.91,now-dashTick) : 0;
            horizontalCap+=dash;
        }
        Vec3 movement = target.subtract(mount.position());
        lastCap = cap;
        if (!Double.isFinite(step) || step > cap || distance + step > cap + credit(distanceCredit, cap)) return false;
        BlockPos pos = BlockPos.containing(target);
        var border=player.level().getWorldBorder();
        var box=mount.getBoundingBox().move(movement);
        if (!player.level().hasChunkAt(pos) || box.minX < border.getMinX() || box.maxX > border.getMaxX()
            || box.minZ < border.getMinZ() || box.maxZ > border.getMaxZ()
            || target.y < player.level().getMinY() || target.y >= player.level().getMaxY()) return false;
        for (double x : new double[]{box.minX,box.maxX}) for (double z : new double[]{box.minZ,box.maxZ})
            if (!player.level().hasChunkAt(BlockPos.containing(x,target.y,z))) return false;
        // Happy ghasts and nautiluses are bounded in 3D (ascent counts), ground mounts horizontally.
        double used = mount instanceof AbstractNautilus || mount.getType() == EntityTypes.HAPPY_GHAST ? step : movement.horizontalDistance();
        // The envelope comes from server attributes/accepted dash, never claimed key states or velocity.
        if (!Double.isFinite(horizontalCap)) return false;
        double limit = Math.min(cap, horizontalCap);
        lastLimit = limit;
        if (used > limit || envelope + used > limit + credit(envelopeCredit, limit)) return false;
        if (!(mount instanceof AbstractNautilus) && mount.getType() != EntityTypes.HAPPY_GHAST
            && !mount.isInWater() && !mount.isInLava() && movement.y > 1e-4) {
            boolean authorizedJump = mount instanceof AbstractHorse && mount.getId() == jumpMountId && now >= horseJumpTick && now - horseJumpTick < 60 && target.y <= jumpCeiling;
            boolean groundedStep = movement.y <= 1 && !player.level().noCollision(mount,
                mount.getBoundingBox().move(movement).move(0,-.05,0));
            // Levitation, slime blocks, explosions: the server itself lifts the mount (effect or upward server velocity), REV2-C-5.
            boolean liftedByServer = living.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION) || mount.getDeltaMovement().y > 1e-4;
            if (!authorizedJump && !groundedStep && !liftedByServer) return false;
        }
        // Retain Vanilla's collision, teleport, floating and movement checks after this guard.
        distance += step;
        envelope += used;
        return true;
    }

    /** Budget left over at the end of the last packet tick plus full budgets of skipped ticks, kept for at most the window. */
    private static double carry(double credit, double limit, long elapsedTicks, double usedLastTick) {
        double next = credit + limit * elapsedTicks - usedLastTick;
        return Double.isFinite(next) ? Math.max(0, Math.min((BUDGET_WINDOW_TICKS - 1) * limit, next)) : 0;
    }

    private static double credit(double credit, double limit) {
        return Math.min(credit, (BUDGET_WINDOW_TICKS - 1) * limit);
    }

    public void recordDash(ServerPlayer player, AbstractNautilus mount, float scale) {
        dashTick = player.level().getGameTime();
        dashMountId = mount.getId();
        dashImpulse = Math.min(4, 1.2 * mount.getAttributeValue(Attributes.MOVEMENT_SPEED)
            * RidingEffects.dashScale(mount, scale));
    }

    public boolean acceptJump(ServerPlayer player, int entityId, int charge) {
        Entity mount = player.getControlledVehicle();
        if (entityId != player.getId() || mount == null || !supported(mount)
            || mount.getControllingPassenger() != player || !mount.isAlive()
            || charge < 1 || charge > 100 || !(mount instanceof PlayerRideableJumping jumping)
            || !jumping.canJump() || jumping.getJumpCooldown() > 0) return false;
        long now = player.level().getGameTime();
        if (jumpTick == now) return false;
        jumpTick = now;
        if (mount instanceof AbstractHorse) {
            if (player.level().noCollision(mount,mount.getBoundingBox().move(0,-.05,0))) return false;
            horseJumpTick=now;
            jumpMountId=mount.getId();
            double power=((LivingEntity)mount).getAttributeValue(Attributes.JUMP_STRENGTH);
            if (mount instanceof net.minecraft.world.entity.animal.camel.Camel) {
                power*=1.4285;
                dashTick=now;
                dashMountId=mount.getId();
                dashImpulse=Math.min(4,22.2222*jumping.getPlayerJumpPendingScale(charge)*((LivingEntity)mount).getAttributeValue(Attributes.MOVEMENT_SPEED));
            }
            jumpCeiling=mount.getY()+Math.min(16,power*power/.16+1);
        }
        return true;
    }

    private static boolean finite(Vec3 v) {
        return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }
}
