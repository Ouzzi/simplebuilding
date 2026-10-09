package com.simplebuilding.dummy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebuilding.version.McClientVersion;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.object.armorstand.ArmorStandArmorModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.world.entity.decoration.ArmorStand;

/**
 * Mittlerer und kleiner Ruestungsstaender (2026-10-09): Vanillas Modell und Holztextur, ohne Kopf-, Schulter- und
 * Rumpfstangen und ohne Arme; der mittlere behaelt Beine und Hueftstange, der kleine nur die Bodenplatte. Die Ruestung
 * (Hose, Stiefel) zeichnet Vanillas Ruestungsebene an der gewohnten Stelle. Der kleine bekommt statt der Beine zwei
 * kurze Zapfen (3 Pixel, Holz der Beinstangen), die in den Stiefeln verschwinden. Nie als „kleiner“ Vanilla-Staender
 * (halbe Groesse), damit die ausgeblendeten Teile des grossen Modells gelten.
 */
public class PartialArmorStandRenderer extends ArmorStandRenderer {
    public PartialArmorStandRenderer(EntityRendererProvider.Context context, boolean medium) {
        super(context);
        EntityModel<ArmorStandRenderState> model = this.getModel();
        ModelPart root = model.root();
        for (String part : new String[]{"head", "body", "right_body_stick", "left_body_stick", "right_arm", "left_arm"}) {
            root.getChild(part).visible = false;
        }
        if (!medium) {
            for (String part : new String[]{"shoulder_stick", "right_leg", "left_leg"}) {
                root.getChild(part).visible = false;
            }
            this.addLayer(new PegLayer(this));
        }
    }

    /** Zwei Zapfen an den Fusspunkten der (ausgeblendeten) Beine, Textur der Vanilla-Beinstange. */
    static final class PegLayer extends RenderLayer<ArmorStandRenderState, ArmorStandArmorModel> {
        private final ModelPart peg;

        PegLayer(RenderLayerParent<ArmorStandRenderState, ArmorStandArmorModel> parent) {
            super(parent);
            MeshDefinition mesh = new MeshDefinition();
            mesh.getRoot().addOrReplaceChild("peg", CubeListBuilder.create().texOffs(8, 0).addBox(-1.0F, 8.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                    PartPose.ZERO);
            this.peg = LayerDefinition.create(mesh, 64, 64).bakeRoot().getChild("peg");
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, ArmorStandRenderState state, float yRot, float xRot) {
            if (state.isMarker || state.isInvisible) {
                return;
            }
            int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
            for (ModelPart leg : new ModelPart[]{this.getParentModel().rightLeg, this.getParentModel().leftLeg}) {
                poseStack.pushPose();
                leg.translateAndRotate(poseStack);
                McClientVersion.submitTintedModelPart(collector, this.peg, poseStack, RenderTypes.entityCutout(DEFAULT_SKIN_LOCATION),
                        lightCoords, overlay, -1);
                poseStack.popPose();
            }
        }
    }

    public static EntityRendererProvider<ArmorStand> medium() {
        return context -> new PartialArmorStandRenderer(context, true);
    }

    public static EntityRendererProvider<ArmorStand> small() {
        return context -> new PartialArmorStandRenderer(context, false);
    }

    @Override
    public void extractRenderState(ArmorStand stand, ArmorStandRenderState state, float partialTick) {
        super.extractRenderState(stand, state, partialTick);
        state.isSmall = false;
        state.showArms = false;
    }
}
