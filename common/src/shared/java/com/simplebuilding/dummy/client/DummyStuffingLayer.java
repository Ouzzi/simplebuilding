package com.simplebuilding.dummy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebuilding.version.McClientVersion;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.object.armorstand.ArmorStandArmorModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Das Aussehen der Trainingspuppe (Besitzer 2026-10-03/04): auf dem Stroh-Staender sitzen ein gestopfter Strohrumpf mit
 * der Zielscheibe des Vanilla-Zielblocks und ein geschnitzter Kuerbis als Kopf (Masse wie Spielerkopf und -rumpf, damit
 * Ruestung passt). Der Kuerbis aus der Umwandlung steckt im Modell, er ist kein Ausruestungsteil; ein Mob-Kopf ersetzt
 * ihn. Die Textur hat doppelte Dichte (128x64 bei erklaerten 64x32): jede Kopfseite traegt eine Vanilla-Blocktextur 1:1
 * (Generator tools/textures/training_dummy_v3_2026_10_04.py).
 * Gebacken aus einer eigenen {@link LayerDefinition} ({@code bakeRoot}), also ohne Modell-Ebenen-Registrierung je Loader.
 */
public class DummyStuffingLayer extends RenderLayer<ArmorStandRenderState, ArmorStandArmorModel> {
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("simplebuilding", "textures/entity/training_dummy/stuffing.png");
    private final ModelPart head;
    private final ModelPart body;

    public DummyStuffingLayer(RenderLayerParent<ArmorStandRenderState, ArmorStandArmorModel> parent) {
        super(parent);
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F), PartPose.ZERO);
        ModelPart baked = LayerDefinition.create(mesh, 64, 32).bakeRoot();
        this.head = baked.getChild("head");
        this.body = baked.getChild("body");
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, ArmorStandRenderState state, float yRot, float xRot) {
        if (state.isMarker || state.isInvisible) {
            return;
        }
        RenderType type = RenderTypes.entityCutout(TEXTURE);
        int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);
        McClientVersion.submitTintedModelPart(collector, this.body, poseStack, type, lightCoords, overlay, -1);
        poseStack.popPose();
        if (state.headEquipment.isEmpty()) {
            poseStack.pushPose();
            this.getParentModel().head.translateAndRotate(poseStack);
            McClientVersion.submitTintedModelPart(collector, this.head, poseStack, type, lightCoords, overlay, -1);
            poseStack.popPose();
        }
    }
}
