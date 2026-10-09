package com.simplebuilding.dummy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebuilding.version.McClientVersion;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.object.armorstand.ArmorStandArmorModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.player.PlayerModelType;

/**
 * Trainingspuppe mit Spielernamen (2026-10-09): zeichnet den Spielerkoerper mit der aufgeloesten Haut (beide Ebenen,
 * breite oder schmale Arme) in der Pose des Ruestungsstaenders. Gebacken aus Vanillas {@link PlayerModel#createMesh}
 * ({@code bakeRoot}), ohne Modell-Ebenen-Registrierung je Loader. Ein Kopf im Kopfslot ersetzt den Hautkopf.
 */
public class DummySkinLayer extends RenderLayer<ArmorStandRenderState, ArmorStandArmorModel> {
    private static final String[] PARTS = {"head", "body", "right_arm", "left_arm", "right_leg", "left_leg"};
    private final ModelPart wide;
    private final ModelPart slim;

    public DummySkinLayer(RenderLayerParent<ArmorStandRenderState, ArmorStandArmorModel> parent) {
        super(parent);
        this.wide = LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, false), 64, 64).bakeRoot();
        this.slim = LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, true), 64, 64).bakeRoot();
    }

    /** Ob die Puppe gerade als Spieler gezeichnet wird (die Strohfuellung bleibt dann weg). */
    static boolean active(ArmorStandRenderState state) {
        return state instanceof DummyRenderState dummy && dummy.skin != null && !state.isMarker && !state.isInvisible && !state.isSmall;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, ArmorStandRenderState state, float yRot, float xRot) {
        if (!active(state)) {
            return;
        }
        var skin = ((DummyRenderState) state).skin.playerSkin();
        ModelPart root = skin.model() == PlayerModelType.SLIM ? this.slim : this.wide;
        ArmorStandArmorModel stand = this.getParentModel();
        ModelPart[] from = {stand.head, stand.body, stand.rightArm, stand.leftArm, stand.rightLeg, stand.leftLeg};
        for (int i = 0; i < PARTS.length; i++) {
            ModelPart part = root.getChild(PARTS[i]);
            part.loadPose(from[i].storePose());
            part.visible = true;
        }
        root.getChild("head").visible = state.headEquipment.isEmpty();
        RenderType type = RenderTypes.entityTranslucent(skin.body().texturePath());
        McClientVersion.submitTintedModelPart(collector, root, poseStack, type, lightCoords,
                LivingEntityRenderer.getOverlayCoords(state, 0.0F), -1);
    }
}
