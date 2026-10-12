package com.simplebuilding.modules.simpleend.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebuilding.modules.simpleend.EndWood;
import com.simplebuilding.modules.simpleend.SimpleEnd;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractBoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Unit;

/** End-wood boats: Vanilla boat geometry (oak model layers) with the module's own texture; no extra model layer. */
public final class EndBoatRenderer extends AbstractBoatRenderer {
    private final Model.Simple waterPatch;
    private final EntityModel<BoatRenderState> model;

    public EndBoatRenderer(EntityRendererProvider.Context context, EndWood wood, boolean chest) {
        super(context, SimpleEnd.id("textures/entity/" + (chest ? "chest_boat/" : "boat/") + wood.name + ".png"));
        this.waterPatch = new Model.Simple(context.bakeLayer(ModelLayers.BOAT_WATER_PATCH), t -> RenderTypes.waterMask());
        this.model = new BoatModel(context.bakeLayer(chest ? ModelLayers.OAK_CHEST_BOAT : ModelLayers.OAK_BOAT));
    }

    @Override
    protected EntityModel<BoatRenderState> model() {
        return model;
    }

    @Override
    protected void submitTypeAdditions(BoatRenderState state, PoseStack poseStack, SubmitNodeCollector collector, int lightCoords) {
        if (!state.isUnderWater) {
            collector.submitModel(waterPatch, Unit.INSTANCE, poseStack, this.texture, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
    }
}
