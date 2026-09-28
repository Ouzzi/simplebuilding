package com.simplebuilding.version;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;

/** Client half of the version shim, 26.3 side (see the 26.2 twin). */
public final class McClientVersion {

    private McClientVersion() {
    }

    public static InputConstants.Key keyboardKey(int keyCode) {
        return InputConstants.Type.KEYBOARD.getOrCreate(keyCode);
    }

    public static InputConstants.Type keyboardType() {
        return InputConstants.Type.KEYBOARD;
    }

    public static boolean isKeyDown(net.minecraft.client.Minecraft client, int keyCode) {
        return InputConstants.isKeyDown(keyCode);
    }

    public static void rotate(PoseStack poseStack, org.joml.Quaternionfc rotation) {
        poseStack.rotate(rotation);
    }

    /** 26.3 replaced the shade flag with a shade direction override; UP is the old "shade": false. */
    public static boolean quadShaded(BakedQuad quad) {
        return quad.materialInfo().shadeDirectionOverride() != Direction.UP;
    }

    public static void submitTintedModelPart(SubmitNodeCollector collector, ModelPart part, PoseStack poseStack,
                                             RenderType renderType, int lightCoords, int overlayCoords, int tint) {
        collector.submitModelPart(part, poseStack, renderType, lightCoords, overlayCoords, null, tint);
    }

    /** Submits a chest model with its break overlay the way 26.3's ChestRenderer does (see the 26.2 twin). */
    public static void submitChestModel(SubmitNodeCollector collector, net.minecraft.client.model.object.chest.ChestModel model,
                                        float open, PoseStack poseStack, int lightCoords,
                                        net.minecraft.client.resources.model.sprite.SpriteId sprite,
                                        net.minecraft.client.resources.model.sprite.SpriteGetter sprites,
                                        net.minecraft.client.renderer.feature.ModelFeatureRenderer.@org.jetbrains.annotations.Nullable CrumblingOverlay crumbling) {
        collector.submitModel(model, open, poseStack, lightCoords, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, -1,
                sprite, sprites, 0);
        if (crumbling != null) {
            collector.order(1).submitCrumblingOverlay(model, open, poseStack, sprite.renderType(model.renderType()), lightCoords,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, -1, crumbling);
        }
    }
}
