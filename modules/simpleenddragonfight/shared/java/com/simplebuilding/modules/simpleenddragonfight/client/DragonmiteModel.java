package com.simplebuilding.modules.simpleenddragonfight.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Dragonmite: the four Endermite-like segments, 1.4 times as large, plus its own shapes - a row of three pale dragon
 * spines along the back and two stubby wings on the broad segment that buzz with a jerk. Texture 64 x 32 (layout in
 * modules/simpleenddragonfight/tools/gen_textures.py).
 */
public class DragonmiteModel extends EntityModel<DragonmiteRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("simpleenddragonfight", "dragonmite"), "main");
    static final int[][] SIZES = {{4, 3, 2}, {6, 4, 5}, {3, 3, 1}, {1, 2, 1}};
    static final int[][] TEXS = {{0, 0}, {0, 5}, {0, 14}, {0, 18}};
    private final ModelPart[] segments = new ModelPart[4];
    private final ModelPart rightWing, leftWing;

    public DragonmiteModel(ModelPart root) {
        super(root);
        for (int i = 0; i < 4; i++) segments[i] = root.getChild("segment" + i);
        this.rightWing = segments[1].getChild("right_wing");
        this.leftWing = segments[1].getChild("left_wing");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        float placement = -3.5F;
        for (int i = 0; i < 4; i++) {
            PartDefinition seg = root.addOrReplaceChild("segment" + i, CubeListBuilder.create().texOffs(TEXS[i][0], TEXS[i][1])
                            .addBox(SIZES[i][0] * -0.5F, 0.0F, SIZES[i][2] * -0.5F, SIZES[i][0], SIZES[i][1], SIZES[i][2]),
                    PartPose.offset(0.0F, 24 - SIZES[i][1], placement));
            if (i == 0) seg.addOrReplaceChild("spine0", CubeListBuilder.create().texOffs(24, 0).addBox(-0.5F, -2.0F, -0.5F, 1, 2, 1), PartPose.offset(0.0F, 0.0F, 0.0F));
            if (i == 1) {
                seg.addOrReplaceChild("spine1", CubeListBuilder.create().texOffs(28, 0).addBox(-0.5F, -3.0F, -0.5F, 1, 3, 1), PartPose.offset(0.0F, 0.0F, -0.5F));
                seg.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(24, 5).addBox(-4.0F, 0.0F, -1.5F, 4, 1, 3), PartPose.offset(-2.5F, 0.0F, 0.5F));
                seg.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(24, 5).mirror().addBox(0.0F, 0.0F, -1.5F, 4, 1, 3), PartPose.offset(2.5F, 0.0F, 0.5F));
            }
            if (i == 2) seg.addOrReplaceChild("spine2", CubeListBuilder.create().texOffs(32, 0).addBox(-0.5F, -2.0F, -0.5F, 1, 2, 1), PartPose.offset(0.0F, 0.0F, 0.0F));
            if (i < 3) placement += (SIZES[i][2] + SIZES[i + 1][2]) * 0.5F;
        }
        return LayerDefinition.create(mesh, 64, 32).apply(MeshTransformer.scaling(1.4F));
    }

    @Override
    public void setupAnim(DragonmiteRenderState state) {
        super.setupAnim(state);
        // Resting: slow, small sway and folded wings. Moving: the quick wiggle in step with the ground covered and the
        // full buzz. Blended by speed; every Dragonmite has its own phase (entity id), so a swarm never buzzes in unison.
        float m = Mth.smoothstep(Mth.clamp(state.walkAnimationSpeed * 2.0F, 0.0F, 1.0F));
        float idleT = state.ageInTicks * 0.1F + state.phase;
        float walkT = state.walkAnimationPos * 0.9F + state.phase;
        for (int i = 0; i < segments.length; i++) {
            float off = i * 0.15F * (float) Math.PI;
            float yRot = Mth.lerp(m, Mth.cos(idleT + off) * 0.25F, Mth.cos(walkT + off));
            float x = Mth.lerp(m, Mth.sin(idleT + off) * 0.25F, Mth.sin(walkT + off));
            segments[i].yRot = yRot * (float) Math.PI * 0.01F * (1 + Math.abs(i - 2));
            segments[i].x = x * (float) Math.PI * 0.1F * Math.abs(i - 2);
        }
        // Bite: the head and broad segment lunge forward (offsets only, no cube changes).
        segments[0].z -= state.lunge * 1.5F;
        segments[1].z -= state.lunge * 0.6F;
        // Buzz: a fast flutter that jerks every seven ticks, each mite on its own beat.
        float jerk = ((int) (state.ageInTicks + state.phase * 7.0F) % 7 == 0) ? 0.3F : 0.0F;
        float buzz = Mth.lerp(m, 0.15F + 0.05F * Mth.sin(idleT * 4.0F), 0.35F + 0.25F * Mth.sin(state.ageInTicks * 2.6F + state.phase) + jerk);
        buzz = Mth.lerp(state.lunge, buzz, 0.0F);
        rightWing.zRot = buzz;
        leftWing.zRot = -buzz;
    }
}
