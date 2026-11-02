package com.simplebuilding.modules.simplemobs.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Brimguard, hat preset M (owner 01.11.): 1.5 blocks; a squat body in a dark void cloak, glowing eyes just under the
 * brim, and a bedrock sombrero - brim edge 0.8 above the feet (13 px), 2.2 wide (34 px plus the turned-up rim), crown in
 * two steps up to 1.5 (24 px). Ducking scales every height by 2/3 (the renderer squashes the root), venting lifts the
 * whole hat by 0.5 (8 px). Texture 256 x 128, layout in tools/textures/boss_textures.py.
 */
public class BrimguardModel extends EntityModel<BrimguardRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("simplemobs", "brimguard"), "main");

    private final ModelPart body, hat, rightArm, leftArm, rightLeg, leftLeg;

    public BrimguardModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.hat = root.getChild("hat");
        this.rightArm = body.getChild("right_arm");
        this.leftArm = body.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition().transformed(p -> p.translated(0.0F, 24.0F, 0.0F));
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(70, 40).addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4), PartPose.offset(-2.5F, -6.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(88, 40).addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4), PartPose.offset(2.5F, -6.0F, 0.0F));
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 40).addBox(-5.0F, -8.0F, -3.5F, 10, 8, 7), PartPose.offset(0.0F, -5.0F, 0.0F));
        body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(36, 40).addBox(-4.0F, -7.0F, -4.0F, 8, 7, 8), PartPose.offset(0.0F, -6.0F, -0.5F));
        body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(106, 40).addBox(-1.5F, -1.0F, -1.5F, 3, 7, 3), PartPose.offset(-6.5F, -6.5F, 0.0F));
        body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(120, 40).addBox(-1.5F, -1.0F, -1.5F, 3, 7, 3), PartPose.offset(6.5F, -6.5F, 0.0F));
        // the hat: pivot at the brim (13 px above the feet)
        PartDefinition hat = root.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(0, 0).addBox(-17.0F, -1.0F, -17.0F, 34, 1, 34), PartPose.offset(0.0F, -13.0F, 0.0F));
        hat.addOrReplaceChild("rim_front", CubeListBuilder.create().texOffs(140, 40).addBox(-17.0F, -2.0F, -18.0F, 34, 2, 1), PartPose.ZERO);
        hat.addOrReplaceChild("rim_back", CubeListBuilder.create().texOffs(140, 44).addBox(-17.0F, -2.0F, 17.0F, 34, 2, 1), PartPose.ZERO);
        hat.addOrReplaceChild("rim_right", CubeListBuilder.create().texOffs(0, 60).addBox(-18.0F, -2.0F, -17.0F, 1, 2, 34), PartPose.ZERO);
        hat.addOrReplaceChild("rim_left", CubeListBuilder.create().texOffs(72, 60).addBox(17.0F, -2.0F, -17.0F, 1, 2, 34), PartPose.ZERO);
        hat.addOrReplaceChild("band", CubeListBuilder.create().texOffs(140, 20).addBox(-6.5F, -3.0F, -6.5F, 13, 2, 13), PartPose.ZERO);
        hat.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(140, 0).addBox(-6.0F, -7.0F, -6.0F, 12, 6, 12), PartPose.ZERO);
        hat.addOrReplaceChild("crown_top", CubeListBuilder.create().texOffs(190, 0).addBox(-4.5F, -11.0F, -4.5F, 9, 5, 9), PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 128);
    }

    @Override
    public void setupAnim(BrimguardRenderState state) {
        super.setupAnim(state);
        float pos = state.walkAnimationPos, speed = Math.min(1.0F, state.walkAnimationSpeed), age = state.ageInTicks;
        // short heavy steps, the hat sways a little
        this.rightLeg.xRot = Mth.cos(pos * 0.7F) * 0.8F * speed;
        this.leftLeg.xRot = Mth.cos(pos * 0.7F + Mth.PI) * 0.8F * speed;
        this.rightArm.xRot = Mth.cos(pos * 0.7F + Mth.PI) * 0.5F * speed;
        this.leftArm.xRot = Mth.cos(pos * 0.7F) * 0.5F * speed;
        this.body.y += Mth.cos(pos * 1.4F) * 0.4F * speed;
        this.hat.y += Mth.cos(pos * 1.4F) * 0.4F * speed;
        this.hat.zRot = Mth.sin(pos * 0.7F) * 0.03F * speed;
        this.hat.yRot = state.yRot * Mth.DEG_TO_RAD * 0.3F;
        // venting: the hat rises 8 px (0.5 blocks) and tilts back a little
        this.hat.y -= 8.0F * state.vent;
        this.hat.xRot = -0.08F * state.vent + Mth.sin(age * 0.9F) * 0.02F * state.vent;
        if (state.slam > 0.0F) {
            // slam telegraph: both arms up to the brim, the hat lifts and spins, then comes down
            float s = state.slam;
            this.rightArm.xRot = -2.6F * s;
            this.leftArm.xRot = -2.6F * s;
            this.hat.y -= 3.0F * Mth.sin(s * Mth.PI);
            this.hat.yRot += s * Mth.TWO_PI;
        }
        if (state.charge > 0.0F) {
            // scrape and charge: leaning forward behind the brim
            this.body.xRot = 0.35F * state.charge;
            this.hat.xRot += 0.25F * state.charge;
            this.rightLeg.xRot += Mth.sin(age * 1.6F) * 0.5F * state.charge;
        }
        // ducking: every height x 2/3 around the feet
        this.root().yScale = 1.0F - state.duck / 3.0F;
    }
}
