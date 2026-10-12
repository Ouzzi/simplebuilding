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
 * Hollow Watcher, 4 blocks: a gaunt shadow on long thin legs, arms that hang to its knees, a narrow chest and a tall
 * head covered in eyes; End stone splinters stick out of shoulders, back and head. Texture 128 x 128
 * (layout in tools/textures/fear_textures.py).
 */
public class WatcherModel extends EntityModel<WatcherRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("simplemobs", "hollow_watcher"), "main");

    private final ModelPart body, head, rightArm, leftArm, rightLeg, leftLeg;

    public WatcherModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = body.getChild("neck").getChild("head");
        this.rightArm = body.getChild("right_arm");
        this.leftArm = body.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition().transformed(p -> p.translated(0.0F, 24.0F, 0.0F));
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, 0.0F, -1.5F, 3, 30, 3), PartPose.offset(-2.5F, -30.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(12, 0).addBox(-1.5F, 0.0F, -1.5F, 3, 30, 3), PartPose.offset(2.5F, -30.0F, 0.0F));
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(48, 0).addBox(-4.5F, -16.0F, -2.5F, 9, 16, 5), PartPose.offset(0.0F, -30.0F, 0.0F));
        PartDefinition neck = body.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(108, 0).addBox(-1.0F, -3.0F, -1.0F, 2, 3, 2), PartPose.offset(0.0F, -16.0F, -0.5F));
        PartDefinition head = neck.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(76, 0).addBox(-4.0F, -12.0F, -4.0F, 8, 12, 8), PartPose.offset(0.0F, -2.5F, 0.0F));
        body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(24, 0).addBox(-1.5F, -1.0F, -1.5F, 3, 34, 3), PartPose.offset(-6.0F, -14.5F, 0.0F));
        body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(36, 0).addBox(-1.5F, -1.0F, -1.5F, 3, 34, 3), PartPose.offset(6.0F, -14.5F, 0.0F));
        // End stone splinters grown into the shadow
        CubeListBuilder thin = CubeListBuilder.create().texOffs(48, 21).addBox(-1.0F, -5.0F, -1.0F, 2, 5, 2);
        CubeListBuilder thick = CubeListBuilder.create().texOffs(56, 21).addBox(-1.5F, -3.0F, -1.5F, 3, 3, 3);
        body.addOrReplaceChild("shard_shoulder", thin, PartPose.offsetAndRotation(-4.0F, -15.0F, 0.5F, 0.2F, 0.0F, -0.6F));
        body.addOrReplaceChild("shard_back", thin, PartPose.offsetAndRotation(1.5F, -10.0F, 2.5F, -0.9F, 0.3F, 0.1F));
        body.addOrReplaceChild("shard_side", thick, PartPose.offsetAndRotation(4.0F, -6.0F, 0.0F, 0.3F, 0.4F, 0.7F));
        head.addOrReplaceChild("shard_crown", thin, PartPose.offsetAndRotation(-1.5F, -12.0F, 0.0F, 0.1F, 0.0F, -0.35F));
        head.addOrReplaceChild("shard_crown2", thick, PartPose.offsetAndRotation(2.5F, -11.5F, 1.0F, -0.3F, 0.5F, 0.5F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(WatcherRenderState state) {
        super.setupAnim(state);
        float pos = state.walkAnimationPos, speed = Math.min(1.0F, state.walkAnimationSpeed), age = state.ageInTicks;
        boolean still = state.frozen || state.watched;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        this.rightLeg.xRot = Mth.cos(pos * 0.4F) * 0.8F * speed;
        this.leftLeg.xRot = Mth.cos(pos * 0.4F + Mth.PI) * 0.8F * speed;
        this.rightArm.xRot = Mth.cos(pos * 0.4F + Mth.PI) * 0.5F * speed;
        this.leftArm.xRot = Mth.cos(pos * 0.4F) * 0.5F * speed;
        // a slow, crooked sway while nobody looks; perfectly still when watched
        float sway = still ? 0.0F : Mth.sin(age * 0.07F) * 0.06F;
        this.body.xRot = 0.12F + sway;
        this.body.zRot = still ? 0.0F : Mth.sin(age * 0.05F) * 0.04F;
        this.head.zRot = still ? 0.12F : Mth.sin(age * 0.09F) * 0.15F;
        this.rightArm.zRot = 0.06F + sway;
        this.leftArm.zRot = -0.06F - sway;
        if (state.reach > 0.0F) {
            float r = state.reach;
            this.rightArm.xRot = -1.5F * r;
            this.leftArm.xRot = -1.5F * r;
            this.rightArm.zRot = 0.25F * r;
            this.leftArm.zRot = -0.25F * r;
            this.body.xRot += 0.25F * r;
        }
        if (state.scream > 0.0F) {
            float s = state.scream;
            this.head.xRot = -0.9F * s;
            this.body.xRot -= 0.25F * s;
            this.rightArm.zRot = 1.2F * s;
            this.leftArm.zRot = -1.2F * s;
        }
    }
}
