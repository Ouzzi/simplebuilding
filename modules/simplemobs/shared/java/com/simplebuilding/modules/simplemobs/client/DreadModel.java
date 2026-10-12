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
 * Dread, about 2.6 blocks: heavy armored legs, a broad breastplate with a skirt of plates, huge pauldrons, a closed
 * bucket helmet with a crest (empty inside, soul flames behind the visor), and in the right hand a chain mace whose
 * spiked head drags on the ground. Texture 128 x 128 (layout in tools/textures/fear_textures.py).
 */
public class DreadModel extends EntityModel<DreadRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("simplemobs", "dread"), "main");
    /** Forward lean of the chain at rest, so the mace head lies on the ground in front of the right foot. */
    private static final float CHAIN_REST = 1.2F;

    private final ModelPart body, head, rightArm, leftArm, rightLeg, leftLeg, chain, maceHead;

    public DreadModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.rightArm = body.getChild("right_arm");
        this.leftArm = body.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.chain = rightArm.getChild("handle").getChild("chain");
        this.maceHead = chain.getChild("mace_head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition().transformed(p -> p.translated(0.0F, 24.0F, 0.0F));
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, 0.0F, -3.0F, 6, 15, 6), PartPose.offset(-4.0F, -15.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 21).addBox(-3.0F, 0.0F, -3.0F, 6, 15, 6), PartPose.offset(4.0F, -15.0F, 0.0F));
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(24, 0).addBox(-8.0F, -15.0F, -4.5F, 16, 15, 9), PartPose.offset(0.0F, -15.0F, 0.0F));
        body.addOrReplaceChild("faulds", CubeListBuilder.create().texOffs(24, 24).addBox(-8.5F, -1.0F, -5.0F, 17, 4, 10), PartPose.ZERO);
        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 42).addBox(-5.0F, -11.0F, -5.0F, 10, 11, 10), PartPose.offset(0.0F, -15.0F, -0.5F));
        head.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(40, 42).addBox(-0.5F, -3.0F, -4.0F, 1, 3, 8), PartPose.offset(0.0F, -11.0F, 0.0F));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(78, 0).addBox(-3.0F, -2.0F, -3.0F, 6, 16, 6), PartPose.offset(-11.0F, -13.0F, 0.0F));
        rightArm.addOrReplaceChild("right_pauldron", CubeListBuilder.create().texOffs(78, 22).addBox(-4.5F, -4.0F, -4.0F, 9, 5, 8), PartPose.offset(-0.5F, 0.0F, 0.0F));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(102, 0).addBox(-3.0F, -2.0F, -3.0F, 6, 16, 6), PartPose.offset(11.0F, -13.0F, 0.0F));
        leftArm.addOrReplaceChild("left_pauldron", CubeListBuilder.create().texOffs(78, 35).addBox(-4.5F, -4.0F, -4.0F, 9, 5, 8), PartPose.offset(0.5F, 0.0F, 0.0F));
        // Chain mace: handle in the right fist, a chain hanging from it and a spiked iron head.
        PartDefinition handle = rightArm.addOrReplaceChild("handle", CubeListBuilder.create().texOffs(110, 22).addBox(-1.0F, 0.0F, -1.0F, 2, 6, 2), PartPose.offset(0.0F, 13.0F, -1.0F));
        PartDefinition chain = handle.addOrReplaceChild("chain", CubeListBuilder.create().texOffs(118, 22).addBox(-0.5F, 0.0F, -0.5F, 1, 6, 1), PartPose.offset(0.0F, 6.0F, 0.0F));
        PartDefinition mace = chain.addOrReplaceChild("mace_head", CubeListBuilder.create().texOffs(40, 54).addBox(-3.0F, 0.0F, -3.0F, 6, 6, 6), PartPose.offset(0.0F, 6.0F, 0.0F));
        CubeListBuilder spike = CubeListBuilder.create().texOffs(64, 54).addBox(-1.0F, -1.0F, -1.0F, 2, 2, 2);
        mace.addOrReplaceChild("spike_n", spike, PartPose.offset(0.0F, 3.0F, -3.5F));
        mace.addOrReplaceChild("spike_s", spike, PartPose.offset(0.0F, 3.0F, 3.5F));
        mace.addOrReplaceChild("spike_w", spike, PartPose.offset(-3.5F, 3.0F, 0.0F));
        mace.addOrReplaceChild("spike_e", spike, PartPose.offset(3.5F, 3.0F, 0.0F));
        mace.addOrReplaceChild("spike_d", spike, PartPose.offset(0.0F, 6.5F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(DreadRenderState state) {
        super.setupAnim(state);
        float pos = state.walkAnimationPos, speed = Math.min(1.0F, state.walkAnimationSpeed), age = state.ageInTicks;
        this.head.yRot = Mth.clamp(state.yRot, -40.0F, 40.0F) * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.5F;
        // heavy, slow stride
        this.rightLeg.xRot = Mth.cos(pos * 0.45F) * 0.7F * speed;
        this.leftLeg.xRot = Mth.cos(pos * 0.45F + Mth.PI) * 0.7F * speed;
        float breathe = Mth.sin(age * 0.05F) * 0.03F;
        this.body.xRot = 0.06F + breathe;
        this.rightArm.xRot = Mth.cos(pos * 0.45F + Mth.PI) * 0.35F * speed;
        this.leftArm.xRot = Mth.cos(pos * 0.45F) * 0.5F * speed - breathe;
        this.rightArm.zRot = 0.12F;
        this.leftArm.zRot = -0.08F;
        // the chain trails behind the fist: keep the mace head on the ground
        this.chain.xRot = -CHAIN_REST - this.rightArm.xRot;
        this.maceHead.xRot = CHAIN_REST;
        if (state.swing > 0.0F) {
            // raise the mace high over the right shoulder, the chain swings back with it
            float s = state.swing;
            this.rightArm.xRot = -2.9F * s;
            this.rightArm.zRot = 0.12F + 0.25F * s;
            this.chain.xRot = Mth.lerp(s, this.chain.xRot, 0.6F);
            this.maceHead.xRot = Mth.lerp(s, CHAIN_REST, 0.0F);
            this.body.xRot -= 0.15F * s;
        }
        if (state.stomp > 0.0F) {
            // both arms up, leaning back: the whole weight comes down
            float s = state.stomp;
            this.rightArm.xRot = -2.6F * s;
            this.leftArm.xRot = -2.6F * s;
            this.rightArm.zRot = 0.12F - 0.3F * s;
            this.leftArm.zRot = -0.08F + 0.3F * s;
            this.chain.xRot = Mth.lerp(s, this.chain.xRot, 0.3F);
            this.body.xRot -= 0.25F * s;
            this.rightLeg.xRot = -0.5F * s;
        }
        if (state.rise < 1.0F) {
            float under = 1.0F - state.rise;
            this.root().y += under * 42.0F;
            this.body.xRot += under * 0.6F;
            this.rightArm.xRot -= under * 1.4F;
            this.leftArm.xRot -= under * 1.4F;
        }
    }
}
