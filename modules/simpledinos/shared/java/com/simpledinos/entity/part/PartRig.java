package com.simpledinos.entity.part;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.AABB;

/**
 * Part hitboxes of one dinosaur kind (a-dino36b, owner DINO36: "the heads may not stick in people or walls"). The core hitbox stays a
 * Vanilla box; the head and the tail are extra boxes computed from the model's bones. Both sides use {@link #pose}: the server to test
 * which head and tail pose is free, the client model to draw exactly that pose.
 *
 * <ul>
 *   <li>look: the neck and the head share the look yaw ({@code neckLook}, {@code headLook}), the head takes {@code headPitch} of the pitch;</li>
 *   <li>tuck 0..1: the neck leans back by {@code tuckNeck} and the head turns by {@code tuckHead} (rest = 0, fully pulled in = 1);</li>
 *   <li>bend -2..2: every tail segment turns sideways by {@code bend x bendStep}.</li>
 * </ul>
 */
public final class PartRig {
    public static final int TUCK_STEPS = 4;
    public static final int BEND_STEPS = 2;

    public final Skeleton skeleton;
    /** Model scale of the grown-up dinosaur (the renderer draws this x growth). */
    public final float modelScale;
    private final int neck, head;
    private final int[] headBones, tailBones;
    private final float neckLook, headLook, headPitch, tuckNeck, tuckHead, bendStep;
    /** How far the stretched head reaches in front of the entity position, grown up, in blocks. */
    public final double reach;

    public PartRig(Skeleton skeleton, float modelScale, String neck, String head, String[] headBones, String[] tailBones,
            float neckLook, float headLook, float headPitch, float tuckNeck, float tuckHead, float bendStep) {
        this.skeleton = skeleton;
        this.modelScale = modelScale;
        this.neck = skeleton.index(neck);
        this.head = skeleton.index(head);
        this.headBones = indices(skeleton, headBones);
        this.tailBones = indices(skeleton, tailBones);
        this.neckLook = neckLook;
        this.headLook = headLook;
        this.headPitch = headPitch;
        this.tuckNeck = tuckNeck;
        this.tuckHead = tuckHead;
        this.bendStep = bendStep;
        List<AABB> boxes = new ArrayList<>();
        headBoxes(pose(0.0F, 0.0F, 0.0F, 0.0F), 1.0F, 0.0F, 0.0, 0.0, 0.0, boxes);
        double front = 0.0;
        for (AABB b : boxes) front = Math.max(front, b.maxZ);
        this.reach = front;
    }

    private static int[] indices(Skeleton skeleton, String[] names) {
        int[] out = new int[names.length];
        for (int i = 0; i < names.length; i++) out[i] = skeleton.index(names[i]);
        return out;
    }

    public int neck() {
        return neck;
    }

    public int head() {
        return head;
    }

    public int[] tailBones() {
        return tailBones.clone();
    }

    /**
     * Extra rotations per bone (radians, {x, y, z}; null = rest) for a look yaw/pitch in degrees (net head yaw = head minus body),
     * a tuck 0..1 and a tail bend in steps (-2..2, fractions while blending).
     */
    public float[][] pose(float lookYawDeg, float lookPitchDeg, float tuck, float bend) {
        float[][] out = new float[skeleton.size()][];
        float yaw = (float) Math.toRadians(lookYawDeg), pitch = (float) Math.toRadians(lookPitchDeg);
        out[neck] = new float[] {tuckNeck * tuck, yaw * neckLook, 0.0F};
        out[head] = new float[] {tuckHead * tuck + pitch * headPitch, yaw * headLook, 0.0F};
        for (int t : tailBones) out[t] = new float[] {0.0F, bend * bendStep, 0.0F};
        return out;
    }

    /** World boxes of the head chain (neck, head, jaw) in {@code pose}; {@code growth} = growth scale (0.25..1) x attribute scale. */
    public void headBoxes(float[][] pose, float growth, float bodyYaw, double x, double y, double z, List<AABB> out) {
        skeleton.boxes(pose, headBones, (double) modelScale * growth, bodyYaw, x, y, z, out);
    }

    /** World boxes of the tail segments in {@code pose}. */
    public void tailBoxes(float[][] pose, float growth, float bodyYaw, double x, double y, double z, List<AABB> out) {
        skeleton.boxes(pose, tailBones, (double) modelScale * growth, bodyYaw, x, y, z, out);
    }
}
