package com.simpleanimals.client;

import com.simpleanimals.SimpleAnimals;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;

/**
 * Crab: wide shell, two claws, eyes on stalks, three legs per side. The whole model is turned a quarter circle, so the
 * animal walks sideways along its wide axis like a real crab. Layout: tools/gen_models.py.
 */
public class CrabModel extends EntityModel<CrabRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(SimpleAnimals.id("crab"), "main");
    private final ModelPart body, armRight, armLeft;
    private final ModelPart[] rightLegs = new ModelPart[3], leftLegs = new ModelPart[3];

    public CrabModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.armRight = this.body.getChild("arm_right");
        this.armLeft = this.body.getChild("arm_left");
        for (int i = 0; i < 3; i++) {
            this.rightLegs[i] = this.body.getChild("right_leg_" + i);
            this.leftLegs[i] = this.body.getChild("left_leg_" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        return CrabLayers.create();
    }

    @Override
    public void setupAnim(CrabRenderer.State state) {
        super.setupAnim(state);
        this.root.yRot = (float) (Math.PI / 2.0);
        float pos = state.walkAnimationPos * 1.2F, speed = Math.min(1.0F, state.walkAnimationSpeed * 2.0F);
        for (int i = 0; i < 3; i++) {
            float phase = i * 1.1F;
            this.rightLegs[i].zRot += Mth.cos(pos + phase) * 0.35F * speed;
            this.rightLegs[i].yRot = Mth.sin(pos + phase) * 0.25F * speed;
            this.leftLegs[i].zRot -= Mth.cos(pos + phase + (float) Math.PI) * 0.35F * speed;
            this.leftLegs[i].yRot = Mth.sin(pos + phase + (float) Math.PI) * 0.25F * speed;
        }
        // The claws open and close a little when it stands, and bob when it walks.
        float idle = Mth.sin(state.ageInTicks * 0.08F) * 0.12F;
        this.armRight.xRot = idle - 0.1F * speed;
        this.armLeft.xRot = -idle - 0.1F * speed;
    }
}
