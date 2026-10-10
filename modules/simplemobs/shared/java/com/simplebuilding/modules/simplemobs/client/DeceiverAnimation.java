package com.simplebuilding.modules.simplemobs.client;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

/** Keyframe animations in the style of the Copper Golem: walk, idle, summon, teleport, drink, unmask. */
public final class DeceiverAnimation {
    private DeceiverAnimation() {}

    private static Keyframe r(float t, float x, float y, float z) {
        return new Keyframe(t, KeyframeAnimations.degreeVec(x, y, z), AnimationChannel.Interpolations.CATMULLROM);
    }

    private static Keyframe s(float t, float v) {
        return new Keyframe(t, KeyframeAnimations.scaleVec(v, v, v), AnimationChannel.Interpolations.CATMULLROM);
    }

    private static Keyframe p(float t, float x, float y, float z) {
        return new Keyframe(t, KeyframeAnimations.posVec(x, y, z), AnimationChannel.Interpolations.CATMULLROM);
    }

    private static AnimationChannel rot(Keyframe... k) { return new AnimationChannel(AnimationChannel.Targets.ROTATION, k); }
    private static AnimationChannel scale(Keyframe... k) { return new AnimationChannel(AnimationChannel.Targets.SCALE, k); }
    private static AnimationChannel pos(Keyframe... k) { return new AnimationChannel(AnimationChannel.Targets.POSITION, k); }

    public static final AnimationDefinition WALK = AnimationDefinition.Builder.withLength(0.8f).looping()
            .addAnimation("right_leg", rot(r(0f, 35f, 0f, 0f), r(0.4f, -35f, 0f, 0f), r(0.8f, 35f, 0f, 0f)))
            .addAnimation("left_leg", rot(r(0f, -35f, 0f, 0f), r(0.4f, 35f, 0f, 0f), r(0.8f, -35f, 0f, 0f)))
            .addAnimation("right_arm", rot(r(0f, -25f, 0f, 6f), r(0.4f, 25f, 0f, 6f), r(0.8f, -25f, 0f, 6f)))
            .addAnimation("left_arm", rot(r(0f, 25f, 0f, -6f), r(0.4f, -25f, 0f, -6f), r(0.8f, 25f, 0f, -6f)))
            .addAnimation("body", rot(r(0f, 6f, 5f, 0f), r(0.2f, 6f, 0f, -3f), r(0.4f, 6f, -5f, 0f), r(0.6f, 6f, 0f, 3f), r(0.8f, 6f, 5f, 0f)))
            .addAnimation("body", pos(p(0f, 0f, 0f, 0f), p(0.2f, 0f, 0.5f, 0f), p(0.4f, 0f, 0f, 0f), p(0.6f, 0f, 0.5f, 0f), p(0.8f, 0f, 0f, 0f)))
            .addAnimation("cloak", rot(r(0f, 18f, 0f, 0f), r(0.4f, 28f, 0f, 0f), r(0.8f, 18f, 0f, 0f)))
            .build();

    public static final AnimationDefinition IDLE = AnimationDefinition.Builder.withLength(4f).looping()
            .addAnimation("body", rot(r(0f, 0f, 0f, 0f), r(2f, 2f, 0f, 0f), r(4f, 0f, 0f, 0f)))
            .addAnimation("head", rot(r(0f, 0f, 0f, -3f), r(2f, 2f, 0f, 3f), r(4f, 0f, 0f, -3f)))
            .addAnimation("right_arm", rot(r(0f, 0f, 0f, 4f), r(2f, 3f, 0f, 8f), r(4f, 0f, 0f, 4f)))
            .addAnimation("left_arm", rot(r(0f, 0f, 0f, -4f), r(2f, 3f, 0f, -8f), r(4f, 0f, 0f, -4f)))
            .addAnimation("cloak", rot(r(0f, 4f, 0f, 0f), r(2f, 9f, 0f, 0f), r(4f, 4f, 0f, 0f)))
            .build();

