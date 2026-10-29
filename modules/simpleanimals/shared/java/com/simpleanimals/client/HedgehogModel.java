package com.simpleanimals.client;

import com.simpleanimals.SimpleAnimals;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;

/**
 * Hedgehog: the quick roll-up is a blend {@code curl} 0..1 (0.3 s): head and legs vanish into the spines, the body gets
 * shorter and rounder. Layout: tools/gen_models.py.
 */
public class HedgehogModel extends EntityModel<HedgehogRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(SimpleAnimals.id("hedgehog"), "main");
    private final ModelPart body, head, rightHindLeg, leftHindLeg, rightFrontLeg, leftFrontLeg;

    public HedgehogModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
    }

    public static LayerDefinition createBodyLayer() {
        return HedgehogLayers.create();
    }

    @Override
    public void setupAnim(HedgehogRenderer.State state) {
        super.setupAnim(state);
        float c = state.curl;
        float walk = (1.0F - c) * state.walkAnimationSpeed;
        this.head.xRot = state.xRot * (float) (Math.PI / 180.0);
        this.head.yRot = state.yRot * (float) (Math.PI / 180.0);
        this.rightHindLeg.xRot = Mth.cos(state.walkAnimationPos * 0.9F) * 1.2F * walk;
        this.leftHindLeg.xRot = Mth.cos(state.walkAnimationPos * 0.9F + (float) Math.PI) * 1.2F * walk;
        this.rightFrontLeg.xRot = Mth.cos(state.walkAnimationPos * 0.9F + (float) Math.PI) * 1.2F * walk;
        this.leftFrontLeg.xRot = Mth.cos(state.walkAnimationPos * 0.9F) * 1.2F * walk;
        // Curled: shorter and rounder, face and feet tucked away (the head leaves first, the feet follow at the end).
        this.body.yScale = 1.0F + 0.45F * c;
        this.body.zScale = 1.0F - 0.2F * c;
        this.body.xScale = 1.0F + 0.1F * c;
        this.head.visible = c < 0.35F;
        boolean feet = c < 0.7F;
        this.rightHindLeg.visible = feet;
        this.leftHindLeg.visible = feet;
        this.rightFrontLeg.visible = feet;
        this.leftFrontLeg.visible = feet;
        this.head.z += 1.5F * c;
        this.root.y += 2.0F * c; // without legs the ball sits on the ground
    }
}
