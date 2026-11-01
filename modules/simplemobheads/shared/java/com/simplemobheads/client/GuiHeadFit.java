package com.simplemobheads.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplemobheads.HeadSlotFit;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

/**
 * IG31-10: how a head is fitted into an inventory slot. Generic, from the model itself (no per-head numbers): the corners of every
 * DRAWN cube of the head and of its passes (eyes, wool, a slime's outer cube, the tinted copy) go through the item's local transform
 * and its GUI transform, and the screen box of those points is fitted by {@link HeadSlotFit} (larger extent
 * {@value HeadSlotFit#TARGET}, centred; a slot is 1.0). IG31-39: Vanilla's {@code getExtents} also counts hidden parts (a villager's
 * robe, a horse's body), which made such heads tiny; it also misses the passes, which then stuck out of the slot.
 */
public final class GuiHeadFit {
    private GuiHeadFit() {
    }

    /** {scale, shiftX, shiftY} in slot units, to be applied before the item's own GUI transform ({@link HeadSlotFit#matrix}). */
    public static float[] compute(MobHeadModel model, ItemTransform gui, Matrix4fc local) {
        PoseStack.Pose pose = new PoseStack.Pose();
        gui.apply(false, pose);
        Matrix4f m = new Matrix4f(pose.pose()).mul(local);
        float[] box = {Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        // the root of a tinted look is switched off as a whole (its tinted copy is a pass), so the root's own flag is not asked
        extents(model.root(), m, box);
        for (MobHeadModel.Pass pass : model.passes()) {
            extents(pass.model().root(), m, box);
        }
        return HeadSlotFit.fit(box[0], box[1], box[2], box[3]);
    }

    private static void extents(ModelPart root, Matrix4f m, float[] box) {
        Map<String, Boolean> drawn = new HashMap<>();
        Vector3f p = new Vector3f();
        root.visit(new PoseStack(), (pose, path, index, cube) -> {
            if (!drawn.computeIfAbsent(path, k -> drawn(root, k))) {
                return;
            }
            for (int corner = 0; corner < 8; corner++) {
                p.set(((corner & 1) == 0 ? cube.minX : cube.maxX) / 16.0F, ((corner & 2) == 0 ? cube.minY : cube.maxY) / 16.0F,
                        ((corner & 4) == 0 ? cube.minZ : cube.maxZ) / 16.0F);
                pose.pose().transformPosition(p);
                m.transformPosition(p);
                box[0] = Math.min(box[0], p.x);
                box[1] = Math.min(box[1], p.y);
                box[2] = Math.max(box[2], p.x);
                box[3] = Math.max(box[3], p.y);
            }
        });
    }

    /** Whether the own cubes of the part at {@code path} ("/head/hat") are drawn: every part below the root visible, itself not skipDraw. */
    private static boolean drawn(ModelPart root, String path) {
        ModelPart part = root;
        if (!path.isEmpty()) {
            for (String name : path.substring(1).split("/")) {
                part = part.getChild(name);
                if (!part.visible) {
                    return false;
                }
            }
        }
        return !part.skipDraw;
    }
}
