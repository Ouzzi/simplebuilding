package com.simplebuilding.items.custom;

import com.simplebuilding.version.McVersion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

/**
 * How a building core moves in the first-person hand when it is used (owner addition 11): a weighted roll picks
 * one motion - the cooler, the rarer - and the ore transmutation has its own, longer one. Pure state and math
 * without client classes; the client (use/useOn on the client side, {@code CoreMotionPayload} for the ore)
 * starts a motion here, {@code HeldItemRendererMixin} reads {@link #pose} every frame and moves the item.
 * Behind {@link McVersion#CORE_MOTIONS} (26.3) and the client config {@code tools.enableCoreAnimations}.
 *
 * <p>Every pose starts and ends at rest (identity), so a motion never snaps; turns are whole revolutions.
 */
public final class CoreHandMotion {
    /** The motions; {@code weight} out of 100 for the roll (FORGE is never rolled), {@code ticks} = length. */
    public enum Motion {
        /** Two heartbeats: the core swells and shrinks. */
        PULSE(45, 14),
        /** One smooth turn around its upright axis, lifted a little. */
        SPIN(30, 16),
        /** Floats up, hovers turning once, settles back into the hand. */
        RISE(15, 24),
        /** Thrown forward spinning flat on a curve and caught again. */
        BOOMERANG(10, 26),
        /** Ore transmutation: rises, spins faster and faster, slams down and springs back. */
        FORGE(0, 40);

        public final int weight;
        public final int ticks;

        Motion(int weight, int ticks) {
            this.weight = weight;
            this.ticks = ticks;
        }
    }

    private static final Motion[] ACTIVE = new Motion[2];
    private static final int[] START = new int[2];

    private CoreHandMotion() {
    }

    /** The motion for a roll in 0..99 (weights 45/30/15/10). */
    public static Motion fromRoll(int roll) {
        int bound = 0;
        for (Motion motion : Motion.values()) {
            bound += motion.weight;
            if (roll < bound) return motion;
        }
        return Motion.BOOMERANG;
    }

    public static Motion roll(RandomSource random) {
        return fromRoll(random.nextInt(100));
    }

    /** Starts {@code motion} in {@code hand} (client thread; ignored on lines without the feature). */
    public static void start(Player player, InteractionHand hand, Motion motion) {
        if (!McVersion.CORE_MOTIONS || player == null || motion == null) return;
        ACTIVE[hand.ordinal()] = motion;
        START[hand.ordinal()] = player.tickCount;
    }

    /** A rolled motion; an ore animation still running is not cut short by a normal use. */
    public static void startRandom(Player player, InteractionHand hand) {
        if (player == null) return;
        if (ACTIVE[hand.ordinal()] == Motion.FORGE && progress(player.tickCount, 0.0F, hand) >= 0.0F) return;
        start(player, hand, roll(player.getRandom()));
    }

    /** 0..1 through the running motion of {@code hand}, or -1 when none runs (it is then forgotten). */
    public static float progress(int tickCount, float partialTick, InteractionHand hand) {
        Motion motion = ACTIVE[hand.ordinal()];
        if (motion == null) return -1.0F;
        float t = (tickCount - START[hand.ordinal()] + partialTick) / motion.ticks;
        if (t < 0.0F || t >= 1.0F) {
            ACTIVE[hand.ordinal()] = null;
            return -1.0F;
        }
        return t;
    }

    /** The current pose of {@code hand}, or null when nothing runs. */
    public static float[] currentPose(int tickCount, float partialTick, InteractionHand hand) {
        Motion motion = ACTIVE[hand.ordinal()];
        float t = progress(tickCount, partialTick, hand);
        return t < 0.0F ? null : pose(motion, t);
    }

    /** Index of the pose values: translation x/y/z (blocks), rotations x/y/z (degrees), uniform scale. */
    public static final int TX = 0, TY = 1, TZ = 2, RX = 3, RY = 4, RZ = 5, SCALE = 6;

    /** The pose of {@code motion} at {@code t} in 0..1 (hand space: -z is forward, +y up). */
    public static float[] pose(Motion motion, float t) {
        float[] p = {0, 0, 0, 0, 0, 0, 1};
        double bell = Math.sin(Math.PI * t);
        switch (motion) {
            case PULSE -> {
                double beat = Math.sin(2 * Math.PI * t);
                p[SCALE] = (float) (1 + 0.3 * beat * beat * (1 - 0.4 * t));
                p[TY] = (float) (0.03 * bell);
            }
            case SPIN -> {
                p[RY] = (float) (360 * easeInOut(t));
                p[TY] = (float) (0.08 * bell);
            }
            case RISE -> {
                double hover = t < 0.35 ? smooth(t / 0.35) : t < 0.7 ? 1 : 1 - smooth((t - 0.7) / 0.3);
                double bob = t >= 0.35 && t < 0.7 ? 0.025 * Math.sin(2 * Math.PI * (t - 0.35) / 0.35) : 0;
                p[TY] = (float) (0.42 * hover + bob);
                p[TZ] = (float) (-0.12 * hover);
                p[RY] = (float) (360 * smooth(t));
                p[SCALE] = (float) (1 + 0.1 * hover);
            }
            case BOOMERANG -> {
                p[TZ] = (float) (-1.3 * bell);
                p[TX] = (float) (-0.35 * Math.sin(2 * Math.PI * t));
                p[TY] = (float) (0.12 * bell);
                p[RX] = (float) (-60 * bell);
                p[RZ] = (float) (720 * smooth(t));
            }
            case FORGE -> forge(p, t);
        }
        return p;
    }

    /** Ore animation: up (0-0.3), accelerating spin with growing glow (0.3-0.75), slam (0.75-0.85), springing back. */
    private static void forge(float[] p, float t) {
        if (t < 0.3F) {
            double up = smooth(t / 0.3);
            p[TY] = (float) (0.35 * up);
            p[TZ] = (float) (-0.1 * up);
        } else if (t < 0.75F) {
            double k = (t - 0.3) / 0.45;
            p[TY] = (float) (0.35 + 0.02 * Math.sin(6 * Math.PI * k));
            p[TZ] = -0.1F;
            p[RY] = (float) (1080 * k * k);
            p[SCALE] = (float) (1 + 0.25 * smooth(k));
        } else if (t < 0.85F) {
            double k = (t - 0.75) / 0.1;
            double fall = k * k;
            p[TY] = (float) (0.35 - 0.47 * fall);
            p[TZ] = (float) (-0.1 * (1 - fall));
            p[RX] = (float) (-30 * fall);
            p[SCALE] = (float) (1.25 - 0.1 * fall);
        } else {
            double k = (t - 0.85) / 0.15;
            double spring = Math.cos(3 * Math.PI * k) * (1 - k);
            p[TY] = (float) (-0.12 * spring * (1 - k));
            p[RX] = (float) (-30 * (1 - smooth(k)));
            p[SCALE] = (float) (1 + 0.15 * (1 - smooth(k)));
        }
    }

    private static double smooth(double x) {
        x = Math.max(0, Math.min(1, x));
        return x * x * (3 - 2 * x);
    }

    private static double easeInOut(double x) {
        return x < 0.5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2;
    }
}
