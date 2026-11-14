package com.simpledinos.entity.part;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.AABB;

/**
 * Part hitboxes of one dinosaur kind (a-dino36b, owner DINO36: "the heads may not stick in people or walls"). The core hitbox stays a
 * Vanilla box; the head and the tail are extra boxes computed from the model's bones. N45 (owner DINOREST3: "only minimise the
 * hitboxes, fewer, not too many"): ONE head box (the hull of every cube of the head chain - neck, head, jaw, horns, frill, crest) and ONE
 * tail box (the hull of the tail segments from the second on plus what hangs on them; the first segment sits right behind the core box).
 * The hull keeps the extremes of the cubes (reach, height, lowest point), so pulling in, bending and the solid head behave as before.
 * A new kind (Utahraptor) passes its head chain and tail chain the same way and gets the same two boxes. Both sides use {@link #pose}: the server to test
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
    private final int[] headBones, tailBones, tailBoxBones;
    private final float neckLook, headLook, headPitch, tuckNeck, tuckHead, bendStep;
    /** How far the stretched head reaches in front of the entity position, grown up, in blocks. */
    public final double reach;
    /**
     * a-dinorest1: the head is solid like the body - it stops the walk at walls (leaves and steps excepted) and pushes living things
     * aside like the core box does. For kinds whose head cannot be pulled in far enough (Triceratops: the frill and horns sit in front
     * of the shoulders); the T. rex pulls its head in instead (false).
     */
    public final boolean solidHead;

    public PartRig(Skeleton skeleton, float modelScale, String neck, String head, String[] headBones, String[] tailBones,
            float neckLook, float headLook, float headPitch, float tuckNeck, float tuckHead, float bendStep) {
        this(skeleton, modelScale, neck, head, headBones, tailBones, neckLook, headLook, headPitch, tuckNeck, tuckHead, bendStep, false);
    }

    public PartRig(Skeleton skeleton, float modelScale, String neck, String head, String[] headBones, String[] tailBones,
            float neckLook, float headLook, float headPitch, float tuckNeck, float tuckHead, float bendStep, boolean solidHead) {
        this(skeleton, modelScale, neck, head, headBones, tailBones, new String[0], neckLook, headLook, headPitch, tuckNeck, tuckHead, bendStep,
                solidHead);
    }

    /**
     * a-dinorest2: {@code tailExtra} are bones hanging on the tail (the Stegosaurus' spikes): their boxes count as tail boxes and they
     * follow the bend of their parents, but they are not bent themselves.
     */
    public PartRig(Skeleton skeleton, float modelScale, String neck, String head, String[] headBones, String[] tailBones, String[] tailExtra,
            float neckLook, float headLook, float headPitch, float tuckNeck, float tuckHead, float bendStep, boolean solidHead) {
        this.solidHead = solidHead;
        this.skeleton = skeleton;
        this.modelScale = modelScale;
        this.neck = skeleton.index(neck);
        this.head = skeleton.index(head);
        this.headBones = indices(skeleton, headBones);
        this.tailBones = indices(skeleton, tailBones);
        int from = tailBones.length > 1 ? 1 : 0; // N45: the tail box starts at the second segment
        String[] all = new String[tailBones.length - from + tailExtra.length];
        System.arraycopy(tailBones, from, all, 0, tailBones.length - from);
        System.arraycopy(tailExtra, 0, all, tailBones.length - from, tailExtra.length);
        this.tailBoxBones = indices(skeleton, all);
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

    /** The ONE world box of the head chain (neck, head, jaw ...) in {@code pose}; {@code growth} = growth scale (0.25..1) x attribute scale. */
    public void headBoxes(float[][] pose, float growth, float bodyYaw, double x, double y, double z, List<AABB> out) {
        hull(pose, headBones, growth, bodyYaw, x, y, z, out);
    }

    /** The ONE world box of the tail (segments from the second on and what hangs on them) in {@code pose}. */
    public void tailBoxes(float[][] pose, float growth, float bodyYaw, double x, double y, double z, List<AABB> out) {
        hull(pose, tailBoxBones, growth, bodyYaw, x, y, z, out);
    }

    /** N45: adds the axis-aligned hull of the turned cubes of {@code bones} as one box (nothing for no cubes). */
    private void hull(float[][] pose, int[] bones, float growth, float bodyYaw, double x, double y, double z, List<AABB> out) {
        List<AABB> cubes = new ArrayList<>();
        skeleton.boxes(pose, bones, (double) modelScale * growth, bodyYaw, x, y, z, cubes);
        if (cubes.isEmpty()) return;
        AABB hull = cubes.get(0);
        for (int i = 1; i < cubes.size(); i++) hull = hull.minmax(cubes.get(i));
        out.add(hull);
    }
}
