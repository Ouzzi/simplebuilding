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
 * Void Runner, variant A1 "fencer with forearm blades" (owner 01.11.): about 1.9 blocks, slim legs, a narrow torso,
 * a smooth head with one magenta eye slit, pale blades along both forearms and a long scarf whose three tail pieces
 * stream out behind it the faster it runs (the speed gauge). Texture 64 x 64, layout in tools/textures/boss_textures.py.
 */
public class VoidRunnerModel extends EntityModel<VoidRunnerRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("simplemobs", "void_runner"), "main");

    private final ModelPart body, head, rightArm, leftArm, rightLeg, leftLeg, scarf1, scarf2, scarf3;

    public VoidRunnerModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.rightArm = body.getChild("right_arm");
        this.leftArm = body.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.scarf1 = body.getChild("scarf").getChild("scarf1");
        this.scarf2 = scarf1.getChild("scarf2");
        this.scarf3 = scarf2.getChild("scarf3");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition().transformed(p -> p.translated(0.0F, 24.0F, 0.0F));
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-1.5F, 0.0F, -1.5F, 3, 13, 3), PartPose.offset(-1.5F, -13.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(12, 16).addBox(-1.5F, 0.0F, -1.5F, 3, 13, 3), PartPose.offset(1.5F, -13.0F, 0.0F));
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(20, 0).addBox(-3.0F, -11.0F, -1.5F, 6, 11, 3), PartPose.offset(0.0F, -13.0F, 0.0F));
        body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -6.0F, -2.5F, 5, 6, 5), PartPose.offset(0.0F, -11.5F, 0.0F));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(24, 16).addBox(-1.0F, -1.0F, -1.0F, 2, 11, 2), PartPose.offset(-4.0F, -10.0F, 0.0F));
        rightArm.addOrReplaceChild("right_blade", CubeListBuilder.create().texOffs(40, 16).addBox(-0.5F, 0.0F, -1.5F, 1, 11, 3), PartPose.offset(-1.2F, 4.0F, 0.0F));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(32, 16).addBox(-1.0F, -1.0F, -1.0F, 2, 11, 2), PartPose.offset(4.0F, -10.0F, 0.0F));
        leftArm.addOrReplaceChild("left_blade", CubeListBuilder.create().texOffs(48, 16).addBox(-0.5F, 0.0F, -1.5F, 1, 11, 3), PartPose.offset(1.2F, 4.0F, 0.0F));
        // scarf: a wrap round the neck and three tail pieces hanging from the back of the neck
        PartDefinition scarf = body.addOrReplaceChild("scarf", CubeListBuilder.create().texOffs(0, 34).addBox(-3.5F, -1.0F, -2.0F, 7, 2, 4), PartPose.offset(0.0F, -11.0F, 0.0F));
        PartDefinition s1 = scarf.addOrReplaceChild("scarf1", CubeListBuilder.create().texOffs(24, 34).addBox(-1.0F, 0.0F, 0.0F, 2, 1, 5), PartPose.offset(1.0F, 0.0F, 2.0F));
        PartDefinition s2 = s1.addOrReplaceChild("scarf2", CubeListBuilder.create().texOffs(38, 34).addBox(-1.0F, 0.0F, 0.0F, 2, 1, 5), PartPose.offset(0.0F, 0.0F, 5.0F));
        s2.addOrReplaceChild("scarf3", CubeListBuilder.create().texOffs(0, 42).addBox(-1.0F, 0.0F, 0.0F, 2, 1, 5), PartPose.offset(0.0F, 0.0F, 5.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(VoidRunnerRenderState state) {
        super.setupAnim(state);
        float pos = state.walkAnimationPos, speed = Math.min(1.0F, state.walkAnimationSpeed), age = state.ageInTicks;
        this.head.yRot = Mth.clamp(state.yRot, -50.0F, 50.0F) * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        // a runner's stride: long, quick, torso leaning into the run
        this.rightLeg.xRot = Mth.cos(pos * 0.8F) * 1.1F * speed;
        this.leftLeg.xRot = Mth.cos(pos * 0.8F + Mth.PI) * 1.1F * speed;
        this.body.xRot = 0.35F * speed;
        this.rightArm.xRot = Mth.cos(pos * 0.8F + Mth.PI) * 0.9F * speed - 0.2F;
        this.leftArm.xRot = Mth.cos(pos * 0.8F) * 0.9F * speed - 0.2F;
        this.rightArm.zRot = 0.08F;
        this.leftArm.zRot = -0.08F;
        // scarf: hangs down when it stands, streams out flat behind it when it runs, flutters at the tip
        float stream = Math.min(1.0F, speed * 1.4F);
        float flutter = Mth.sin(age * 0.6F) * 0.15F * stream;
        this.scarf1.xRot = Mth.lerp(stream, -1.35F, -0.05F) - this.body.xRot;
        this.scarf2.xRot = Mth.lerp(stream, 0.1F, 0.0F) + flutter;
        this.scarf3.xRot = Mth.lerp(stream, 0.1F, 0.0F) - flutter * 1.5F;
        this.scarf1.yRot = Mth.sin(age * 0.2F) * 0.1F;
        if (state.strike > 0.0F) {
            // blade slash: right arm sweeps across
            float s = state.strike;
            this.rightArm.xRot = -1.5F * s;
            this.rightArm.yRot = 0.8F * s;
            this.body.yRot = -0.4F * s;
        }
        if (state.crouch > 0.0F) {
            // lunge telegraph: low crouch, both blades back
            float c = state.crouch;
            this.body.xRot += 0.5F * c;
            this.rightLeg.xRot = -0.9F * c;
            this.leftLeg.xRot = 0.4F * c;
            this.root().y += 3.0F * c;
            this.rightArm.xRot = 1.0F * c;
            this.leftArm.xRot = 1.0F * c;
        }
        if (state.lunge > 0.0F) {
            // in the air: both blades forward
            this.rightArm.xRot = -1.6F;
            this.leftArm.xRot = -1.6F;
            this.rightLeg.xRot = 0.6F;
            this.leftLeg.xRot = -0.6F;
            this.scarf1.xRot = -0.05F - this.body.xRot;
        }
        if (state.stagger > 0.0F) {
            float t = state.stagger;
            this.body.xRot = -0.3F * t;
            this.head.xRot = 0.4F * t + Mth.sin(age * 0.8F) * 0.1F * t;
            this.rightArm.zRot = 0.6F * t;
            this.leftArm.zRot = -0.6F * t;
        }
    }
}
