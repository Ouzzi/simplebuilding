package com.simplebuilding.version;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;

/**
 * Client half of the version shim, 26.2 side (see {@link McVersion}). Only ever loaded on the client.
 */
public final class McClientVersion {

    private McClientVersion() {
    }

    /** The keyboard key with this code (26.2: KEYSYM, 26.3: KEYBOARD). */
    public static InputConstants.Key keyboardKey(int keyCode) {
        return InputConstants.Type.KEYSYM.getOrCreate(keyCode);
    }

    /** The input type of keyboard keys (26.2: KEYSYM, 26.3: KEYBOARD). */
    public static InputConstants.Type keyboardType() {
        return InputConstants.Type.KEYSYM;
    }

    /** Whether a baked quad asks for directional shading (26.3: no UP shade override). */
    public static boolean quadShaded(BakedQuad quad) {
        return quad.materialInfo().shade();
    }

    /** submitModelPart with a tint (26.2 has an extra sprite/crumbling pair, 26.3 a UV mapping). */
    public static void submitTintedModelPart(SubmitNodeCollector collector, ModelPart part, PoseStack poseStack,
                                             RenderType renderType, int lightCoords, int overlayCoords, int tint) {
        collector.submitModelPart(part, poseStack, renderType, lightCoords, overlayCoords, null, tint, null);
    }
}
