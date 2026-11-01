package com.simplemobheads.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplemobheads.HeadSlotFit;
import net.minecraft.client.renderer.special.SkullSpecialRenderer;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

/**
 * IG31-10: how a head is fitted into an inventory slot. Generic, from the model itself (no per-head numbers): every vertex of the
 * head's model ({@link SkullSpecialRenderer#getExtents}) goes through the item's local transform and its GUI transform, exactly as
 * Vanilla's {@code ItemStackRenderState#visitExtents} does, and the screen box of those points is fitted by {@link HeadSlotFit}
 * (larger extent {@value HeadSlotFit#TARGET}, centred; a slot is 1.0). Small heads grow a little, big ones shrink.
 */
public final class GuiHeadFit {
    private GuiHeadFit() {
    }

    /** {scale, shiftX, shiftY} in slot units, to be applied before the item's own GUI transform ({@link HeadSlotFit#matrix}). */
    public static float[] compute(SkullSpecialRenderer renderer, ItemTransform gui, Matrix4fc local) {
        PoseStack.Pose pose = new PoseStack.Pose();
        gui.apply(false, pose);
        Matrix4f m = new Matrix4f(pose.pose()).mul(local);
        float[] box = {Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        Vector3f p = new Vector3f();
        renderer.getExtents(v -> {
            m.transformPosition(v.x(), v.y(), v.z(), p);
            box[0] = Math.min(box[0], p.x);
            box[1] = Math.min(box[1], p.y);
            box[2] = Math.max(box[2], p.x);
            box[3] = Math.max(box[3], p.y);
        });
        return HeadSlotFit.fit(box[0], box[1], box[2], box[3]);
    }
}
