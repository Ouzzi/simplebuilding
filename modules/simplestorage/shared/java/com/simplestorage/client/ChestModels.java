package com.simplestorage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.jetbrains.annotations.Nullable;

/** Submits a chest model with its sprite (and the crumbling overlay while it is being mined), as Vanilla's chest renderer does. */
final class ChestModels {
    private ChestModels() {
    }

    static void submit(SubmitNodeCollector collector, net.minecraft.client.model.object.chest.ChestModel model, float open, PoseStack poseStack,
                       int lightCoords, net.minecraft.client.resources.model.sprite.SpriteId sprite,
                       net.minecraft.client.resources.model.sprite.SpriteGetter sprites, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        collector.submitModel(model, open, poseStack, lightCoords, OverlayTexture.NO_OVERLAY, -1, sprite, sprites, 0);
        if (crumbling != null) {
            collector.order(1).submitCrumblingOverlay(model, open, poseStack, sprite.renderType(model.renderType()), lightCoords,
                    OverlayTexture.NO_OVERLAY, -1, crumbling);
        }
    }
}
