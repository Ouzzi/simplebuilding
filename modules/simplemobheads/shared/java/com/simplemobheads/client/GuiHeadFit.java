package com.simplemobheads.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.special.SkullSpecialRenderer;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

/**
 * IG31-10: how a head is fitted into an inventory slot. Generic, from the model's bounding box (no per-head numbers): the 8 corners
 * of the box (centred on the block middle, bottom on the floor, as {@link MobHeadModels} poses every head, times the item's local
 * scale) go through the GUI transform of the item model and are projected to the screen; the head is then scaled uniformly so that
 * its larger screen extent is {@value #TARGET} (a Vanilla 8 px head measures 0.71, a slot is 1.0) and shifted so that the
 * box's centre sits on the slot's centre. Small heads grow a little ({@value #MAX_SCALE} at most), big ones shrink.
 */
public final class GuiHeadFit {
    static final float TARGET = 0.74F;
    static final float MIN_SCALE = 0.3F;
    static final float MAX_SCALE = 1.25F;

    private GuiHeadFit() {
    }

    /** {scale, shiftX, shiftY} in screen units after the item's own GUI transform. */
    public static float[] compute(SkullSpecialRenderer renderer, ItemTransform gui, Matrix4fc local) {
        float[] min = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        float[] max = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        renderer.getExtents(v -> {
            min[0] = Math.min(min[0], v.x());
            min[1] = Math.min(min[1], v.y());
            min[2] = Math.min(min[2], v.z());
            max[0] = Math.max(max[0], v.x());
            max[1] = Math.max(max[1], v.y());
            max[2] = Math.max(max[2], v.z());
        });
        if (min[0] > max[0]) {
            return new float[] {1.0F, 0.0F, 0.0F};
        }
        // block space: the skull model hangs from y = 0 downwards in model space and is turned over when drawn
        float halfX = (max[0] - min[0]) / 2.0F;
        float halfZ = (max[2] - min[2]) / 2.0F;
        float height = max[1] - min[1];
        PoseStack.Pose pose = new PoseStack.Pose();
        gui.apply(false, pose);
        Matrix4f m = new Matrix4f(pose.pose()).mul(local);
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        Vector3f p = new Vector3f();
        for (int corner = 0; corner < 8; corner++) {
            p.set((corner & 1) == 0 ? -halfX : halfX, (corner & 2) == 0 ? 0.0F : height, (corner & 4) == 0 ? -halfZ : halfZ);
            m.transformPosition(p);
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
        }
        float extent = Math.max(maxX - minX, maxY - minY);
        float scale = extent <= 0.0F ? 1.0F : Math.max(MIN_SCALE, Math.min(MAX_SCALE, TARGET / extent));
        return new float[] {scale, -scale * (minX + maxX) / 2.0F, -scale * (minY + maxY) / 2.0F};
    }
}
