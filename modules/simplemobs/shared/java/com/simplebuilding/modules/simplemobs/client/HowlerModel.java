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
 * Hollow Howler, about 2.5 blocks: sculk legs and torso with a rib plate, long arms with catalyst shoulder caps (the
 * T of the summoning figure), and the head of the figure - a lower jaw and a skeleton skull hinged at the back, so the
 * mouth opens between them. Texture 128 x 128 (layout in tools/textures/howler_textures.py).
 */
public class HowlerModel extends EntityModel<HowlerRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("simplemobs", "hollow_howler"), "main");

    private final ModelPart body, neck, skull, jaw, rightArm, leftArm, rightLeg, leftLeg, rightTendril, leftTendril;

    public HowlerModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.neck = body.getChild("neck");
        this.skull = neck.getChild("skull");
        this.jaw = neck.getChild("jaw");
        this.rightTendril = skull.getChild("right_tendril");
        this.leftTendril = skull.getChild("left_tendril");
        this.rightArm = body.getChild("right_arm");
        this.leftArm = body.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition().transformed(p -> p.translated(0.0F, 24.0F, 0.0F));
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, 0.0F, -2.5F, 5, 13, 5), PartPose.offset(-4.5F, -13.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 20).addBox(-2.5F, 0.0F, -2.5F, 5, 13, 5), PartPose.offset(4.5F, -13.0F, 0.0F));
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(24, 0).addBox(-7.0F, -14.0F, -4.5F, 14, 14, 9), PartPose.offset(0.0F, -13.0F, 0.0F));
        body.addOrReplaceChild("ribs", CubeListBuilder.create().texOffs(62, 56).addBox(-5.0F, -12.0F, -5.5F, 10, 9, 1), PartPose.ZERO);
        PartDefinition rightArm = body.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(72, 0).addBox(-2.5F, -1.0F, -2.5F, 5, 18, 5), PartPose.offset(-9.5F, -12.0F, 0.0F));
        rightArm.addOrReplaceChild("right_cap", CubeListBuilder.create().texOffs(0, 40).addBox(-3.5F, -4.0F, -3.5F, 7, 5, 7), PartPose.offset(0.0F, -1.0F, 0.0F));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(94, 0).addBox(-2.5F, -1.0F, -2.5F, 5, 18, 5), PartPose.offset(9.5F, -12.0F, 0.0F));
        leftArm.addOrReplaceChild("left_cap", CubeListBuilder.create().texOffs(30, 40).addBox(-3.5F, -4.0F, -3.5F, 7, 5, 7), PartPose.offset(0.0F, -1.0F, 0.0F));
        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create(), PartPose.offset(0.0F, -14.0F, 0.0F));
        PartDefinition jaw = neck.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(60, 40).addBox(-5.0F, -4.0F, -10.0F, 10, 4, 10), PartPose.offset(0.0F, 0.0F, 4.0F));
        jaw.addOrReplaceChild("lower_teeth", CubeListBuilder.create().texOffs(42, 60).addBox(-4.0F, -5.0F, -9.5F, 8, 1, 1), PartPose.ZERO);
        PartDefinition skull = neck.addOrReplaceChild("skull",
                CubeListBuilder.create().texOffs(0, 56).addBox(-5.0F, -9.0F, -10.0F, 10, 9, 10), PartPose.offset(0.0F, -4.0F, 4.0F));
        skull.addOrReplaceChild("upper_teeth", CubeListBuilder.create().texOffs(42, 56).addBox(-4.0F, 0.0F, -9.5F, 8, 1, 1), PartPose.ZERO);
        // Antennae like the Warden's: flat forked tendrils beside the skull, they twitch and glow when it hears something.
        skull.addOrReplaceChild("right_tendril", CubeListBuilder.create().texOffs(90, 56).addBox(-10.0F, -10.0F, 0.0F, 10, 10, 0),
                PartPose.offset(-4.0F, -6.0F, -4.0F));
        skull.addOrReplaceChild("left_tendril", CubeListBuilder.create().texOffs(90, 68).addBox(0.0F, -10.0F, 0.0F, 10, 10, 0),
                PartPose.offset(4.0F, -6.0F, -4.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(HowlerRenderState state) {
        super.setupAnim(state);
        float pos = state.walkAnimationPos, speed = Math.min(1.0F, state.walkAnimationSpeed), age = state.ageInTicks, seed = state.seed;
        // The head turns fully to what it hears (the body lags behind); never still: two unrelated slow periods, plus a jaw chatter every ~3 s.
        this.neck.yRot = state.yRot * Mth.DEG_TO_RAD + Mth.sin(age * 0.07F + seed) * 0.05F + Mth.sin(age * 0.11F + seed * 1.7F) * 0.03F + state.emergeTwitch;
        this.neck.xRot = state.xRot * Mth.DEG_TO_RAD * 0.5F + Mth.cos(age * 0.09F + seed) * 0.03F;
        this.neck.zRot = state.emergeTwitch * 0.5F;
        float chatter = (age + seed * 7.0F) % 60.0F < 3.0F ? 0.05F : 0.0F;
        // Uneven gait: the left leg strides full, the right one 3/4; the body leans to the weak side and the arms trail behind the legs.
        float gait = pos * (state.charging ? 1.6F : 1.0F);
        this.rightLeg.xRot = Mth.cos(gait * 0.55F) * 0.8F * speed;
        this.leftLeg.xRot = Mth.cos(gait * 0.55F + Mth.PI) * 1.1F * speed;
        float sway = Mth.sin(age * 0.06F) * 0.05F;
        this.rightArm.xRot = Mth.cos(gait * 0.55F + Mth.PI - 1.4F) * 0.55F * speed + sway;
        this.leftArm.xRot = Mth.cos(gait * 0.55F - 1.4F) * 0.55F * speed - sway - 0.3F;
        // Posture without new cubes: right arm out, left arm bent forward, skull tilted about 4 degrees.
        this.rightArm.zRot = 0.2F + state.shriek * 0.9F;
        this.leftArm.zRot = -0.08F - state.shriek * 0.9F;
        this.skull.zRot = 0.07F;
        this.body.zRot = 0.04F * speed;
        this.body.xRot = 0.08F * speed + state.jawOpen * 0.12F - state.shriek * 0.2F;
        // The mouth opens between skull and jaw, both hinged at the back of the head.
        this.skull.xRot = -state.jawOpen * 0.55F - state.shriek * 0.2F;
        this.jaw.xRot = state.jawOpen * 0.5F + chatter;
        // listening: the antennae twitch like the Warden's tendrils; at rest they sway slowly
        float twitch = Math.min(1.0F, state.tendril * 3.0F) * Mth.cos(age * 2.25F) * Mth.PI * 0.1F;
        float rest = Mth.sin(age * 0.05F) * 0.04F;
        this.rightTendril.zRot = twitch + rest;
        this.leftTendril.zRot = -twitch - rest;
        if (state.charging) {
            // running charge: body forward, head low, arms back
            this.body.xRot += 0.35F;
            this.neck.xRot += 0.3F;
            this.rightArm.xRot += 0.9F;
            this.leftArm.xRot += 0.9F;
        }
        if (state.staggerAge >= 0.0F && state.staggerAge < 4.0F) {
            // wall hit: thrown back, springing forward again over 4 ticks
            this.body.xRot -= 0.4F * (1.0F - state.staggerAge / 4.0F);
        }
        if (state.emerge < 1.0F) {
            float under = 1.0F - state.emerge;
            this.root().y += under * 40.0F;
            this.body.xRot += under * 0.5F;
            this.rightArm.xRot -= under * 2.2F;
            this.leftArm.xRot -= under * 2.2F;
        }
        if (state.death > 0.0F) {
            // collapse over 2 s: down on the knees, head drops, the jaw has already fallen open (see renderer)
            float d = Mth.sin(state.death * Mth.HALF_PI);
            this.root().y += d * 9.0F;
            this.rightLeg.xRot = -d * 1.1F;
            this.leftLeg.xRot = -d * 1.1F;
            this.body.xRot += d * 0.45F;
            this.neck.xRot += d * 0.6F;
            this.rightArm.xRot += d * 0.4F;
            this.leftArm.xRot += d * 0.4F;
        }
    }
}
