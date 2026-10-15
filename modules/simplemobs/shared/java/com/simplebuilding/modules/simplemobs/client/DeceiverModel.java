package com.simplebuilding.modules.simplemobs.client;

import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Small hooded figure (about 0.94 blocks): robe, hood head with a shadowed face, wide sleeves, a full cloak down the back like the player's Cloak of Deception (N48), optional chest plate. */
public class DeceiverModel extends EntityModel<DeceiverRenderState> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("simplemobs", "deceiver"), "main");

    private final ModelPart head;
    private final ModelPart plate;
    /** Parts the idle loop touches (rotation only), and their rotations before it ran, for fading it out while walking. */
    private final ModelPart[] idleParts;
    private final float[] beforeIdle = new float[15];
    private final KeyframeAnimation walk, idle, summon, teleport, drink, unmask;

    public DeceiverModel(ModelPart root) {
        super(root);
        ModelPart body = root.getChild("body");
        this.head = body.getChild("head");
        this.plate = body.getChild("plate");
        this.idleParts = new ModelPart[] {body, head, body.getChild("cloak"), body.getChild("right_arm"), body.getChild("left_arm")};
        this.walk = DeceiverAnimation.WALK.bake(root);
        this.idle = DeceiverAnimation.IDLE.bake(root);
        this.summon = DeceiverAnimation.SUMMON.bake(root);
        this.teleport = DeceiverAnimation.TELEPORT.bake(root);
        this.drink = DeceiverAnimation.DRINK.bake(root);
        this.unmask = DeceiverAnimation.UNMASK.bake(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition().transformed(p -> p.translated(0.0F, 24.0F, 0.0F));
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, 0.0F, -2.5F, 7.0F, 7.0F, 5.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, -9.0F, 0.0F));
        body.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 12).addBox(-3.0F, -6.0F, -3.0F, 6.0F, 6.0F, 6.0F, CubeDeformation.NONE)
                        .texOffs(24, 12).addBox(-1.0F, -7.0F, 1.0F, 2.0F, 3.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("cloak",
                CubeListBuilder.create().texOffs(24, 0).addBox(-3.5F, 0.0F, 0.0F, 7.0F, 10.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 0.0F, 2.5F));
        body.addOrReplaceChild("plate",
                CubeListBuilder.create().texOffs(24, 18).addBox(-3.0F, 0.5F, -3.0F, 6.0F, 4.0F, 1.0F, CubeDeformation.NONE),
                PartPose.ZERO);
        body.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(48, 0).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 7.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offset(-5.0F, 1.0F, 0.0F));
        body.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(48, 12).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 7.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offset(5.0F, 1.0F, 0.0F));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 24).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offset(-1.8F, -3.0F, 0.0F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(12, 24).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offset(1.8F, -3.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(DeceiverRenderState state) {
        super.setupAnim(state);
        this.plate.visible = state.armored;
        this.head.xRot = state.xRot * (float) (Math.PI / 180.0);
        this.head.yRot = state.yRot * (float) (Math.PI / 180.0);
        this.walk.applyWalk(state.walkAnimationPos, state.walkAnimationSpeed, 2.0F, 2.5F);
        applyIdle(state);
        this.summon.apply(state.summon, state.ageInTicks);
        this.teleport.apply(state.teleport, state.ageInTicks);
        this.drink.apply(state.drink, state.ageInTicks);
        this.unmask.apply(state.unmask, state.ageInTicks);
        applyRun(state);
    }

    /**
     * Own run pose for fleeing (walk speed above ~0.5): forward lean, arms swept back, the cloak streams out behind and
     * swings against turns; plus a limp crumple on death. Added on top of the keyframes.
     */
    private void applyRun(DeceiverRenderState state) {
        ModelPart body = root().getChild("body");
        ModelPart cloak = body.getChild("cloak");
        float run = Mth.smoothstep(Mth.clamp((state.walkAnimationSpeed - 0.5F) / 0.4F, 0.0F, 1.0F));
        body.xRot += 0.3F * run;
        cloak.xRot += 0.6F * run + 0.1F * run * Mth.sin(state.walkAnimationPos * 1.4F);
        body.getChild("right_arm").xRot += 0.9F * run;
        body.getChild("left_arm").xRot += 0.9F * run;
        this.head.xRot -= 0.3F * run;
        cloak.zRot += Mth.clamp(state.turn * 0.04F, -0.5F, 0.5F);
        if (state.deathTime > 0.0F) {
            float d = Mth.smoothstep(Mth.clamp(state.deathTime / 15.0F, 0.0F, 1.0F));
            body.xRot += 0.4F * d;
            this.head.xRot += 0.5F * d;
            body.getChild("right_arm").xRot += 0.5F * d;
            body.getChild("left_arm").xRot += 0.5F * d;
            cloak.xRot += 0.5F * d;
        }
    }

    /**
     * The idle loop adds on top of the walk, so walking would wobble twice. It fades out as soon as the Deceiver moves
     * (full below 0.02 walk speed, gone at 0.15) by blending the rotations back, so nothing snaps.
     */
    private void applyIdle(DeceiverRenderState state) {
        float w = 1.0F - Mth.smoothstep(Mth.clamp((state.walkAnimationSpeed - 0.02F) / 0.13F, 0.0F, 1.0F));
        if (w <= 0.0F) return;
        for (int i = 0; i < idleParts.length; i++) {
            beforeIdle[i * 3] = idleParts[i].xRot;
            beforeIdle[i * 3 + 1] = idleParts[i].yRot;
            beforeIdle[i * 3 + 2] = idleParts[i].zRot;
        }
        this.idle.apply(state.idle, state.ageInTicks);
        for (int i = 0; i < idleParts.length; i++) {
            idleParts[i].xRot = Mth.lerp(w, beforeIdle[i * 3], idleParts[i].xRot);
            idleParts[i].yRot = Mth.lerp(w, beforeIdle[i * 3 + 1], idleParts[i].yRot);
            idleParts[i].zRot = Mth.lerp(w, beforeIdle[i * 3 + 2], idleParts[i].zRot);
        }
    }
}
