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

/** Small hooded figure (about 0.94 blocks): robe, hood head with a shadowed face, wide sleeves, cloak flap, optional chest plate. */
public class DeceiverModel extends EntityModel<DeceiverRenderState> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("simplemobs", "deceiver"), "main");

    private final ModelPart head;
    private final ModelPart plate;
    private final KeyframeAnimation walk, idle, summon, teleport, drink, unmask;

    public DeceiverModel(ModelPart root) {
        super(root);
        ModelPart body = root.getChild("body");
        this.head = body.getChild("head");
        this.plate = body.getChild("plate");
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
                CubeListBuilder.create().texOffs(24, 0).addBox(-3.0F, 0.0F, 0.0F, 6.0F, 8.0F, 1.0F, CubeDeformation.NONE),
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
        this.idle.apply(state.idle, state.ageInTicks);
        this.summon.apply(state.summon, state.ageInTicks);
        this.teleport.apply(state.teleport, state.ageInTicks);
        this.drink.apply(state.drink, state.ageInTicks);
        this.unmask.apply(state.unmask, state.ageInTicks);
    }
}