    /** Raises both arms, leans back, shakes, then slams down: the wave appears at the end. */
    public static final AnimationDefinition SUMMON = AnimationDefinition.Builder.withLength(2.5f)
            .addAnimation("right_arm", rot(r(0f, 0f, 0f, 4f), r(0.6f, -150f, 0f, 20f), r(1.9f, -160f, 0f, 24f), r(2.1f, -20f, 0f, 6f), r(2.5f, 0f, 0f, 4f)))
            .addAnimation("left_arm", rot(r(0f, 0f, 0f, -4f), r(0.6f, -150f, 0f, -20f), r(1.9f, -160f, 0f, -24f), r(2.1f, -20f, 0f, -6f), r(2.5f, 0f, 0f, -4f)))
            .addAnimation("body", rot(r(0f, 0f, 0f, 0f), r(0.6f, -12f, 0f, 0f), r(1.2f, -14f, 3f, 0f), r(1.6f, -14f, -3f, 0f), r(1.9f, -14f, 0f, 0f), r(2.1f, 18f, 0f, 0f), r(2.5f, 0f, 0f, 0f)))
            .addAnimation("head", rot(r(0f, 0f, 0f, 0f), r(0.6f, -25f, 0f, 0f), r(1.9f, -28f, 0f, 0f), r(2.1f, 15f, 0f, 0f), r(2.5f, 0f, 0f, 0f)))
            .addAnimation("cloak", rot(r(0f, 4f, 0f, 0f), r(0.6f, 30f, 0f, 0f), r(1.9f, 38f, 0f, 0f), r(2.1f, -10f, 0f, 0f), r(2.5f, 4f, 0f, 0f)))
            .build();

    /** Pop-in at the arrival spot: squashes small, overshoots, settles. */
    public static final AnimationDefinition TELEPORT = AnimationDefinition.Builder.withLength(1f)
            .addAnimation("body", scale(s(0f, 0.05f), s(0.2f, 1.25f), s(0.35f, 0.92f), s(0.5f, 1f), s(1f, 1f)))
            .addAnimation("right_leg", scale(s(0f, 0.05f), s(0.2f, 1.2f), s(0.35f, 0.95f), s(0.5f, 1f), s(1f, 1f)))
            .addAnimation("left_leg", scale(s(0f, 0.05f), s(0.2f, 1.2f), s(0.35f, 0.95f), s(0.5f, 1f), s(1f, 1f)))
            .addAnimation("cloak", rot(r(0f, 40f, 0f, 0f), r(0.4f, -8f, 0f, 0f), r(1f, 4f, 0f, 0f)))
            .addAnimation("head", rot(r(0f, 0f, 0f, 0f), r(0.3f, 0f, 25f, 0f), r(0.6f, 0f, -20f, 0f), r(1f, 0f, 0f, 0f)))
            .build();

    public static final AnimationDefinition DRINK = AnimationDefinition.Builder.withLength(1.2f)
            .addAnimation("right_arm", rot(r(0f, 0f, 0f, 4f), r(0.3f, -115f, 25f, 0f), r(0.9f, -115f, 25f, 0f), r(1.2f, 0f, 0f, 4f)))
            .addAnimation("head", rot(r(0f, 0f, 0f, 0f), r(0.4f, -22f, 0f, 0f), r(0.9f, -22f, 0f, 0f), r(1.2f, 0f, 0f, 0f)))
            .addAnimation("body", rot(r(0f, 0f, 0f, 0f), r(0.4f, -6f, 0f, 0f), r(0.9f, -6f, 0f, 0f), r(1.2f, 0f, 0f, 0f)))
            .build();

    /** Hit while disguised: recoils, arms flung out, cloak flares. */
    public static final AnimationDefinition UNMASK = AnimationDefinition.Builder.withLength(0.8f)
            .addAnimation("body", rot(r(0f, 0f, 0f, 0f), r(0.1f, -24f, 0f, 0f), r(0.5f, -10f, 0f, 0f), r(0.8f, 0f, 0f, 0f)))
            .addAnimation("head", rot(r(0f, 0f, 0f, 0f), r(0.1f, -20f, 0f, 12f), r(0.3f, -10f, 0f, -12f), r(0.5f, -5f, 0f, 8f), r(0.8f, 0f, 0f, 0f)))
            .addAnimation("right_arm", rot(r(0f, 0f, 0f, 4f), r(0.1f, -20f, 0f, 70f), r(0.8f, 0f, 0f, 4f)))
            .addAnimation("left_arm", rot(r(0f, 0f, 0f, -4f), r(0.1f, -20f, 0f, -70f), r(0.8f, 0f, 0f, -4f)))
            .addAnimation("cloak", rot(r(0f, 4f, 0f, 0f), r(0.1f, 48f, 0f, 0f), r(0.8f, 4f, 0f, 0f)))
            .build();
}
