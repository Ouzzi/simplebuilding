package com.simplebuilding.blocks.custom;

import com.simplebuilding.config.ServerTuning;
import com.simplebuilding.config.ServerTuningConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

/**
 * What the Astral and Nihil rails do to a minecart (docs/ai/PLAN-ASTRAL-NIHIL-SCHIENEN-2026-10-02.md). Called once per
 * minecart tick at the start of vanilla's {@code moveAlongTrack} (old and experimental behaviour alike, see
 * {@code MinecartBehaviorEndRailMixin}) and from {@code AbstractMinecart#getMaxSpeed} ({@code AbstractMinecartEndRailMixin}).
 *
 * <ul>
 *   <li>Flat Astral rail: the top speed is {@code max(vanilla, astralRailMaxSpeed)}; everywhere else vanilla's stays.</li>
 *   <li>Powered Astral rail: {@code v -> v + boost * (1 - (v / cap)^2)}. The boost is held at or below {@code cap / 2},
 *       which keeps the step strictly increasing on [0, cap], so the speed approaches the cap and never reaches it;
 *       the faster the cart, the less every rail adds. Vanilla friction still applies afterwards.</li>
 *   <li>Powered Nihil rail: {@code v -> 0.8 v - brake}, below vanilla's 0.03 the cart stops and is held.</li>
 * </ul>
 */
public final class EndRailPhysics {
    /** Vanilla's powered rail adds this much per tick (Old/NewMinecartBehavior). */
    public static final double VANILLA_POWERED_RAIL_BOOST = 0.06;
    /** Vanilla's top speed in the default (old) minecart behaviour, blocks per tick. */
    public static final double VANILLA_MAX_SPEED = 0.4;
    /** Below this speed vanilla's halting powered rail stops a cart; the Nihil rail uses the same threshold. */
    public static final double STOP_BELOW = 0.03;
    /** Share of its speed a cart loses per tick on a powered Nihil rail, on top of the configured brake. */
    public static final double BRAKE_FRICTION = 0.2;
    /** The old behaviour moves a ridden cart by 0.75 of its delta; the boost target compensates for it. */
    private static final double RIDDEN_SCALE = 0.75;

    public static final int MIN_MAX_SPEED = 8, MAX_MAX_SPEED = 20;
    public static final double MIN_BOOST = 0.07, MAX_BOOST = 0.25;
    public static final double MIN_BRAKE = 0.02, MAX_BRAKE = 0.4;

    private EndRailPhysics() {
    }

    public static boolean enabled() {
        if (!com.simplebuilding.version.McVersion.END_RAILS) return false;
        ServerTuningConfig.Features f = ServerTuning.get().features;
        return f.endRails && f.endSignals;
    }

    /** Raised top speed on Astral rails in blocks per tick, server cap 8..20 blocks per second. */
    public static double maxSpeedPerTick() {
        return Math.clamp(ServerTuning.get().machines.astralRailMaxSpeed, MIN_MAX_SPEED, MAX_MAX_SPEED) / 20.0;
    }

    /** Boost of a powered Astral rail per tick, server cap 0.07..0.25 (always above the powered rail's 0.06). */
    public static double boost() {
        return ServerTuningConfig.clamp(ServerTuning.get().machines.astralRailBoost, MIN_BOOST, MAX_BOOST, 0.12);
    }

    /** Constant part of the Nihil brake per tick, server cap 0.02..0.4. */
    public static double brake() {
        return ServerTuningConfig.clamp(ServerTuning.get().machines.nihilRailBrake, MIN_BRAKE, MAX_BRAKE, 0.08);
    }

    /** One tick on a powered Astral rail: approaches {@code cap} from below, never reaches or passes it. */
    public static double boosted(double speed, double cap, double boost) {
        if (cap <= 0) return speed;
        if (speed >= cap) return cap;
        double step = Math.min(boost, cap / 2);
        double ratio = Math.max(0, speed) / cap;
        return Math.max(0, speed) + step * (1 - ratio * ratio);
    }

    /** One tick on a powered Nihil rail: friction plus a constant brake, a stop below {@link #STOP_BELOW}. */
    public static double braked(double speed, double brake) {
        double next = speed * (1 - BRAKE_FRICTION) - brake;
        return next < STOP_BELOW ? 0 : next;
    }

    /** The raised top speed for the rail the cart is on, or 0 when vanilla's applies (no flat Astral rail, water). */
    public static double raisedMaxSpeed(AbstractMinecart cart) {
        if (!enabled() || cart.isInWater()) return 0;
        BlockState state = cart.level().getBlockState(cart.getCurrentBlockPosOrRailBelow());
        return state.getBlock() instanceof EndRailBlock rail && rail.astral() && !state.getValue(EndRailBlock.SHAPE).isSlope()
                ? maxSpeedPerTick() : 0;
    }

    /**
     * Applies the rail under the cart to its delta movement, before vanilla moves it along the track.
     *
     * @param vanillaCap the behaviour's own top speed (0.4, or the experimental game rule), without the Astral raise
     * @param oldBehaviour true for vanilla's default behaviour, which moves a ridden cart by 0.75 of its delta
     */
    public static void beforeTrackMove(AbstractMinecart cart, double vanillaCap, boolean oldBehaviour) {
        if (!enabled()) return;
        BlockPos pos = cart.getCurrentBlockPosOrRailBelow();
        BlockState state = cart.level().getBlockState(pos);
        if (!(state.getBlock() instanceof EndRailBlock rail)) return;
        Vec3 delta = cart.getDeltaMovement();
        Vec3 flat = new Vec3(delta.x, 0, delta.z);
        double speed = flat.length();
        boolean powered = state.getValue(EndRailBlock.POWERED);
        if (!rail.astral()) {
            if (!powered || speed <= 0) return;
            double next = braked(speed, brake());
            cart.setDeltaMovement(flat.scale(next / speed).add(0, delta.y, 0));
            return;
        }
        RailShape shape = state.getValue(EndRailBlock.SHAPE);
        double top = shape.isSlope() || cart.isInWater() ? vanillaCap : Math.max(vanillaCap, maxSpeedPerTick());
        double cap = top / (oldBehaviour && cart.isVehicle() ? RIDDEN_SCALE : 1.0);
        Vec3 direction;
        if (speed > 0.01) {
            direction = flat.scale(1 / speed);
            speed = Math.min(speed, cap);
        } else {
            direction = powered ? kickDirection(cart, pos, shape) : null;
            if (direction == null) return;
            speed = 0;
        }
        double next = powered ? boosted(speed, cap, boost()) : speed;
        cart.setDeltaMovement(direction.x * next, delta.y, direction.z * next);
    }

    /** Like the powered rail from standstill: away from a solid block at one end of a flat rail. */
    private static Vec3 kickDirection(AbstractMinecart cart, BlockPos pos, RailShape shape) {
        if (shape == RailShape.EAST_WEST) {
            if (cart.isRedstoneConductor(pos.west())) return new Vec3(1, 0, 0);
            if (cart.isRedstoneConductor(pos.east())) return new Vec3(-1, 0, 0);
        } else if (shape == RailShape.NORTH_SOUTH) {
            if (cart.isRedstoneConductor(pos.north())) return new Vec3(0, 0, 1);
            if (cart.isRedstoneConductor(pos.south())) return new Vec3(0, 0, -1);
        }
        return null;
    }
}
